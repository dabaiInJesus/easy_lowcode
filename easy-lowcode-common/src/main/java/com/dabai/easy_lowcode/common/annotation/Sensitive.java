package com.dabai.easy_lowcode.common.annotation;

import java.lang.annotation.*;

/**
 * 数据脱敏注解
 * 用于实体字段，自动对敏感数据进行脱敏处理
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Sensitive {
    
    /**
     * 脱敏类型
     */
    SensitiveType type() default SensitiveType.DEFAULT;
    
    /**
     * 自定义脱敏规则（当type为CUSTOM时使用）
     * 格式: 前缀保留:后缀保留
     * 示例: "3:4" 表示保留前3位和后4位
     */
    String customRule() default "";
    
    /**
     * 脱敏字符
     */
    char maskChar() default '*';
    
    /**
     * 脱敏后显示的位数（用于固定长度脱敏）
     */
    int visibleLength() default 4;
}
