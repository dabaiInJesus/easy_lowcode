package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.auth.entity.SysUser;
import com.dabai.easy_lowcode.auth.service.SysUserService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final SysUserService userService;
    
    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String token = userService.login(request.getUsername(), request.getPassword());
        
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        
        return Result.success("登录成功", data);
    }
    
    /**
     * 用户登出
     */
    @PostMapping("/logout")
    public Result<Void> logout() {
        userService.logout();
        return Result.success("登出成功", null);
    }
    
    /**
     * 获取当前用户信息
     */
    @GetMapping("/current")
    public Result<SysUser> getCurrentUser() {
        SysUser user = userService.getCurrentUser();
        return Result.success(user);
    }
    
    /**
     * 登录请求DTO
     */
    @lombok.Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}
