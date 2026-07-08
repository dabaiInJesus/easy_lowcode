package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.mapper.SysUserMapper;
import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.security.LoginUser;
import com.dabai.easy_lowcode.common.security.TokenBlacklistService;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final JdbcTemplate jdbcTemplate;
    
    @Override
    public String login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);
        
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        if (!EncryptUtil.verifyPassword(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        
        String token = jwtUtil.generateToken(user.getId(), user.getUsername());
        log.info("用户登录成功: {}", username);
        return token;
    }
    
    @Override
    public void logout() {
        jakarta.servlet.http.HttpServletRequest request = 
                ((org.springframework.web.context.request.ServletRequestAttributes) 
                org.springframework.web.context.request.RequestContextHolder.getRequestAttributes()).getRequest();
        if (request != null) {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                tokenBlacklistService.blacklist(token);
            }
        }
        SecurityContextHolder.clearContext();
        log.info("用户登出成功");
    }
    
    @Override
    public SysUser getCurrentUser() {
        SysUser user = getCurrentUserEntity();
        if (user != null) {
            user.setPassword(null);
        }
        return user;
    }
    
    @Override
    public UserDetails loadUserById(Long userId) {
        SysUser user = this.getById(userId);
        if (user == null) {
            return null;
        }

        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        // Load user's roles from sys_user_role
        String roleSql = "SELECT r.role_code FROM sys_role r " +
                "INNER JOIN sys_user_role ur ON r.id = ur.role_id " +
                "WHERE ur.user_id = ? AND r.deleted = 0";
        List<String> roleCodes = jdbcTemplate.queryForList(roleSql, String.class, userId);

        for (String roleCode : roleCodes) {
            authorities.add(new SimpleGrantedAuthority("ROLE_" + roleCode));
        }

        // Load permissions from menus assigned via roles
        if (!roleCodes.isEmpty()) {
            String permSql = "SELECT DISTINCT m.perms FROM sys_menu m " +
                    "INNER JOIN sys_role_menu rm ON m.id = rm.menu_id " +
                    "INNER JOIN sys_user_role ur ON rm.role_id = ur.role_id " +
                    "WHERE ur.user_id = ? AND m.deleted = 0 " +
                    "AND m.perms IS NOT NULL AND m.perms != ''";
            List<String> perms = jdbcTemplate.queryForList(permSql, String.class, userId);
            for (String perm : perms) {
                authorities.add(new SimpleGrantedAuthority(perm));
            }
        }

        // Fallback: if no roles assigned, give basic ROLE_USER
        if (authorities.isEmpty()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == 1,
                true,
                true,
                true,
                authorities
        );
    }
    
    private SysUser getCurrentUserEntity() {
        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof LoginUser loginUser) {
            return this.getById(loginUser.getUserId());
        }
        return null;
    }
}