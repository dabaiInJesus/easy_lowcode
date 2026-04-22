package com.dabai.easy_lowcode.ai.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;

/**
 * AI 智能体服务接口
 */
public interface AiAgentService extends IService<AiAgent> {
    
    /**
     * 执行智能体对话
     * 
     * @param agentCode 智能体编码
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse executeAgent(String agentCode, ChatRequest request);
    
    /**
     * 发布智能体
     * 
     * @param agentId 智能体ID
     */
    void publishAgent(Long agentId);
    
    /**
     * 增加使用次数
     * 
     * @param agentId 智能体ID
     */
    void incrementUsageCount(Long agentId);
}
