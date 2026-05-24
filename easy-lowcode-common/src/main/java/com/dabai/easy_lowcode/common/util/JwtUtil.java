package com.dabai.easy_lowcode.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class JwtUtil {

    private static final String JWT_SECRET = "EasyLowcode@2024#SecretKey$For@JWT$Token$Generation";
    private static final long JWT_EXPIRATION = 2592000000L;

    private static SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
    }

    public static String generateToken(Long userId, String username) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("username", username);

        Date now = new Date();
        Date expiration = new Date(now.getTime() + JWT_EXPIRATION);

        return Jwts.builder()
                .claims(claims)
                .subject(userId.toString())
                .issuedAt(now)
                .expiration(expiration)
                .signWith(getSigningKey())
                .compact();
    }

    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public static Long getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            log.error("Failed to get userId from token: {}", e.getMessage());
            return null;
        }
    }

    public static String getUsernameFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return claims.get("username", String.class);
        } catch (Exception e) {
            log.error("Failed to get username from token: {}", e.getMessage());
            return null;
        }
    }
}