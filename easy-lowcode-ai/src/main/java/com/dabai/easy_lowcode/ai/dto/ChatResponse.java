package com.dabai.easy_lowcode.ai.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * AI 聊天响应
 */
@Data
public class ChatResponse implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 回复内容
     */
    private String content;
    
    /**
     * 使用的模型
     */
    private String model;
    
    /**
     * 消耗的 token 数
     */
    private Usage usage;
    
    @Data
    public static class Usage {
        /**
         * 提示 token 数
         */
        private Integer promptTokens;
        
        /**
         * 完成 token 数
         */
        private Integer completionTokens;
        
        /**
         * 总 token 数
         */
        private Integer totalTokens;
    }
}
