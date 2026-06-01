package com.dabai.easy_lowcode.common.annotation;

import java.lang.annotation.*;

/**
 * 业务缓存注解
 * 用于方法级别，实现自动缓存读取和更新
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cacheable {
    
    /**
     * 缓存key，支持SpEL表达式
     * 示例: "user:#{id}" -> 实际拼接 user:123
     */
    String key();
    
    /**
     * 过期时间（秒），默认3600（1小时）
     */
    long expire() default 3600;
    
    /**
     * 缓存条件，SpEL表达式，返回true时才缓存
     */
    String condition() default "";
    
    /**
     * 是否使用分布式锁防止缓存击穿
     */
    boolean distributedLock() default false;
    
    /**
     * 分布式锁超时时间（秒）
     */
    long lockTimeout() default 10;
    
    /**
     * 缓存失败时是否回源到数据库
     */
    boolean fallback() default true;
}