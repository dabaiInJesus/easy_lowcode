package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.auth.service.AuthorizationService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 授权管理Controller
 */
@RestController
@RequestMapping("/api/auth/authorization")
@RequiredArgsConstructor
public class AuthorizationController {
    
    private final AuthorizationService authorizationService;
    
    /**
     * 获取用户列表（包含角色信息）
     */
    @GetMapping("/users")
    public Result<List<Map<String, Object>>> getUsersWithRoles() {
        List<Map<String, Object>> users = authorizationService.getUsersWithRoles();
        return Result.success(users);
    }
    
    /**
     * 获取角色列表（包含菜单数量）
     */
    @GetMapping("/roles")
    public Result<List<Map<String, Object>>> getRolesWithMenuCount() {
        List<Map<String, Object>> roles = authorizationService.getRolesWithMenuCount();
        return Result.success(roles);
    }
    
    /**
     * 获取所有角色
     */
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
    
    /**
     * 获取用户的角色
     */
    @GetMapping("/user/{userId}/roles")
    public Result<List<Map<String, Object>>> getUserRoles(@PathVariable Long userId) {
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
    
    /**
     * 为用户分配角色
     */
    @PostMapping("/user/{userId}/roles")
    public Result<Void> assignRolesToUser(
            @PathVariable Long userId,
            @RequestBody AssignRolesRequest request) {
        authorizationService.assignRolesToUser(userId, request.getRoleIds());
        return Result.success();
    }
    
    /**
     * 获取角色的菜单
     */
    @GetMapping("/role/{roleId}/menus")
    public Result<List<Long>> getRoleMenus(@PathVariable Long roleId) {
        List<Long> menuIds = authorizationService.getRoleMenus(roleId).stream()
                .map(menu -> menu.getId())
                .collect(java.util.stream.Collectors.toList());
        return Result.success(menuIds);
    }
    
    /**
     * 获取菜单树
     */
    @GetMapping("/menus/tree")
    public Result<List<Map<String, Object>>> getMenuTree() {
        List<Map<String, Object>> menuTree = authorizationService.getMenuTree();
        return Result.success(menuTree);
    }
    
    /**
     * 为角色分配菜单
     */
    @PostMapping("/role/{roleId}/menus")
    public Result<Void> assignMenusToRole(
            @PathVariable Long roleId,
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
}
