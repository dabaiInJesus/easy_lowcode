package com.dabai.easy_lowcode.common.annotation;

import java.lang.annotation.*;

/**
 * 限流注解
 * 用于Controller方法上，实现基于Redis的请求限流
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    
    /**
     * 限流key前缀，默认使用方法名
     */
    String key() default "";
    
    /**
     * 限流时间窗口（秒）
     */
    int window() default 60;
    
    /**
     * 时间窗口内最大请求数
     */
    int maxRequests() default 100;
    
    /**
     * 限流提示消息
     */
    String message() default "请求过于频繁，请稍后再试";
}