package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.service.SessionManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * AI 会话管理器实现
 * 优先使用 Redis 持久化会话（24小时TTL），不可用时降级为内存存储
 */
@Slf4j
@Component
public class SessionManagerImpl implements SessionManager {

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L;
    private static final long REDIS_TTL_HOURS = 24;
    private static final String SESSION_KEY_PREFIX = "ai:session:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final Map<String, ChatSession> sessionMap = new ConcurrentHashMap<>();
    private volatile boolean redisAvailable;

    public SessionManagerImpl(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.redisAvailable = testRedisConnection();
    }

    private boolean testRedisConnection() {
        try {
            redisTemplate.hasKey("__health_check__");
            log.info("Redis 连接正常，启用 Redis 会话存储");
            return true;
        } catch (Exception e) {
            log.warn("Redis 不可用，降级为内存会话存储: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public void addMessage(String agentCode, String sessionId, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String key = buildSessionKey(agentCode, sessionId);

        if (redisAvailable) {
            try {
                addMessageToRedis(key, role, content);
                return;
            } catch (Exception e) {
                log.warn("Redis 写入失败，降级为内存存储: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        ChatSession session = sessionMap.computeIfAbsent(key, k -> new ChatSession());
        session.messages.add(Map.of("role", role, "content", content));
        session.lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public List<Map<String, String>> getHistory(String agentCode, String sessionId) {
        String key = buildSessionKey(agentCode, sessionId);

        if (redisAvailable) {
            try {
                return getHistoryFromRedis(key);
            } catch (Exception e) {
                log.warn("Redis 读取失败，降级为内存存储: {}", e.getMessage());
                redisAvailable = false;
            }
        }

        ChatSession session = sessionMap.get(key);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.messages;
    }

    @Override
    public void clearSession(String agentCode, String sessionId) {
        String key = buildSessionKey(agentCode, sessionId);
        sessionMap.remove(key);

        if (redisAvailable) {
            try {
                redisTemplate.delete(SESSION_KEY_PREFIX + key);
            } catch (Exception e) {
                log.warn("Redis 删除失败: {}", e.getMessage());
            }
        }
        log.info("清除会话: agent={}, sessionId={}", agentCode, sessionId);
    }

    @Override
    public int cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int before = sessionMap.size();
        sessionMap.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccessTime > SESSION_TIMEOUT_MS);
        int removed = before - sessionMap.size();
        if (removed > 0) {
            log.debug("清理过期会话 {} 个，剩余 {} 个", removed, sessionMap.size());
        }
        return removed;
    }

    private void addMessageToRedis(String key, String role, String content) throws JsonProcessingException {
        String redisKey = SESSION_KEY_PREFIX + key;
        String json = redisTemplate.opsForValue().get(redisKey);
        List<Map<String, String>> messages;
        if (json != null) {
            messages = objectMapper.readValue(json, new TypeReference<>() {});
        } else {
            messages = new ArrayList<>();
        }
        messages.add(Map.of("role", role, "content", content));
        redisTemplate.opsForValue().set(redisKey, objectMapper.writeValueAsString(messages),
                REDIS_TTL_HOURS, TimeUnit.HOURS);
    }

    private List<Map<String, String>> getHistoryFromRedis(String key) throws JsonProcessingException {
        String redisKey = SESSION_KEY_PREFIX + key;
        String json = redisTemplate.opsForValue().get(redisKey);
        if (json == null) {
            return Collections.emptyList();
        }
        return objectMapper.readValue(json, new TypeReference<>() {});
    }

    private String buildSessionKey(String agentCode, String sessionId) {
        return agentCode + ":" + (sessionId != null ? sessionId : "default");
    }

    private static class ChatSession {
        List<Map<String, String>> messages = new ArrayList<>();
        long lastAccessTime = System.currentTimeMillis();
    }
}
