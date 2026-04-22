package com.dabai.easy_lowcode.ai.enums;

import lombok.Getter;

/**
 * AI 模型厂商标举
 */
@Getter
public enum AiProvider {
    
    /**
     * OpenAI (GPT-3.5, GPT-4)
     */
    OPENAI("openai", "OpenAI"),
    
    /**
     * 阿里云通义千问
     */
    DASHSCOPE("dashscope", "通义千问"),
    
    /**
     * 百度文心一言
     */
    WENXIN("wenxin", "文心一言"),
    
    /**
     * 腾讯混元
     */
    HUNYUAN("hunyuan", "腾讯混元"),
    
    /**
     * 智谱清言
     */
    ZHIPU("zhipu", "智谱清言"),
    
    /**
     * Moonshot (Kimi)
     */
    MOONSHOT("moonshot", "Moonshot");
    
    private final String code;
    private final String name;
    
    AiProvider(String code, String name) {
        this.code = code;
        this.name = name;
    }
    
    public static AiProvider fromCode(String code) {
        for (AiProvider provider : values()) {
            if (provider.getCode().equals(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("未知的 AI 厂商: " + code);
    }
}
