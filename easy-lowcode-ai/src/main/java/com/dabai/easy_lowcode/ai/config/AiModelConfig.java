package com.dabai.easy_lowcode.ai.config;

import com.openai.client.OpenAIClient;
import com.openai.client.OpenAIClientImpl;
import com.openai.core.ClientOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
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
 * Spring AI Alibaba Starter 会自动配置 dashscopeChatModel，
 * 本配置仅用于 DeepSeek / Minimax / Ollama 等非阿里云厂商。
 */
@Slf4j
@Configuration
public class AiModelConfig {

    private OpenAIClient createOpenAiClient(String apiKey, String baseUrl) {
        return new OpenAIClientImpl(
                ClientOptions.builder()
                        .apiKey(apiKey)
                        .baseUrl(baseUrl)
                        .build()
        );
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
        return OpenAiChatModel.builder()
                .openAiClient(createOpenAiClient(apiKey, baseUrl))
                .options(OpenAiChatOptions.builder().model(model).build())
                .build();
    }

    // ==================== Minimax ChatModel ====================

    @Bean
    @ConditionalOnProperty(name = "ai.minimax.enabled", havingValue = "true")
    @ConditionalOnMissingBean(name = "minimaxChatModel")
    public ChatModel minimaxChatModel(
            @Value("${ai.minimax.base-url:https://api.minimax.chat/v1}") String baseUrl,
            @Value("${ai.minimax.api-key:}") String apiKey,
            @Value("${ai.minimax.model:abab6.5s-chat}") String model) {
        log.info("初始化 Minimax ChatModel, baseUrl={}, model={}", baseUrl, model);
        String url = baseUrl.endsWith("/v1") ? baseUrl : baseUrl + "/v1";
        return OpenAiChatModel.builder()
                .openAiClient(createOpenAiClient(apiKey, url))
                .options(OpenAiChatOptions.builder().model(model).build())
                .build();
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
                .options(OllamaChatOptions.builder().model(model).build())
                .build();
    }

    // ==================== RestTemplate ====================

    @Bean
    @ConditionalOnMissingBean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
