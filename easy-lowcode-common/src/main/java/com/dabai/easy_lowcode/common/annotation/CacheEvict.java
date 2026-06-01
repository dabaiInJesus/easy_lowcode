package com.dabai.easy_lowcode.common.annotation;

import java.lang.annotation.*;

/**
 * 缓存更新注解
 * 用于方法上，在方法执行后自动更新缓存
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheEvict {
    
    /**
     * 要清除的缓存key，支持SpEL表达式
     */
    String key();
    
    /**
     * 是否清除所有匹配的前缀key（如: user:*）
     */
    boolean allEntries() default false;
    
    /**
     * 是否在方法执行前清除缓存（默认false，方法执行后清除）
     */
    boolean beforeInvocation() default false;
}