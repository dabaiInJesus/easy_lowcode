package com.dabai.easy_lowcode.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;
    private static final String SECRET = "test-secret-key-for-unit-tests-32ch";

    @BeforeEach
    void setUp() throws Exception {
        jwtUtil = new JwtUtil(SECRET);
        Field expirationField = JwtUtil.class.getDeclaredField("jwtExpiration");
        expirationField.setAccessible(true);
        expirationField.set(jwtUtil, 2592000000L);
    }

    @Test
    void generateToken_returnsNonNullNonEmpty() {
        String token = jwtUtil.generateToken(1L, "admin");
        assertThat(token).isNotNull().isNotBlank();
    }

    @Test
    void generateToken_containsCorrectUserId() {
        String token = jwtUtil.generateToken(42L, "admin");
        Long userId = jwtUtil.getUserIdFromToken(token);
        assertThat(userId).isEqualTo(42L);
    }

    @Test
    void generateToken_containsCorrectUsername() {
        String token = jwtUtil.generateToken(1L, "testuser");
        String username = jwtUtil.getUsernameFromToken(token);
        assertThat(username).isEqualTo("testuser");
    }

    @Test
    void validateToken_returnsTrueForValidToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_returnsFalseForTamperedToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        String tampered = token.substring(0, token.length() - 3) + "XYZ";
        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    void validateToken_returnsFalseForDifferentKey() {
        JwtUtil otherUtil = new JwtUtil("another-secret-key-for-testing-32");
        String token = otherUtil.generateToken(1L, "admin");
        assertThat(jwtUtil.validateToken(token)).isFalse();
    }

    @Test
    void getUserIdFromToken_returnsNullForInvalidToken() {
        assertThat(jwtUtil.getUserIdFromToken("invalid.token.here")).isNull();
    }

    @Test
    void getUsernameFromToken_returnsNullForInvalidToken() {
        assertThat(jwtUtil.getUsernameFromToken("invalid.token.here")).isNull();
    }

    @Test
    void getExpirationFromToken_returnsFutureDateForValidToken() {
        String token = jwtUtil.generateToken(1L, "admin");
        Date expiration = jwtUtil.getExpirationFromToken(token);
        assertThat(expiration).isNotNull().isAfter(new Date());
    }

    @Test
    void getExpirationFromToken_returnsNullForInvalidToken() {
        assertThat(jwtUtil.getExpirationFromToken("invalid.token.here")).isNull();
    }
}
