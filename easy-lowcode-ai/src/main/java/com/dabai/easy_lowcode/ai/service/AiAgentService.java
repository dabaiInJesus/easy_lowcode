package com.dabai.easy_lowcode.ai.service;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 服务接口
 */
public interface AiAgentService {
    
    /**
     * 执行 Agent 任务
     * 
     * @param agentName Agent 名称
     * @param task 任务描述
     * @return 执行结果
     */
    String executeAgent(String agentName, String task);
    
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
     * @return Agent ID
     */
    String createAgent(String name, String description, String instructions);
}
