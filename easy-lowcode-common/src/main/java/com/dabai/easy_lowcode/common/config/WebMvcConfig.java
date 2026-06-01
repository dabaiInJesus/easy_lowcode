package com.dabai.easy_lowcode.common.config;

import com.dabai.easy_lowcode.common.interceptor.ApiResponseInterceptor;
import com.dabai.easy_lowcode.common.interceptor.RequestTimingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置
 */
@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RequestTimingInterceptor requestTimingInterceptor;
    private final ApiResponseInterceptor apiResponseInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 请求计时拦截器
        registry.addInterceptor(requestTimingInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/error");

        // API响应日志拦截器
        registry.addInterceptor(apiResponseInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/swagger-ui/**", "/v3/api-docs/**", "/error");
    }
}