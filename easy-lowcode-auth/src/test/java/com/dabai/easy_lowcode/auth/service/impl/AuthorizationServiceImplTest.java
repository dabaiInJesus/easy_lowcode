package com.dabai.easy_lowcode.auth.service.impl;

import com.dabai.easy_lowcode.auth.entity.SysMenu;
import com.dabai.easy_lowcode.auth.entity.SysRole;
import com.dabai.easy_lowcode.auth.mapper.SysMenuMapper;
import com.dabai.easy_lowcode.auth.mapper.SysRoleMapper;
import com.dabai.easy_lowcode.auth.mapper.SysUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthorizationServiceImpl 单元测试")
class AuthorizationServiceImplTest {

    @Mock
    private SysUserMapper userMapper;

    @Mock
    private SysRoleMapper roleMapper;

    @Mock
    private SysMenuMapper menuMapper;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private AuthorizationServiceImpl authorizationService;

    private SysRole adminRole;
    private SysRole userRole;
    private SysMenu parentMenu;
    private SysMenu childMenu;

    @BeforeEach
    void setUp() {
        adminRole = new SysRole();
        adminRole.setId(1L);
        adminRole.setRoleName("管理员");
        adminRole.setRoleCode("admin");
        adminRole.setDescription("系统管理员");
        adminRole.setStatus(1);
        adminRole.setSort(1);

        userRole = new SysRole();
        userRole.setId(2L);
        userRole.setRoleName("普通用户");
        userRole.setRoleCode("user");
        userRole.setDescription("普通用户");
        userRole.setStatus(1);
        userRole.setSort(2);

        parentMenu = new SysMenu();
        parentMenu.setId(10L);
        parentMenu.setParentId(0L);
        parentMenu.setMenuName("系统管理");
        parentMenu.setMenuType(1);
        parentMenu.setPath("/system");
        parentMenu.setSort(1);
        parentMenu.setVisible(1);

        childMenu = new SysMenu();
        childMenu.setId(20L);
        childMenu.setParentId(10L);
        childMenu.setMenuName("用户管理");
        childMenu.setMenuType(2);
        childMenu.setPath("/system/user");
        childMenu.setComponent("system/user/index");
        childMenu.setPerms("system:user:list");
        childMenu.setSort(1);
        childMenu.setVisible(1);
    }

    // ==================== assignRolesToUser() ====================

    @Test
    @DisplayName("为用户分配角色 - 成功分配多个角色")
    void testAssignRolesToUser_withRoles_success() {
        List<Long> roleIds = List.of(1L, 2L);
        when(jdbcTemplate.update(eq("DELETE FROM sys_user_role WHERE user_id = ?"), eq(1L))).thenReturn(2);
        when(jdbcTemplate.update(eq("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)"), eq(1L), anyLong()))
                .thenReturn(1);

        authorizationService.assignRolesToUser(1L, roleIds);

        verify(jdbcTemplate).update("DELETE FROM sys_user_role WHERE user_id = ?", 1L);
        verify(jdbcTemplate).update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", 1L, 1L);
        verify(jdbcTemplate).update("INSERT INTO sys_user_role (user_id, role_id) VALUES (?, ?)", 1L, 2L);
    }

    @Test
    @DisplayName("为用户分配角色 - 空角色列表时只删除原有角色")
    void testAssignRolesToUser_emptyRoles_onlyDeletes() {
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(0);

        authorizationService.assignRolesToUser(1L, Collections.emptyList());

        verify(jdbcTemplate, times(1)).update(anyString(), eq(1L));
        verify(jdbcTemplate, never()).update(anyString(), anyLong(), anyLong());
    }

    @Test
    @DisplayName("为用户分配角色 - null角色列表时只删除原有角色")
    void testAssignRolesToUser_nullRoles_onlyDeletes() {
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(0);

        authorizationService.assignRolesToUser(1L, null);

        verify(jdbcTemplate, times(1)).update(anyString(), eq(1L));
        verify(jdbcTemplate, never()).update(anyString(), anyLong(), anyLong());
    }

    // ==================== getUserRoles() ====================

    @Test
    @DisplayName("获取用户角色 - 用户有角色时返回角色列表")
    void testGetUserRoles_found_returnsRoles() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(adminRole, userRole));

        List<SysRole> roles = authorizationService.getUserRoles(1L);

        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).getRoleName()).isEqualTo("管理员");
        assertThat(roles.get(1).getRoleName()).isEqualTo("普通用户");
        verify(jdbcTemplate).query(anyString(), any(RowMapper.class), eq(1L));
    }

    @Test
    @DisplayName("获取用户角色 - 用户无角色时返回空列表")
    void testGetUserRoles_notFound_returnsEmptyList() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99L)))
                .thenReturn(Collections.emptyList());

        List<SysRole> roles = authorizationService.getUserRoles(99L);

        assertThat(roles).isEmpty();
    }

    // ==================== getRoleMenus() ====================

    @Test
    @DisplayName("获取角色菜单 - 有菜单时返回列表")
    void testGetRoleMenus_found_returnsMenus() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(1L)))
                .thenReturn(List.of(parentMenu, childMenu));

        List<SysMenu> menus = authorizationService.getRoleMenus(1L);

        assertThat(menus).hasSize(2);
        assertThat(menus.get(0).getMenuName()).isEqualTo("系统管理");
        assertThat(menus.get(1).getMenuName()).isEqualTo("用户管理");
    }

    @Test
    @DisplayName("获取角色菜单 - 无菜单时返回空列表")
    void testGetRoleMenus_notFound_returnsEmptyList() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class), eq(99L)))
                .thenReturn(Collections.emptyList());

        List<SysMenu> menus = authorizationService.getRoleMenus(99L);

        assertThat(menus).isEmpty();
    }

    // ==================== assignMenusToRole() ====================

    @Test
    @DisplayName("为角色分配菜单 - 有效ID时成功分配")
    void testAssignMenusToRole_withValidIds_success() {
        List<String> menuIds = List.of("10", "20");
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(2);
        when(jdbcTemplate.update(anyString(), eq(1L), anyLong())).thenReturn(1);

        authorizationService.assignMenusToRole(1L, menuIds);

        verify(jdbcTemplate).update("DELETE FROM sys_role_menu WHERE role_id = ?", 1L);
        verify(jdbcTemplate).update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", 1L, 10L);
        verify(jdbcTemplate).update("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)", 1L, 20L);
    }

    @Test
    @DisplayName("为角色分配菜单 - 存在无效ID时跳过")
    void testAssignMenusToRole_withInvalidIds_skipsInvalid() {
        List<String> menuIds = List.of("10", "invalid", "20");
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(2);
        when(jdbcTemplate.update(anyString(), eq(1L), anyLong())).thenReturn(1);

        authorizationService.assignMenusToRole(1L, menuIds);

        verify(jdbcTemplate, times(2)).update(eq("INSERT INTO sys_role_menu (role_id, menu_id) VALUES (?, ?)"),
                eq(1L), anyLong());
    }

    @Test
    @DisplayName("为角色分配菜单 - 空列表时只删除原有菜单")
    void testAssignMenusToRole_emptyList_onlyDeletes() {
        when(jdbcTemplate.update(anyString(), eq(1L))).thenReturn(0);

        authorizationService.assignMenusToRole(1L, Collections.emptyList());

        verify(jdbcTemplate, times(1)).update(anyString(), eq(1L));
        verify(jdbcTemplate, never()).update(anyString(), eq(1L), anyLong());
    }

    // ==================== getAllRoles() ====================

    @Test
    @DisplayName("获取所有角色 - 返回启用并按sort排序的角色列表")
    void testGetAllRoles_returnsEnabledRolesSorted() {
        when(roleMapper.selectList(any())).thenReturn(List.of(adminRole, userRole));

        List<SysRole> roles = authorizationService.getAllRoles();

        assertThat(roles).hasSize(2);
        assertThat(roles.get(0).getRoleName()).isEqualTo("管理员");
        assertThat(roles.get(1).getRoleName()).isEqualTo("普通用户");
        verify(roleMapper).selectList(any());
    }

    @Test
    @DisplayName("获取所有角色 - 无角色时返回空列表")
    void testGetAllRoles_noRoles_returnsEmptyList() {
        when(roleMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<SysRole> roles = authorizationService.getAllRoles();

        assertThat(roles).isEmpty();
    }

    // ==================== getMenuTree() ====================

    @Test
    @DisplayName("获取菜单树 - 返回树形结构")
    void testGetMenuTree_returnsTreeStructure() {
        when(menuMapper.selectList(any())).thenReturn(List.of(parentMenu, childMenu));

        List<Map<String, Object>> tree = authorizationService.getMenuTree();

        assertThat(tree).hasSize(1);
        assertThat(tree.get(0)).containsEntry("menuName", "系统管理");
        assertThat(tree.get(0)).containsKey("children");
        List<Map<String, Object>> children = (List<Map<String, Object>>) tree.get(0).get("children");
        assertThat(children).hasSize(1);
        assertThat(children.get(0)).containsEntry("menuName", "用户管理");
    }

    @Test
    @DisplayName("获取菜单树 - 无菜单时返回空列表")
    void testGetMenuTree_noMenus_returnsEmptyList() {
        when(menuMapper.selectList(any())).thenReturn(Collections.emptyList());

        List<Map<String, Object>> tree = authorizationService.getMenuTree();

        assertThat(tree).isEmpty();
    }

    // ==================== getUsersWithRoles() ====================

    @Test
    @DisplayName("获取用户列表（含角色）- 成功返回")
    void testGetUsersWithRoles_returnsUsersWithRoles() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> rowMapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getString("username")).thenReturn("testuser");
                    when(rs.getString("nickname")).thenReturn("测试用户");
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getString("roles")).thenReturn("管理员,普通用户");
                    return List.of(rowMapper.mapRow(rs, 0));
                });

        List<Map<String, Object>> users = authorizationService.getUsersWithRoles();

        assertThat(users).hasSize(1);
        assertThat(users.get(0))
                .containsEntry("username", "testuser")
                .containsEntry("nickname", "测试用户")
                .containsEntry("status", 1);
        List<String> roles = (List<String>) users.get(0).get("roles");
        assertThat(roles).containsExactly("管理员", "普通用户");
    }

    @Test
    @DisplayName("获取用户列表（含角色）- 用户无角色时返回空角色列表")
    void testGetUsersWithRoles_noRoles_returnsEmptyRoles() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> rowMapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getString("username")).thenReturn("testuser");
                    when(rs.getString("nickname")).thenReturn("测试用户");
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getString("roles")).thenReturn(null);
                    return List.of(rowMapper.mapRow(rs, 0));
                });

        List<Map<String, Object>> users = authorizationService.getUsersWithRoles();

        assertThat(users).hasSize(1);
        List<String> roles = (List<String>) users.get(0).get("roles");
        assertThat(roles).isEmpty();
    }

    // ==================== getRolesWithMenuCount() ====================

    @Test
    @DisplayName("获取角色列表（含菜单数量）- 成功返回")
    void testGetRolesWithMenuCount_returnsRolesWithCount() {
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenAnswer(invocation -> {
                    RowMapper<Map<String, Object>> rowMapper = invocation.getArgument(1);
                    ResultSet rs = mock(ResultSet.class);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getString("role_name")).thenReturn("管理员");
                    when(rs.getString("role_code")).thenReturn("admin");
                    when(rs.getString("description")).thenReturn("系统管理员");
                    when(rs.getInt("status")).thenReturn(1);
                    when(rs.getInt("sort")).thenReturn(1);
                    when(rs.getInt("menu_count")).thenReturn(5);
                    return List.of(rowMapper.mapRow(rs, 0));
                });

        List<Map<String, Object>> roles = authorizationService.getRolesWithMenuCount();

        assertThat(roles).hasSize(1);
        assertThat(roles.get(0))
                .containsEntry("roleName", "管理员")
                .containsEntry("roleCode", "admin")
                .containsEntry("menuCount", 5);
    }

    // ==================== cleanupInvalidMenus() ====================

    @Test
    @DisplayName("清理无效菜单 - 成功删除并返回数量")
    void testCleanupInvalidMenus_returnsDeletedCount() {
        when(jdbcTemplate.update("DELETE FROM sys_menu WHERE path = '/dashboard/design'")).thenReturn(3);

        int deleted = authorizationService.cleanupInvalidMenus();

        assertThat(deleted).isEqualTo(3);
        verify(jdbcTemplate).update("DELETE FROM sys_menu WHERE path = '/dashboard/design'");
    }

    @Test
    @DisplayName("清理无效菜单 - 无匹配时返回0")
    void testCleanupInvalidMenus_noMatches_returnsZero() {
        when(jdbcTemplate.update(anyString())).thenReturn(0);

        int deleted = authorizationService.cleanupInvalidMenus();

        assertThat(deleted).isEqualTo(0);
    }
}
