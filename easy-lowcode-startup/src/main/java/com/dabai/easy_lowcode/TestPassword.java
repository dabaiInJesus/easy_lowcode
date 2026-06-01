package com.dabai.easy_lowcode;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class TestPassword {
    public static void main(String[] args) {
        // 数据库中的密码 hash
        String storedHash = "$2a$10$l9Z.7X9AIuzj1gk783KvcORuWBlLCGraRFMTtdAmz7DTALdS0ajjO";
        
        // 要测试的密码
        String password = "123456";
        
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 测试直接匹配
        boolean result1 = encoder.matches(password, storedHash);
        System.out.println("BCrypt 直接匹配结果: " + result1);
        
        // 生成新的 hash
        String newHash = encoder.encode(password);
        System.out.println("123456 的新 hash: " + newHash);
        
        // 验证新 hash
        boolean result2 = encoder.matches(password, newHash);
        System.out.println("新 hash 验证: " + result2);
    }
}