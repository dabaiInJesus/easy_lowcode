package com.dabai.easy_lowcode.auth.filter;

import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.security.LoginUser;
import com.dabai.easy_lowcode.common.security.TokenBlacklistService;
import com.dabai.easy_lowcode.common.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("JwtAuthenticationFilter 单元测试")
class JwtAuthenticationFilterTest {

    @Mock
    private SysUserService userService;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private TokenBlacklistService tokenBlacklistService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FilterChain filterChain;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
        SecurityContextHolder.clearContext();

        userDetails = new LoginUser(1L, "testuser", "pwd",
                true, true, true, true,
                List.of(() -> "ROLE_USER"));
    }

    // ==================== 有效token ====================

    @Test
    @DisplayName("有效token - 设置认证信息并放行")
    void testDoFilterInternal_validToken_setsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer valid-jwt-token");

        when(jwtUtil.validateToken("valid-jwt-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-jwt-token")).thenReturn(false);
        when(jwtUtil.getUserIdFromToken("valid-jwt-token")).thenReturn(1L);
        when(userService.loadUserById(1L)).thenReturn(userDetails);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("testuser");
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 缺少Authorization头 ====================

    @Test
    @DisplayName("缺少Authorization头 - 不设置认证信息，直接放行")
    void testDoFilterInternal_noAuthHeader_doesNotSetAuthentication() throws Exception {
        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString());
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(userService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 不完整的Bearer token ====================

    @Test
    @DisplayName("Authorization头不含Bearer前缀 - 不设置认证信息")
    void testDoFilterInternal_nonBearerAuth_doesNotSetAuthentication() throws Exception {
        request.addHeader("Authorization", "Basic base64credentials");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("空token值 - validateToken被调用并返回false，不设置认证信息")
    void testDoFilterInternal_emptyToken_skipsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer ");
        when(jwtUtil.validateToken("")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil).validateToken("");
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 无效token ====================

    @Test
    @DisplayName("无效token - 验证失败，不设置认证信息")
    void testDoFilterInternal_invalidToken_skipsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer invalid-jwt-token");

        when(jwtUtil.validateToken("invalid-jwt-token")).thenReturn(false);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(tokenBlacklistService, never()).isBlacklisted(anyString());
        verify(userService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 黑名单token ====================

    @Test
    @DisplayName("黑名单中的token - 不设置认证信息")
    void testDoFilterInternal_blacklistedToken_skipsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer blacklisted-token");

        when(jwtUtil.validateToken("blacklisted-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("blacklisted-token")).thenReturn(true);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(userService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 用户不存在 ====================

    @Test
    @DisplayName("token有效但用户不存在 - 不设置认证信息")
    void testDoFilterInternal_tokenValidButUserNotFound_skipsAuthentication() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token-no-user");

        when(jwtUtil.validateToken("valid-token-no-user")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token-no-user")).thenReturn(false);
        when(jwtUtil.getUserIdFromToken("valid-token-no-user")).thenReturn(99L);
        when(userService.loadUserById(99L)).thenReturn(null);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 已存在认证 ====================

    @Test
    @DisplayName("token有效但已存在认证信息 - 不覆盖")
    void testDoFilterInternal_authAlreadyExists_doesNotOverride() throws Exception {
        request.addHeader("Authorization", "Bearer valid-token");
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        "existing", null, List.of()));

        when(jwtUtil.validateToken("valid-token")).thenReturn(true);
        when(tokenBlacklistService.isBlacklisted("valid-token")).thenReturn(false);
        when(jwtUtil.getUserIdFromToken("valid-token")).thenReturn(1L);

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication().getName()).isEqualTo("existing");
        verify(userService, never()).loadUserById(anyLong());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 白名单路径（放行） ====================

    @Test
    @DisplayName("白名单路径 - 无需token直接放行")
    void testDoFilterInternal_whitelistedPath_passesThrough() throws Exception {
        request.setRequestURI("/api/auth/login");
        request.setMethod("POST");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("公开路径 - 无需token直接放行")
    void testDoFilterInternal_publicPath_passesThrough() throws Exception {
        request.setRequestURI("/api/auth/register");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(jwtUtil, never()).validateToken(anyString());
        verify(filterChain).doFilter(request, response);
    }

    // ==================== 非受保护路径 ====================

    @Test
    @DisplayName("静态资源路径 - 无需token直接放行")
    void testDoFilterInternal_staticResourcePath_passesThrough() throws Exception {
        request.setRequestURI("/favicon.ico");

        jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verify(filterChain).doFilter(request, response);
    }
}
