package com.dabai.easy_lowcode.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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
    @NotBlank(message = "消息内容不能为空")
    @Size(max = 10000, message = "消息内容不能超过10000字符")
    private String message;
    
    /**
     * 系统提示词
     */
    private String systemPrompt;
    
    /**
     * 温度参数 (0-2)，控制随机性
     */
    @Min(value = 0, message = "温度参数最小为0")
    @Max(value = 2, message = "温度参数最大为2")
    private Double temperature = 0.7;
    
    /**
     * 最大 token 数
     */
    @Min(value = 1, message = "最大token数最小为1")
    @Max(value = 32000, message = "最大token数最大为32000")
    private Integer maxTokens = 2000;
    
    /**
     * 模型名称（可选，使用默认模型）
     */
    private String model;
}
