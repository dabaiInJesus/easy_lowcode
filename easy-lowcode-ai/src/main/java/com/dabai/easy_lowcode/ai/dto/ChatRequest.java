package com.dabai.easy_lowcode.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 聊天请求
 */
@Data
public class ChatRequest implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 消息内容
     */
    private String message;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 温度参数 (0-2)，控制随机性
     */
    private Double temperature = 0.7;
    
    /**
     * 最大 token 数
     */
    private Integer maxTokens = 2000;
    
    /**
     * 模型名称（可选，使用默认模型）
     */
    private String model;
}
