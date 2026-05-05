package com.dabai.easy_lowcode.auth.service;

import com.dabai.easy_lowcode.auth.entity.SysMenu;
import com.dabai.easy_lowcode.auth.entity.SysRole;
import com.dabai.easy_lowcode.auth.entity.SysUser;

import java.util.List;
import java.util.Map;

/**
 * 授权服务接口
 */
public interface AuthorizationService {
    
    /**
     * 获取用户的角色列表
     */
    List<SysRole> getUserRoles(Long userId);
    
    /**
     * 为用户分配角色
     */
    void assignRolesToUser(Long userId, List<Long> roleIds);
    
    /**
     * 获取角色的菜单列表
     */
    List<SysMenu> getRoleMenus(Long roleId);
    
    /**
     * 为角色分配菜单
     */
    void assignMenusToRole(Long roleId, List<String> menuIds);
    
    /**
     * 获取所有角色
     */
    List<SysRole> getAllRoles();
    
    /**
     * 获取所有菜单（树形结构）
     */
    List<Map<String, Object>> getMenuTree();
    
    /**
     * 获取用户列表（包含角色信息）
     */
    List<Map<String, Object>> getUsersWithRoles();
    
    /**
     * 获取角色列表（包含菜单数量）
     */
    List<Map<String, Object>> getRolesWithMenuCount();
}
