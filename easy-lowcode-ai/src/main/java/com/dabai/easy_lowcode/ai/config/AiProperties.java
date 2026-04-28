package com.dabai.easy_lowcode.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {
    
    /**
     * 默认 AI 提供商
     */
    private String defaultProvider = "openai";
    
    /**
     * OpenAI 配置
     */
    private OpenAiConfig openai = new OpenAiConfig();
    
    /**
     * 通义千问配置
     */
    private DashScopeConfig dashscope = new DashScopeConfig();
    
    /**
     * 文心一言配置
     */
    private WenxinConfig wenxin = new WenxinConfig();
    
    /**
     * Ollama 配置
     */
    private OllamaConfig ollama = new OllamaConfig();
    
    /**
     * DeepSeek 配置
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();
    
    /**
     * Minimax 配置
     */
    private MinimaxConfig minimax = new MinimaxConfig();
    
    @Data
    public static class OpenAiConfig {
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-3.5-turbo";
    }
    
    @Data
    public static class DashScopeConfig {
        private String apiKey;
        private String model = "qwen-turbo";
    }
    
    @Data
    public static class WenxinConfig {
        private String apiKey;
        private String secretKey;
        private String model = "eb-instant";
    }
    
    @Data
    public static class OllamaConfig {
        private String baseUrl = "http://localhost:11434";
        private String model = "llama2";
    }
    
    @Data
    public static class DeepSeekConfig {
        private String apiKey;
        private String baseUrl = "https://api.deepseek.com/v1";
        private String model = "deepseek-chat";
    }
    
    @Data
    public static class MinimaxConfig {
        private String apiKey;
        private String baseUrl = "https://api.minimax.chat/v1";
        private String model = "abab6-chat";
    }
}
