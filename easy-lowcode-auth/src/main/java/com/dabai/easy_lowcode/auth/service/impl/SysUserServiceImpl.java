package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.mapper.SysUserMapper;
import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.security.LoginUser;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {

    private final JwtUtil jwtUtil;
    
    @Override
    public String login(String username, String password) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);
        
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        log.debug("用户输入密码: {}", password);
        log.debug("数据库存储密码: {}", user.getPassword());
        log.debug("BCrypt验证结果: {}", EncryptUtil.verifyPassword(password, user.getPassword()));
        
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
        return new LoginUser(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                user.getStatus() == 1,
                true,
                true,
                true,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
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