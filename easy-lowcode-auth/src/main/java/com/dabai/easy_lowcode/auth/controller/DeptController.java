package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysDept;
import com.dabai.easy_lowcode.auth.service.SysDeptService;
import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门管理控制器
 */
@Tag(name = "部门管理", description = "部门CRUD及树形结构管理")
@Slf4j
@RestController
@RequestMapping("/api/auth/dept")
@RequiredArgsConstructor
public class DeptController {
    
    private final SysDeptService deptService;
    private final JdbcTemplate jdbcTemplate;
    
    @Operation(summary = "获取部门树形列表", description = "获取树形结构的部门列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<SysDept>> getDeptList() {
        List<SysDept> deptTree = deptService.getDeptTree();
        return Result.success(deptTree);
    }
    
    @Operation(summary = "创建部门", description = "创建新部门，自动生成部门编码")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> createDept(@RequestBody SysDept dept) {
        if (dept.getDeptCode() == null || dept.getDeptCode().trim().isEmpty()) {
            dept.setDeptCode(generateDeptCode(dept.getDeptName()));
        }
        
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptCode, dept.getDeptCode());
        if (deptService.count(wrapper) > 0) {
            return Result.error("部门编码已存在");
        }
        
        if (dept.getSort() == null) {
            dept.setSort(1);
        }
        
        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        
        deptService.save(dept);
        return Result.success();
    }
    
    @Operation(summary = "更新部门", description = "更新部门信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> updateDept(@RequestBody SysDept dept) {
        LambdaQueryWrapper<SysDept> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysDept::getDeptCode, dept.getDeptCode())
               .ne(SysDept::getId, dept.getId());
        if (deptService.count(wrapper) > 0) {
            return Result.error("部门编码已存在");
        }
        
        deptService.updateById(dept);
        return Result.success();
    }
    
    @Operation(summary = "删除部门", description = "删除部门（需确保无子部门且无关联用户）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> deleteDept(@Parameter(description = "部门ID") @PathVariable Long id) {
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
    
    private String generateDeptCode(String deptName) {
        if (deptName == null || deptName.trim().isEmpty()) {
            return "dept_" + System.currentTimeMillis();
        }
        
        String code = deptName.replaceAll("[\\s\\u3000]+", "_")
                              .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "")
                              .toLowerCase();
        
        if (code.matches(".*[\\u4e00-\\u9fa5].*")) {
            return code;
        }
        
        if (code.matches("^\\d.*")) {
            code = "dept_" + code;
        }
        
        return code;
    }
}
