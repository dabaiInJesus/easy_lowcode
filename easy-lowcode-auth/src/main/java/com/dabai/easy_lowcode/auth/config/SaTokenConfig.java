package com.dabai.easy_lowcode.auth.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 配置
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {
    
    /**
     * 注册 Sa-Token 拦截器
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验
        registry.addInterceptor(new SaInterceptor(handle -> {
            // 指定一条 match 规则
            SaRouter.match("/**")
                // 排除登录接口
                .notMatch("/api/auth/login")
                // 排除修复密码接口（临时）
                .notMatch("/api/auth/fix-admin-password")
                // 排除生成BCrypt密码接口
                .notMatch("/api/auth/generate-bcrypt")
                // 排除静态资源
                .notMatch("/*.html", "/*.js", "/*.css", "/*.ico", "/static/**", "/public/**", "/favicon.ico")
                // 其他接口都需要登录
                .check(r -> StpUtil.checkLogin());
        })).addPathPatterns("/**");
    }
}
