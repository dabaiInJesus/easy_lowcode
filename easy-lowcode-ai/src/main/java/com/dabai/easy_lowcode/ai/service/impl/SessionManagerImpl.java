package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 会话管理器实现
 * 使用内存存储会话，支持 30 分钟过期自动清理
 */
@Slf4j
@Component
public class SessionManagerImpl implements SessionManager {

    private static final long SESSION_TIMEOUT_MS = 30 * 60 * 1000L;

    private final Map<String, ChatSession> sessionMap = new ConcurrentHashMap<>();

    @Override
    public void addMessage(String agentCode, String sessionId, String role, String content) {
        if (content == null || content.isBlank()) {
            return;
        }
        String key = buildSessionKey(agentCode, sessionId);
        ChatSession session = sessionMap.computeIfAbsent(key, k -> new ChatSession());
        session.messages.add(Map.of("role", role, "content", content));
        session.lastAccessTime = System.currentTimeMillis();
    }

    @Override
    public List<Map<String, String>> getHistory(String agentCode, String sessionId) {
        String key = buildSessionKey(agentCode, sessionId);
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

    private String buildSessionKey(String agentCode, String sessionId) {
        return agentCode + ":" + (sessionId != null ? sessionId : "default");
    }

    private static class ChatSession {
        List<Map<String, String>> messages = new ArrayList<>();
        long lastAccessTime = System.currentTimeMillis();
    }
}
