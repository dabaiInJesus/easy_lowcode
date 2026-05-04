package com.dabai.easy_lowcode.auth.service.impl;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.mapper.SysUserMapper;
import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserServiceImpl extends ServiceImpl<SysUserMapper, SysUser> implements SysUserService {
    
    @Override
    public String login(String username, String password) {
        // 查询用户
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysUser::getUsername, username);
        SysUser user = this.getOne(wrapper);
        
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 验证密码（使用 BCrypt 比对）
        if (!EncryptUtil.verifyPassword(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        
        // 检查用户状态
        if (user.getStatus() == 0) {
            throw new BusinessException("用户已被禁用");
        }
        
        // 登录成功，生成token
        StpUtil.login(user.getId());
        String token = StpUtil.getTokenValue();
        
        log.info("用户登录成功: {}", username);
        return token;
    }
    
    @Override
    public void logout() {
        StpUtil.logout();
        log.info("用户登出成功");
    }
    
    @Override
    public SysUser getCurrentUser() {
        Long userId = StpUtil.getLoginIdAsLong();
        return this.getById(userId);
    }
}
