package com.dabai.easy_lowcode;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GeneratePasswordHash {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 生成 123456 的 hash
        String hash1 = encoder.encode("123456");
        System.out.println("123456 的 hash: " + hash1);
        System.out.println("验证 123456: " + encoder.matches("123456", hash1));
        
        // 生成 123456 的 hash  
        String hash2 = encoder.encode("123456");
        System.out.println("123456 的 hash: " + hash2);
        System.out.println("验证 123456: " + encoder.matches("123456", hash2));
    }
}