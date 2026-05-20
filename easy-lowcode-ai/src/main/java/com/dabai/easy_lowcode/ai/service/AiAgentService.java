package com.dabai.easy_lowcode.ai.service;

import java.util.List;
import java.util.Map;

import reactor.core.publisher.Flux;

/**
 * AI Agent 服务接口
 */
public interface AiAgentService {

    /**
     * 执行 Agent 任务
     *
     * @param agentCode Agent 编码
     * @param task 任务描述
     * @return 执行结果
     */
    String executeAgent(String agentCode, String task);

    /**
     * 流式执行 Agent 任务
     *
     * @param agentCode Agent 编码
     * @param task 任务描述
     * @return 流式响应
     */
    Flux<String> executeAgentStream(String agentCode, String task);

    /**
     * 获取所有可用的 Agent
     *
     * @return Agent 列表
     */
    List<Map<String, Object>> listAgents();

    /**
     * 创建自定义 Agent
     *
     * @param name Agent 名称
     * @param description Agent 描述
     * @param instructions Agent 指令
     * @return Agent 编码
     */
    String createAgent(String name, String description, String instructions);

    /**
     * 获取 Agent 对话历史
     *
     * @param agentCode Agent 编码
     * @param sessionId 会话 ID
     * @return 消息列表
     */
    List<Map<String, String>> getChatHistory(String agentCode, String sessionId);

    /**
     * 清除指定会话
     *
     * @param agentCode Agent 编码
     * @param sessionId 会话 ID
     */
    void clearSession(String agentCode, String sessionId);
}
