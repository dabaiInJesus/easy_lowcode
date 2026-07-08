package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.auth.service.AuthorizationService;
import com.dabai.easy_lowcode.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 授权管理Controller
 */
@Tag(name = "授权管理", description = "用户角色分配、角色菜单分配、权限管理")
@RestController
@RequestMapping("/api/auth/authorization")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AuthorizationController {
    
    private final AuthorizationService authorizationService;
    
    @Operation(summary = "获取用户列表（包含角色）", description = "获取所有用户及其关联的角色信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsersWithRoles() {
        List<Map<String, Object>> users = authorizationService.getUsersWithRoles();
        return Result.success(users);
    }
    
    @Operation(summary = "获取角色列表（包含菜单数量）", description = "获取所有角色及其关联的菜单数量")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> getRolesWithMenuCount() {
        List<Map<String, Object>> roles = authorizationService.getRolesWithMenuCount();
        return Result.success(roles);
    }
    
    @Operation(summary = "获取所有角色", description = "获取系统中所有角色的基本信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/all-roles")
    public Result<List<Map<String, Object>>> getAllRoles() {
        List<Map<String, Object>> roles = authorizationService.getAllRoles().stream()
                .map(role -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", role.getId());
                    map.put("roleName", role.getRoleName());
                    map.put("roleCode", role.getRoleCode());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return Result.success(roles);
    }
    
    @Operation(summary = "获取用户的角色", description = "获取指定用户已分配的角色列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/user/{userId}/roles")
    public Result<List<Map<String, Object>>> getUserRoles(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<Map<String, Object>> roles = authorizationService.getUserRoles(userId).stream()
                .map(role -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", role.getId());
                    map.put("roleName", role.getRoleName());
                    map.put("roleCode", role.getRoleCode());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
        return Result.success(roles);
    }
    
    @Operation(summary = "为用户分配角色", description = "为指定用户分配一个或多个角色")
    @ApiResponse(responseCode = "200", description = "分配成功")
    @PostMapping("/user/{userId}/roles")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> assignRolesToUser(
            @Parameter(description = "用户ID") @PathVariable Long userId,
            @RequestBody AssignRolesRequest request) {
        authorizationService.assignRolesToUser(userId, request.getRoleIds());
        return Result.success();
    }
    
    @Operation(summary = "获取角色的菜单", description = "获取指定角色已分配的菜单ID列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/role/{roleId}/menus")
    public Result<List<Long>> getRoleMenus(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<Long> menuIds = authorizationService.getRoleMenus(roleId).stream()
                .map(menu -> menu.getId())
                .collect(java.util.stream.Collectors.toList());
        return Result.success(menuIds);
    }
    
    @Operation(summary = "获取菜单树", description = "获取完整的菜单树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/menus/tree")
    public Result<List<Map<String, Object>>> getMenuTree() {
        List<Map<String, Object>> menuTree = authorizationService.getMenuTree();
        return Result.success(menuTree);
    }
    
    @Operation(summary = "为角色分配菜单", description = "为指定角色分配菜单权限")
    @ApiResponse(responseCode = "200", description = "分配成功")
    @PostMapping("/role/{roleId}/menus")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> assignMenusToRole(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody AssignMenusRequest request) {
        authorizationService.assignMenusToRole(roleId, request.getMenuIds());
        return Result.success();
    }
    
    @Data
    static class AssignRolesRequest {
        private List<Long> roleIds;
    }
    
    @Data
    static class AssignMenusRequest {
        private List<String> menuIds;
    }

    @Operation(summary = "清理无效的菜单", description = "删除大屏设计等无效菜单项（路径为/dashboard/design但没有对应路由）")
    @DeleteMapping("/menus/cleanup")
    @PreAuthorize("hasRole('admin')")
    public Result<Integer> cleanupInvalidMenus() {
        int count = authorizationService.cleanupInvalidMenus();
        return Result.success(count);
    }
}
