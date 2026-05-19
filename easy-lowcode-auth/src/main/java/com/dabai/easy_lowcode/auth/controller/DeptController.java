package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysDept;
import com.dabai.easy_lowcode.auth.service.SysDeptService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/auth/dept")
@RequiredArgsConstructor
public class DeptController {
    
    private final SysDeptService deptService;
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 获取部门树形列表
     */
    @GetMapping("/list")
    public Result<List<SysDept>> getDeptList() {
        List<SysDept> deptTree = deptService.getDeptTree();
        return Result.success(deptTree);
    }
    
    /**
     * 创建部门
     */
    @PostMapping
    public Result<Void> createDept(@RequestBody SysDept dept) {
        // 自动生成dept_code（如果未提供）
        if (dept.getDeptCode() == null || dept.getDeptCode().trim().isEmpty()) {
            dept.setDeptCode(generateDeptCode(dept.getDeptName()));
        }
        
        // 验证dept_code唯一性
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptCode, dept.getDeptCode());
        if (deptService.count(wrapper) > 0) {
            return Result.error("部门编码已存在");
        }
        
        // 设置默认排序
        if (dept.getSort() == null) {
            dept.setSort(1);
        }
        
        // 设置默认状态
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        
        // 设置默认父部门ID
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        
        deptService.save(dept);
        return Result.success();
    }
    
    /**
     * 更新部门
     */
    @PutMapping
    public Result<Void> updateDept(@RequestBody SysDept dept) {
        // 验证dept_code唯一性（排除自身）
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptCode, dept.getDeptCode())
               .ne(SysDept::getId, dept.getId());
        if (deptService.count(wrapper) > 0) {
            return Result.error("部门编码已存在");
        }
        
        deptService.updateById(dept);
        return Result.success();
    }
    
    /**
     * 删除部门
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteDept(@PathVariable Long id) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getParentId, id);
        long childCount = deptService.count(wrapper);
        if (childCount > 0) {
            return Result.error("该部门下有子部门，无法删除");
        }

        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE dept_id = ?", Integer.class, id);
        if (userCount != null && userCount > 0) {
            return Result.error("该部门下有 " + userCount + " 个用户关联，请先移除用户关联后再删除");
        }

        deptService.removeById(id);
        return Result.success();
    }
    
    /**
     * 根据部门名称生成部门编码
     */
    private String generateDeptCode(String deptName) {
        if (deptName == null || deptName.trim().isEmpty()) {
            return "dept_" + System.currentTimeMillis();
        }
        
        // 简单处理：移除空格和特殊字符，转为小写
        String code = deptName.replaceAll("[\\s\\u3000]+", "_")  // 替换空格
                             .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "")  // 移除特殊字符
                             .toLowerCase();
        
        // 如果是中文，保留原样让用户手动输入英文
        if (code.matches(".*[\\u4e00-\\u9fa5].*")) {
            return code;
        }
        
        // 确保不以数字开头
        if (code.matches("^\\d.*")) {
            code = "dept_" + code;
        }
        
        return code;
    }
}
