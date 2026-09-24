package com.dabai.easy_lowcode.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * AI 模型配置类
 *
 * 统一管理所有 ChatModel Bean。
 * openAiChatModel 由 spring-ai-starter-model-openai 自动装配；
 * DashScope 走官方 OpenAI 兼容模式（compatible-mode），不再依赖 Spring AI Alibaba；
 * 本配置用于 DashScope / DeepSeek / Minimax / Ollama 等厂商。
 */
@Slf4j
@Configuration
public class AiModelConfig {

    /**
     * 构建 OpenAI 协议兼容的 OpenAiApi。
     * baseUrl 归一化规则：剥掉尾部斜杠与末尾的 /v1，实际请求路径由
     * OpenAiApi 默认 completionsPath(/v1/chat/completions) 拼接。
     */
    private OpenAiApi createOpenAiApi(String apiKey, String baseUrl) {
        String normalized = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        if (normalized.endsWith("/v1")) {
            normalized = normalized.substring(0, normalized.length() - 3);
        }
        return OpenAiApi.builder()
                .apiKey(apiKey == null ? "" : apiKey)
                .baseUrl(normalized)
                .build();
    }

    /**
     * 构建 OpenAI 协议兼容的 ChatModel（OpenAI / DashScope / DeepSeek / Minimax 等厂商通用）
     */
    private ChatModel createCompatibleChatModel(String apiKey, String baseUrl, String model) {
        return OpenAiChatModel.builder()
                .openAiApi(createOpenAiApi(apiKey, baseUrl))
                .defaultOptions(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    // ==================== DashScope（通义千问）ChatModel ====================
    // 走 DashScope 官方 OpenAI 兼容模式，替代原 Spring AI Alibaba Starter 自动装配
    @Bean
    @ConditionalOnProperty(name = "ai.dashscope.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "dashScopeChatModel")
    public ChatModel dashScopeChatModel(
            @Value("${ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode}") String baseUrl,
            @Value("${ai.dashscope.api-key:}") String apiKey,
            @Value("${ai.dashscope.model:qwen-turbo}") String model) {
        log.info("初始化 DashScope ChatModel（OpenAI 兼容模式）, baseUrl={}, model={}", baseUrl, model);
        return createCompatibleChatModel(apiKey, baseUrl, model);
    }

    // ==================== DeepSeek ChatModel ====================
    @Bean
    @ConditionalOnProperty(name = "ai.deepseek.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "deepSeekChatModel")
    public ChatModel deepSeekChatModel(
            @Value("${ai.deepseek.base-url:https://api.deepseek.com}") String baseUrl,
            @Value("${ai.deepseek.api-key:}") String apiKey,
            @Value("${ai.deepseek.model:deepseek-chat}") String model) {
        log.info("初始化 DeepSeek ChatModel, baseUrl={}, model={}", baseUrl, model);
        return createCompatibleChatModel(apiKey, baseUrl, model);
    }

    // ==================== Minimax ChatModel ====================

    @Bean
    @ConditionalOnProperty(name = "ai.minimax.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "minimaxChatModel")
    public ChatModel minimaxChatModel(
            @Value("${ai.minimax.base-url:https://api.minimax.chat/v1}") String baseUrl,
            @Value("${ai.minimax.api-key:}") String apiKey,
            @Value("${ai.minimax.model:abab6.5s-chat}") String model) {
        log.info("初始化 Minimax ChatModel（OpenAI 兼容模式）, baseUrl={}, model={}", baseUrl, model);
        return createCompatibleChatModel(apiKey, baseUrl, model);
    }

    // ==================== Ollama ChatModel ====================
    @Bean
    @ConditionalOnProperty(name = "ai.ollama.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "ollamaChatModel")
    public ChatModel ollamaChatModel(
            @Value("${ai.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${ai.ollama.model:llama2}") String model) {
        log.info("初始化 Ollama ChatModel, baseUrl={}, model={}", baseUrl, model);
        OllamaApi ollamaApi = OllamaApi.builder()
                .baseUrl(baseUrl)
                .build();
        return OllamaChatModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(OllamaChatOptions.builder().model(model).build())
                .build();
    }

    // ==================== RestTemplate ====================

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
