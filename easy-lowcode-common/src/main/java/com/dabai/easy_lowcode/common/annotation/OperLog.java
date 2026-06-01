package com.dabai.easy_lowcode.common.annotation;

import java.lang.annotation.*;

/**
 * 操作日志注解
 * 用于方法上，自动记录操作日志
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OperLog {
    
    /**
     * 操作模块
     */
    String module() default "";
    
    /**
     * 操作类型
     * CREATE, UPDATE, DELETE, QUERY, EXECUTE, LOGIN, LOGOUT 等
     */
    String type() default "";
    
    /**
     * 操作描述，支持SpEL表达式
     * 示例: "删除用户 #{#userId}"
     */
    String description() default "";
    
    /**
     * 是否记录请求参数
     */
    boolean logParams() default true;
    
    /**
     * 是否记录返回结果
     */
    boolean logResult() default false;
    
    /**
     * 请求参数中需要脱敏的字段名
     */
    String[] sensitiveParams() default {};
    
    /**
     * 成功消息（用于记录）
     */
    String successMsg() default "";
    
    /**
     * 失败消息模板
     */
    String failMsg() default "";
}