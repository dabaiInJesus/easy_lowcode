package com.dabai.easy_lowcode.ai.service;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;

/**
 * AI 服务统一接口
 */
public interface AiService {
    
    /**
     * 聊天对话
     * 
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);
    
    /**
     * 简单文本对话
     * 
     * @param message 用户消息
     * @return AI 回复
     */
    default String simpleChat(String message) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        ChatResponse response = chat(request);
        return response.getContent();
    }
}
