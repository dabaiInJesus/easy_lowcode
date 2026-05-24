package com.dabai.easy_lowcode.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI 会话管理器
 * 负责管理基于 sessionId 的多轮对话会话，支持过期清理
 */
public interface SessionManager {

    /**
     * 添加消息到会话
     */
    void addMessage(String agentCode, String sessionId, String role, String content);

    /**
     * 获取会话历史
     */
    List<Map<String, String>> getHistory(String agentCode, String sessionId);

    /**
     * 清除指定会话
     */
    void clearSession(String agentCode, String sessionId);

    /**
     * 清理过期会话（30分钟未访问）
     */
    int cleanupExpiredSessions();
}
