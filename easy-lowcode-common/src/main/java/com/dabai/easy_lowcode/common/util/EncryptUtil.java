package com.dabai.easy_lowcode.common.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 加密工具类
 */
public class EncryptUtil {

    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final int GCM_NONCE_LENGTH = 12;
    private static final int GCM_TAG_LENGTH = 128;
    // 旧版本使用的静态 nonce（16 字节），用于兼容解密历史数据
    private static final byte[] LEGACY_NONCE = "LowCodeGCMNonce!".getBytes(StandardCharsets.UTF_8);
    private static final String AES_KEY;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static {
        String key = System.getProperty("encrypt.aes.key");
        if (key == null) key = System.getenv("ENCRYPT_AES_KEY");
        if (key == null || key.isEmpty()) {
            key = "1234567890123456";
            System.err.println("[WARN] ENCRYPT_AES_KEY 未配置，使用默认开发密钥，生产环境必须通过环境变量设置！");
        }
        if (key.length() != 16) {
            throw new RuntimeException("AES密钥长度必须为16位，当前: " + key.length());
        }
        AES_KEY = key;
    }

    public static String md5(String text) {
        return cn.hutool.crypto.SecureUtil.md5(text);
    }

    public static String sha256(String text) {
        return cn.hutool.crypto.SecureUtil.sha256(text);
    }

    public static String bcrypt(String password) {
        return encoder.encode(password);
    }

    public static boolean verifyPassword(String rawPassword, String encodedPassword) {
        try {
            return encoder.matches(rawPassword, encodedPassword);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 加密：使用随机 12 字节 nonce，格式为 v1:Base64(nonce || ciphertext)
     */
    public static String encrypt(String text) {
        try {
            byte[] nonce = new byte[GCM_NONCE_LENGTH];
            SECURE_RANDOM.nextBytes(nonce);
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));
            byte[] encrypted = cipher.doFinal(text.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[GCM_NONCE_LENGTH + encrypted.length];
            System.arraycopy(nonce, 0, combined, 0, GCM_NONCE_LENGTH);
            System.arraycopy(encrypted, 0, combined, GCM_NONCE_LENGTH, encrypted.length);
            return "v1:" + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            throw new RuntimeException("加密失败", e);
        }
    }

    /**
     * 解密：支持新格式（v1: 前缀 + 随机 nonce）和旧格式（静态 nonce）
     */
    public static String decrypt(String encryptedText) {
        try {
            if (encryptedText != null && encryptedText.startsWith("v1:")) {
                byte[] combined = Base64.getDecoder().decode(encryptedText.substring(3));
                byte[] nonce = Arrays.copyOfRange(combined, 0, GCM_NONCE_LENGTH);
                byte[] ciphertext = Arrays.copyOfRange(combined, GCM_NONCE_LENGTH, combined.length);
                SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, nonce));
                return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
            }
            // 旧格式兼容：使用原始静态 nonce
            SecretKeySpec keySpec = new SecretKeySpec(AES_KEY.getBytes(StandardCharsets.UTF_8), "AES");
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, keySpec, new GCMParameterSpec(GCM_TAG_LENGTH, LEGACY_NONCE));
            byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedText));
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("解密失败", e);
        }
    }
}
