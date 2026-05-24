package com.dabai.easy_lowcode.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.auth.entity.SysUser;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 用户服务接口
 */
public interface SysUserService extends IService<SysUser> {
    
    /**
     * 用户登录
     */
    String login(String username, String password);
    
    /**
     * 用户登出
     */
    void logout();
    
    /**
     * 获取当前登录用户信息
     */
    SysUser getCurrentUser();
    
    /**
     * 根据用户ID加载用户(用于Spring Security)
     */
    UserDetails loadUserById(Long userId);
}
