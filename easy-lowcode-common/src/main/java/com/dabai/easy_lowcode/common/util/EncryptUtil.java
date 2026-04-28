package com.dabai.easy_lowcode.common.util;

import cn.hutool.crypto.SecureUtil;

/**
 * 加密工具类
 */
public class EncryptUtil {
    
    /**
     * MD5 加密
     */
    public static String md5(String text) {
        return SecureUtil.md5(text);
    }
    
    /**
     * SHA256 加密
     */
    public static String sha256(String text) {
        return SecureUtil.sha256(text);
    }
    
    /**
     * BCrypt 加密（用于密码）
     */
    public static String bcrypt(String password) {
        // TODO: 使用 Spring Security Crypto 或 jBCrypt
        return SecureUtil.md5(password);
    }
    
    /**
     * 验证密码
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        return md5(rawPassword).equals(encodedPassword);
    }
}
