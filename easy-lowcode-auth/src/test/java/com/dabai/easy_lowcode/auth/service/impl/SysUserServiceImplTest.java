package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.mapper.SysUserMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.security.LoginUser;
import com.dabai.easy_lowcode.common.security.TokenBlacklistService;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.common.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SysUserServiceImpl 单元测试")
class SysUserServiceImplTest {

    @Mock
    private SysUserMapper sysUserMapper;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private SysUserServiceImpl sysUserService;

    private SysUser testUser;
    private final String rawPassword = "password123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sysUserService, "baseMapper", sysUserMapper);

        testUser = new SysUser();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setPassword(EncryptUtil.bcrypt(rawPassword));
        testUser.setNickname("测试用户");
        testUser.setRealName("测试");
        testUser.setPhone("13800138000");
        testUser.setEmail("test@example.com");
        testUser.setStatus(1);
        testUser.setGender(1);
        testUser.setDeptId(1L);
        testUser.setCreateTime(LocalDateTime.now());
    }

    // ==================== login() ====================

    @Test
    @DisplayName("登录成功 - 返回有效的JWT token")
    void testLogin_successful_returnsToken() {
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);
        when(jwtUtil.generateToken(1L, "testuser")).thenReturn("jwt-token-123");

        String token = sysUserService.login("testuser", rawPassword);

        assertThat(token).isEqualTo("jwt-token-123");
        verify(jwtUtil).generateToken(1L, "testuser");
        verify(sysUserMapper).selectOne(any(), anyBoolean());
    }

    @Test
    @DisplayName("登录失败 - 用户名不存在")
    void testLogin_userNotFound_throwsException() {
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        assertThatThrownBy(() -> sysUserService.login("unknown", rawPassword))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("登录失败 - 密码错误")
    void testLogin_wrongPassword_throwsException() {
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        assertThatThrownBy(() -> sysUserService.login("testuser", "wrong-password"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户名或密码错误");
    }

    @Test
    @DisplayName("登录失败 - 用户已被禁用")
    void testLogin_userDisabled_throwsException() {
        testUser.setStatus(0);
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        assertThatThrownBy(() -> sysUserService.login("testuser", rawPassword))
                .isInstanceOf(BusinessException.class)
                .hasMessage("用户已被禁用");
    }

    // ==================== logout() ====================

    @Test
    @DisplayName("登出成功 - 有token时加入黑名单并清除上下文")
    void testLogout_withToken_blacklistsAndClearsContext() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        mockRequest.addHeader("Authorization", "Bearer test-jwt-token");
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class);
             MockedStatic<SecurityContextHolder> security = mockStatic(SecurityContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);
            security.when(SecurityContextHolder::clearContext).then(invocation -> null);

            sysUserService.logout();

            verify(tokenBlacklistService).blacklist("test-jwt-token");
            security.verify(SecurityContextHolder::clearContext);
        }
    }

    @Test
    @DisplayName("登出成功 - 无token时只清除上下文")
    void testLogout_withoutAuthHeader_clearsContextOnly() {
        MockHttpServletRequest mockRequest = new MockHttpServletRequest();
        ServletRequestAttributes attrs = new ServletRequestAttributes(mockRequest);

        try (MockedStatic<RequestContextHolder> holder = mockStatic(RequestContextHolder.class);
             MockedStatic<SecurityContextHolder> security = mockStatic(SecurityContextHolder.class)) {
            holder.when(RequestContextHolder::getRequestAttributes).thenReturn(attrs);
            security.when(SecurityContextHolder::clearContext).then(invocation -> null);

            sysUserService.logout();

            verify(tokenBlacklistService, never()).blacklist(anyString());
            security.verify(SecurityContextHolder::clearContext);
        }
    }

    // ==================== getCurrentUser() ====================

    @Test
    @DisplayName("获取当前用户 - 已认证时返回用户信息（密码置空）")
    void testGetCurrentUser_authenticated_returnsUserWithoutPassword() {
        LoginUser loginUser = new LoginUser(1L, "testuser", "pwd",
                true, true, true, true,
                List.of(() -> "ROLE_USER"));
        Authentication auth = mock(Authentication.class);

        try (MockedStatic<SecurityContextHolder> security = mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = mock(SecurityContext.class);
            security.when(SecurityContextHolder::getContext).thenReturn(context);
            when(context.getAuthentication()).thenReturn(auth);
            when(auth.getPrincipal()).thenReturn(loginUser);
            when(sysUserMapper.selectById(1L)).thenReturn(testUser);

            SysUser result = sysUserService.getCurrentUser();

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getNickname()).isEqualTo("测试用户");
            assertThat(result.getPassword()).isNull();
            verify(sysUserMapper).selectById(1L);
        }
    }

    @Test
    @DisplayName("获取当前用户 - 未认证时返回null")
    void testGetCurrentUser_notAuthenticated_returnsNull() {
        try (MockedStatic<SecurityContextHolder> security = mockStatic(SecurityContextHolder.class)) {
            SecurityContext context = mock(SecurityContext.class);
            security.when(SecurityContextHolder::getContext).thenReturn(context);
            when(context.getAuthentication()).thenReturn(null);

            SysUser result = sysUserService.getCurrentUser();

            assertThat(result).isNull();
        }
    }

    // ==================== loadUserById() ====================

    @Test
    @DisplayName("加载用户 - 用户存在时返回UserDetails")
    void testLoadUserById_userFound_returnsUserDetails() {
        when(sysUserMapper.selectById(1L)).thenReturn(testUser);

        UserDetails userDetails = sysUserService.loadUserById(1L);

        assertThat(userDetails).isNotNull();
        assertThat(userDetails.getUsername()).isEqualTo("testuser");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.getAuthorities()).isNotEmpty();
    }

    @Test
    @DisplayName("加载用户 - 用户不存在时返回null")
    void testLoadUserById_userNotFound_returnsNull() {
        when(sysUserMapper.selectById(99L)).thenReturn(null);

        UserDetails userDetails = sysUserService.loadUserById(99L);

        assertThat(userDetails).isNull();
    }

    // ==================== 继承的IService方法测试 ====================

    @Test
    @DisplayName("分页查询 - 有关键词时按用户名模糊查询")
    void testGetUserPage_withKeyword_filtersByUsername() {
        SysUser user2 = new SysUser();
        user2.setId(2L);
        user2.setUsername("testuser2");
        user2.setNickname("测试用户2");
        user2.setStatus(1);

        Page<SysUser> expectedPage = new Page<>(1, 10, 2);
        expectedPage.setRecords(List.of(testUser, user2));
        when(sysUserMapper.selectPage(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(expectedPage);

        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .like(SysUser::getUsername, "test")
                .orderByDesc(SysUser::getCreateTime);

        Page<SysUser> result = sysUserService.page(new Page<>(1, 10), wrapper);

        assertThat(result.getRecords()).hasSize(2);
        assertThat(result.getTotal()).isEqualTo(2);
    }

    @Test
    @DisplayName("创建用户 - 保存成功")
    void testCreateUser_success() {
        when(sysUserMapper.insert(any(SysUser.class))).thenReturn(1);

        boolean result = sysUserService.save(testUser);

        assertThat(result).isTrue();
        verify(sysUserMapper).insert(testUser);
    }

    @Test
    @DisplayName("更新用户 - 成功更新")
    void testUpdateUser_success() {
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        boolean result = sysUserService.updateById(testUser);

        assertThat(result).isTrue();
        verify(sysUserMapper).updateById(testUser);
    }

    @Test
    @DisplayName("更新用户 - 不存在时返回false")
    void testUpdateUser_notFound_returnsFalse() {
        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(0);

        boolean result = sysUserService.updateById(testUser);

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("删除用户 - 成功删除")
    void testDeleteUser_success() {
        try (MockedStatic<TableInfoHelper> helper = mockStatic(TableInfoHelper.class)) {
            TableInfo tableInfo = mock(TableInfo.class);
            when(tableInfo.isWithLogicDelete()).thenReturn(false);
            helper.when(() -> TableInfoHelper.getTableInfo(any(Class.class))).thenReturn(tableInfo);
            when(sysUserMapper.deleteById(1L)).thenReturn(1);

            boolean result = sysUserService.removeById(1L);

            assertThat(result).isTrue();
            verify(sysUserMapper).deleteById(1L);
        }
    }

    @Test
    @DisplayName("删除用户 - 不存在时返回false")
    void testDeleteUser_notFound_returnsFalse() {
        try (MockedStatic<TableInfoHelper> helper = mockStatic(TableInfoHelper.class)) {
            TableInfo tableInfo = mock(TableInfo.class);
            when(tableInfo.isWithLogicDelete()).thenReturn(false);
            helper.when(() -> TableInfoHelper.getTableInfo(any(Class.class))).thenReturn(tableInfo);
            when(sysUserMapper.deleteById(99L)).thenReturn(0);

            boolean result = sysUserService.removeById(99L);

            assertThat(result).isFalse();
        }
    }

    @Test
    @DisplayName("重置密码 - updateById携带新密码")
    void testResetPassword_success() {
        SysUser passwordUpdate = new SysUser();
        passwordUpdate.setId(1L);
        passwordUpdate.setPassword(EncryptUtil.bcrypt("newPassword123"));

        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        boolean result = sysUserService.updateById(passwordUpdate);

        assertThat(result).isTrue();
        verify(sysUserMapper).updateById(argThat(u ->
                u.getId().equals(1L) && u.getPassword() != null));
    }

    @Test
    @DisplayName("根据ID获取用户 - 找到返回用户")
    void testGetUserById_found_returnsUser() {
        when(sysUserMapper.selectById(1L)).thenReturn(testUser);

        SysUser result = sysUserService.getById(1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("根据ID获取用户 - 未找到返回null")
    void testGetUserById_notFound_returnsNull() {
        when(sysUserMapper.selectById(99L)).thenReturn(null);

        SysUser result = sysUserService.getById(99L);

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("按用户名查找 - 通过getOne实现")
    void testFindByUsername_userFound() {
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(testUser);

        SysUser result = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "testuser"));

        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("testuser");
    }

    @Test
    @DisplayName("按用户名查找 - 不存在返回null")
    void testFindByUsername_notFound_returnsNull() {
        when(sysUserMapper.selectOne(any(), anyBoolean())).thenReturn(null);

        SysUser result = sysUserService.getOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "unknown"));

        assertThat(result).isNull();
    }

    @Test
    @DisplayName("更新用户状态 - 启用")
    void testUpdateStatus_enable() {
        SysUser statusUpdate = new SysUser();
        statusUpdate.setId(1L);
        statusUpdate.setStatus(1);

        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        boolean result = sysUserService.updateById(statusUpdate);

        assertThat(result).isTrue();
        verify(sysUserMapper).updateById(argThat(u ->
                u.getId().equals(1L) && u.getStatus() == 1));
    }

    @Test
    @DisplayName("更新用户状态 - 禁用")
    void testUpdateStatus_disable() {
        SysUser statusUpdate = new SysUser();
        statusUpdate.setId(1L);
        statusUpdate.setStatus(0);

        when(sysUserMapper.updateById(any(SysUser.class))).thenReturn(1);

        boolean result = sysUserService.updateById(statusUpdate);

        assertThat(result).isTrue();
        verify(sysUserMapper).updateById(argThat(u ->
                u.getId().equals(1L) && u.getStatus() == 0));
    }
}
