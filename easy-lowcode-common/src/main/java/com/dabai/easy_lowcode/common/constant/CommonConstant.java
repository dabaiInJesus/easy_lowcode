package com.dabai.easy_lowcode.common.constant;

/**
 * 通用常量
 */
public interface CommonConstant {
    
    /**
     * UTF-8 编码
     */
    String UTF8 = "UTF-8";
    
    /**
     * 成功标记
     */
    Integer SUCCESS = 200;
    
    /**
     * 失败标记
     */
    Integer FAIL = 500;
    
    /**
     * 默认页码
     */
    Integer DEFAULT_PAGE_NUM = 1;
    
    /**
     * 默认每页大小
     */
    Integer DEFAULT_PAGE_SIZE = 10;
    
    /**
     * JWT Token 前缀
     */
    String TOKEN_PREFIX = "Bearer ";
    
    /**
     * 登录用户信息键
     */
    String LOGIN_USER_KEY = "loginUser";
}
