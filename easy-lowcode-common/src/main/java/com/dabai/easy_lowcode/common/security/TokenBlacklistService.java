package com.dabai.easy_lowcode.common.security;

import com.dabai.easy_lowcode.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";
    private static final String SHA_256 = "SHA-256";

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance(SHA_256);
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not available", e);
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    public void blacklist(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + hashToken(token);
            Date expiration = jwtUtil.getExpirationFromToken(token);
            if (expiration != null) {
                long ttl = expiration.getTime() - System.currentTimeMillis();
                if (ttl > 0) {
                    redisTemplate.opsForValue().set(key, "blacklisted", ttl, TimeUnit.MILLISECONDS);
                }
            }
            log.info("Token added to blacklist");
        } catch (Exception e) {
            log.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    public boolean isBlacklisted(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + hashToken(token);
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check token blacklist: {}", e.getMessage());
            return true; // Fail closed: treat token as blacklisted if Redis unavailable
        }
    }
}
