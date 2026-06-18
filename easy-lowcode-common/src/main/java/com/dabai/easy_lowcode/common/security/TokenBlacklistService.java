package com.dabai.easy_lowcode.common.security;

import com.dabai.easy_lowcode.common.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    private static final String BLACKLIST_KEY_PREFIX = "token:blacklist:";

    public void blacklist(String token) {
        try {
            String key = BLACKLIST_KEY_PREFIX + token;
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
            String key = BLACKLIST_KEY_PREFIX + token;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("Failed to check token blacklist: {}", e.getMessage());
            return false;
        }
    }
}
