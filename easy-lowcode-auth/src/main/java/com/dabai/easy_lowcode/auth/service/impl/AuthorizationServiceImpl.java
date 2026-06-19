package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.auth.entity.SysMenu;
import com.dabai.easy_lowcode.auth.entity.SysRole;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.mapper.SysMenuMapper;
import com.dabai.easy_lowcode.auth.mapper.SysRoleMapper;
import com.dabai.easy_lowcode.auth.mapper.SysUserMapper;
import com.dabai.easy_lowcode.auth.service.AuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 授权服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthorizationServiceImpl implements AuthorizationService {
    
    private final SysUserMapper userMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public List<SysRole> getUserRoles(Long userId) {
        String sql = "SELECT r.* FROM sys_role r " +
                    "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                    "WHERE ur.user_id = ? AND r.deleted = 0";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SysRole role = new SysRole();
            role.setId(rs.getLong("id"));
            role.setRoleName(rs.getString("role_name"));
            role.setRoleCode(rs.getString("role_code"));
            role.setDescription(rs.getString("description"));
            role.setStatus(rs.getInt("status"));
            role.setSort(rs.getInt("sort"));
            return role;
        }, userId);
    }
    
    @Override
    @Transactional
    public void assignRolesToUser(Long userId, List<Long> roleIds) {
        // 删除用户原有的角色
        String deleteSql = "DELETE FROM sys_user_role WHERE user_id = ?";
        jdbcTemplate.update(deleteSql, userId);
        
        // 添加新的角色关联
        if (roleIds != null && !roleIds.isEmpty()) {
            String insertSql = "INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)";
            for (Long roleId : roleIds) {
                jdbcTemplate.update(insertSql, userId, roleId);
            }
        }
        
        log.info("为用户 {} 分配角色: {}", userId, roleIds);
    }
    
    @Override
    public List<SysMenu> getRoleMenus(Long roleId) {
        String sql = "SELECT m.* FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "WHERE rm.role_id = ? AND m.deleted = 0 " +
                    "ORDER BY m.sort";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            SysMenu menu = new SysMenu();
            menu.setId(rs.getLong("id"));
            menu.setParentId(rs.getLong("parent_id"));
            menu.setMenuName(rs.getString("menu_name"));
            menu.setMenuType(rs.getInt("menu_type"));
            menu.setPath(rs.getString("path"));
            menu.setComponent(rs.getString("component"));
            menu.setPerms(rs.getString("perms"));
            menu.setIcon(rs.getString("icon"));
            menu.setSort(rs.getInt("sort"));
            menu.setVisible(rs.getInt("visible"));
            return menu;
        }, roleId);
    }
    
    @Override
    @Transactional
    public void assignMenusToRole(Long roleId, List<String> menuIds) {
        // 删除角色原有的菜单
        String deleteSql = "DELETE FROM sys_role_menu WHERE role_id = ?";
        jdbcTemplate.update(deleteSql, roleId);
        
        // 添加新的菜单关联
        if (menuIds != null && !menuIds.isEmpty()) {
            String insertSql = "INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)";
            for (String menuIdStr : menuIds) {
                try {
                    Long menuId = Long.parseLong(menuIdStr);
                    jdbcTemplate.update(insertSql, roleId, menuId);
                } catch (NumberFormatException e) {
                    log.warn("无效的菜单ID: {}", menuIdStr);
                }
            }
        }
        
        log.info("为角色 {} 分配菜单: {}", roleId, menuIds);
    }
    
    @Override
    public List<SysRole> getAllRoles() {
        LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysRole::getStatus, 1)
               .orderByAsc(SysRole::getSort);
        return roleMapper.selectList(wrapper);
    }
    
    @Override
    public List<Map<String, Object>> getMenuTree() {
        // 获取所有菜单（只查询目录和菜单类型，排除按钮）
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(SysMenu::getMenuType, 1, 2) // 1-目录 2-菜单，排除按钮
               .eq(SysMenu::getVisible, 1) // 只显示可见菜单
               .orderByAsc(SysMenu::getSort);
        List<SysMenu> allMenus = menuMapper.selectList(wrapper);

        // 数据修复：把顶级 API管理（menu_name='API管理'，parent_id=0）重新归属到 资源管理 父菜单下
        SysMenu resourceMenu = allMenus.stream()
                .filter(m -> "resource".equals(m.getMenuCode()))
                .findFirst().orElse(null);
        if (resourceMenu != null) {
            for (SysMenu m : allMenus) {
                if ("API管理".equals(m.getMenuName()) && (m.getParentId() == null || m.getParentId() == 0L)) {
                    m.setParentId(resourceMenu.getId());
                    m.setSort(3);
                }
            }
        }

        // 构建树形结构
        return buildMenuTree(allMenus, 0L);
    }
    
    private List<Map<String, Object>> buildMenuTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> {
                    // 处理parentId为null的情况
                    Long menuParentId = menu.getParentId();
                    if (menuParentId == null) {
                        return parentId == 0L; // null视为顶级菜单
                    }
                    return menuParentId.equals(parentId);
                })
                .map(menu -> {
                    Map<String, Object> node = new HashMap<>();
                    // 将Long类型ID转换为String，避免前端精度丢失
                    node.put("id", String.valueOf(menu.getId()));
                    node.put("parentId", menu.getParentId() != null ? String.valueOf(menu.getParentId()) : "0");
                    node.put("menuName", menu.getMenuName());
                    node.put("menuType", menu.getMenuType());
                    node.put("path", menu.getPath());
                    node.put("component", menu.getComponent());
                    node.put("perms", menu.getPerms());
                    node.put("icon", menu.getIcon());
                    node.put("sort", menu.getSort());
                    node.put("visible", menu.getVisible());
                    
                    // 递归获取子菜单
                    List<Map<String, Object>> children = buildMenuTree(menus, menu.getId());
                    if (!children.isEmpty()) {
                        node.put("children", children);
                    }
                    
                    return node;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<Map<String, Object>> getUsersWithRoles() {
        // PostgreSQL使用STRING_AGG替代MySQL的GROUP_CONCAT
        String sql = "SELECT u.id, u.username, u.nickname, u.status, " +
                    "STRING_AGG(r.role_name, ',') as roles " +
                    "FROM sys_user u " +
                    "LEFT JOIN sys_user_role ur ON u.id = ur.user_id " +
                    "LEFT JOIN sys_role r ON ur.role_id = r.id " +
                    "WHERE u.deleted = 0 " +
                    "GROUP BY u.id, u.username, u.nickname, u.status";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> user = new HashMap<>();
            user.put("id", rs.getLong("id"));
            user.put("username", rs.getString("username"));
            user.put("nickname", rs.getString("nickname"));
            user.put("status", rs.getInt("status"));
            
            String rolesStr = rs.getString("roles");
            List<String> roles = rolesStr != null ? 
                Arrays.asList(rolesStr.split(",")) : Collections.emptyList();
            user.put("roles", roles);
            
            return user;
        });
    }
    
    @Override
    public List<Map<String, Object>> getRolesWithMenuCount() {
        String sql = "SELECT r.id, r.role_name, r.role_code, r.description, r.status, r.sort, " +
                    "COUNT(rm.menu_id) as menu_count " +
                    "FROM sys_role r " +
                    "LEFT JOIN sys_role_menu rm ON r.id = rm.role_id " +
                    "WHERE r.deleted = 0 " +
                    "GROUP BY r.id, r.role_name, r.role_code, r.description, r.status, r.sort " +
                    "ORDER BY r.sort";
        
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> role = new HashMap<>();
            role.put("id", rs.getLong("id"));
            role.put("roleName", rs.getString("role_name"));
            role.put("roleCode", rs.getString("role_code"));
            role.put("description", rs.getString("description"));
            role.put("status", rs.getInt("status"));
            role.put("sort", rs.getInt("sort"));
            role.put("menuCount", rs.getInt("menu_count"));
            return role;
        });
    }

    @Override
    @Transactional
    public int cleanupInvalidMenus() {
        // 删除路径为 /dashboard/design 的菜单（设计器需要ID参数，不适合作为菜单）
        String deleteSql = "DELETE FROM sys_menu WHERE path = '/dashboard/design'";
        int deleted = jdbcTemplate.update(deleteSql);
        log.info("清理无效菜单，删除了 {} 条记录", deleted);
        return deleted;
    }
}
