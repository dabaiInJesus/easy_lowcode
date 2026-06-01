package com.dabai.easy_lowcode.common.annotation;

/**
 * 脱敏类型枚举
 */
public enum SensitiveType {
    /**
     * 默认脱敏（显示前1后1）
     */
    DEFAULT,
    
    /**
     * 手机号（显示前3后4）
     */
    PHONE,
    
    /**
     * 邮箱（只显示@前1位）
     */
    EMAIL,
    
    /**
     * 身份证（显示前6后4）
     */
    ID_CARD,
    
    /**
     * 姓名（只显示第一个字）
     */
    NAME,
    
    /**
     * 银行卡（显示前6后4）
     */
    BANK_CARD,
    
    /**
     * 地址（只显示省市区）
     */
    ADDRESS,
    
    /**
     * 密码（全部隐藏）
     */
    PASSWORD,
    
    /**
     * 自定义（使用customRule）
     */
    CUSTOM,
    
    /**
     * 不脱敏（用于覆盖父类注解）
     */
    NONE
}
