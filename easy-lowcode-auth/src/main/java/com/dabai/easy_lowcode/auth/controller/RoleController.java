package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysRole;
import com.dabai.easy_lowcode.auth.service.SysRoleService;
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
 * 角色管理控制器
 */
@Tag(name = "角色管理", description = "角色CRUD管理")
@Slf4j
@RestController
@RequestMapping("/api/auth/role")
@RequiredArgsConstructor
public class RoleController {
    
    private final SysRoleService roleService;
    private final JdbcTemplate jdbcTemplate;
    
    @Operation(summary = "获取角色列表", description = "获取所有角色列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<SysRole>> getRoleList() {
        List<SysRole> roleList = roleService.getRoleList();
        return Result.success(roleList);
    }
    
    @Operation(summary = "创建角色", description = "创建新角色，自动生成角色编码")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> createRole(@RequestBody SysRole role) {
        if (role.getRoleCode() == null || role.getRoleCode().trim().isEmpty()) {
            role.setRoleCode(generateRoleCode(role.getRoleName()));
        }
        
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
        if (roleService.count(wrapper) > 0) {
            return Result.error("角色编码已存在");
        }
        
        if (role.getSort() == null) {
            role.setSort(1);
        }
        
        if (role.getStatus() == null) {
            role.setStatus(1);
        }
        
        roleService.save(role);
        return Result.success();
    }
    
    @Operation(summary = "更新角色", description = "更新角色信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> updateRole(@RequestBody SysRole role) {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getRoleCode, role.getRoleCode())
               .ne(SysRole::getId, role.getId());
        if (roleService.count(wrapper) > 0) {
            return Result.error("角色编码已存在");
        }
        
        roleService.updateById(role);
        return Result.success();
    }
    
    @Operation(summary = "删除角色", description = "删除角色（需确保无关联用户）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> deleteRole(@Parameter(description = "角色ID") @PathVariable Long id) {
        Integer userCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user_role WHERE role_id = ?", Integer.class, id);
        if (userCount != null && userCount > 0) {
            return Result.error("该角色下有 " + userCount + " 个用户关联，请先移除用户关联后再删除");
        }

        roleService.removeById(id);
        return Result.success();
    }
    
    private String generateRoleCode(String roleName) {
        if (roleName == null || roleName.trim().isEmpty()) {
            return "role_" + System.currentTimeMillis();
        }
        
        String code = roleName.replaceAll("[\\s\\u3000]+", "_")
                              .replaceAll("[^a-zA-Z0-9_\\u4e00-\\u9fa5]", "")
                              .toLowerCase();
        
        if (code.matches(".*[\\u4e00-\\u9fa5].*")) {
            return code;
        }
        
        if (code.matches("^\\d.*")) {
            code = "role_" + code;
        }
        
        return code;
    }
}
