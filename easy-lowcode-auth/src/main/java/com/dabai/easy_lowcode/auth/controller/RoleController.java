package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysRole;
import com.dabai.easy_lowcode.auth.service.SysRoleService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 角色管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/role")
@RequiredArgsConstructor
public class RoleController {
    
    private final SysRoleService roleService;
    
    /**
     * 获取角色列表
     */
    @GetMapping("/list")
    public Result<List<SysRole>> getRoleList() {
        List<SysRole> roleList = roleService.getRoleList();
        return Result.success(roleList);
    }
    
    /**
     * 创建角色
     */
    @PostMapping
    public Result<Void> createRole(@RequestBody SysRole role) {
        // 自动生成role_code（如果未提供）
        if (role.getRoleCode() == null || role.getRoleCode().trim().isEmpty()) {
            role.setRoleCode(generateRoleCode(role.getRoleName()));
        }
        
        // 验证role_code唯一性
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
        if (roleService.count(wrapper) > 0) {
            return Result.error("角色编码已存在");
        }
        
        // 设置默认排序
        if (role.getSort() == null) {
            role.setSort(1);
        }
        
        // 设置默认状态
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        
        roleService.save(role);
        return Result.success();
    }
    
    /**
     * 更新角色
     */
    @PutMapping
    public Result<Void> updateRole(@RequestBody SysRole role) {
        // 验证role_code唯一性（排除自身）
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode())
               .ne(SysRole::getId, role.getId());
        if (roleService.count(wrapper) > 0) {
            return Result.error("角色编码已存在");
        }
        
        roleService.updateById(role);
        return Result.success();
    }
    
    /**
     * 删除角色
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@PathVariable Long id) {
        // TODO: 检查是否有用户关联该角色
        
        roleService.removeById(id);
        return Result.success();
    }
    
    /**
     * 根据角色名称生成角色编码
     */
    private String generateRoleCode(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return "role_" + System.currentTimeMillis();
        }
        
        // 简单处理：移除空格和特殊字符，转为小写
        String code = roleName.replaceAll("[\\s\\u3000]+", "_")  // 替换空格
                             .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "")  // 移除特殊字符
                             .toLowerCase();
        
        // 如果是中文，保留原样让用户手动输入英文
        if (code.matches(".*[\\u4e00-\\u9fa5].*")) {
            return code;
        }
        
        // 确保不以数字开头
        if (code.matches("^\\d.*")) {
            code = "role_" + code;
        }
        
        return code;
    }
}
