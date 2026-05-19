package com.dabai.easy_lowcode.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * 加密工具类
 */
public class EncryptUtil {
    
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * MD5 加密
     */
    public static String md5(String text) {
        return cn.hutool.crypto.SecureUtil.md5(text);
    }
    
    /**
     * SHA256 加密
     */
    public static String sha256(String text) {
        return cn.hutool.crypto.SecureUtil.sha256(text);
    }
    
    /**
     * BCrypt 加密（用于密码）
     */
    public static String bcrypt(String password) {
        return encoder.encode(password);
    }
    
    /**
     * 验证密码
     */
    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        try {
            return encoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            // 如枟 BCrypt 验证失败，返回 false
            return false;
        }
    }
    
    private static String resolveAesKey() {
        String key = System.getProperty("encrypt.aes.key");
        if (key == null) {
            key = System.getenv("ENCRYPT_AES_KEY");
        }
        if (key == null || key.length() != 16) {
            throw new RuntimeException("AES密钥未配置！请设置 -DenCrypt.aes.key=16位密钥 或 ENCRYPT_AES_KEY=16位密钥");
        }
        return key;
    }
    private static final String AES_KEY = resolveAesKey();
    private static final String AES_GCM_NONCE = "LowCodeGCMNonce!";

    public static String encrypt(String text) {
        try {
            byte[] nonce = AES_GCM_NONCE.getBytes();
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new javax.crypto.spec.GCMParameterSpec(128, nonce));
            byte[] encrypted = cipher.doFinal(text.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            byte[] nonce = AES_GCM_NONCE.getBytes();
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new javax.crypto.spec.GCMParameterSpec(128, nonce));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
