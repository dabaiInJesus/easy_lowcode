package com.dabai.easy_lowcode.ai.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * AI Provider 健康检查
 * <p>
 * 检查各 AI 提供商的连通性，在 /actuator/health 中显示详情
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiProviderHealthIndicator implements HealthIndicator {

    private final AiProperties aiProperties;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // 检查 DashScope
        checkDashScope(builder);

        // 检查 OpenAI
        checkOpenAi(builder);

        // 检查 DeepSeek
        checkDeepSeek(builder);

        // 检查 Minimax
        checkMinimax(builder);

        // 检查 Ollama
        checkOllama(builder);

        return builder.build();
    }

    private void checkDashScope(Health.Builder builder) {
        String apiKey = aiProperties.getDashscope().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            builder.withDetail("dashscope", "未配置（请设置 DASHSCOPE_API_KEY 环境变量）");
        } else {
            builder.withDetail("dashscope", "已配置（模型: " + aiProperties.getDashscope().getModel() + "）");
        }
    }

    private void checkOpenAi(Health.Builder builder) {
        String apiKey = aiProperties.getOpenai().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("sk-pla")) {
            builder.withDetail("openai", "未配置（请设置 OPENAI_API_KEY 环境变量）");
        } else {
            builder.withDetail("openai", "已配置（模型: " + aiProperties.getOpenai().getModel() + "）");
        }
    }

    private void checkDeepSeek(Health.Builder builder) {
        String apiKey = aiProperties.getDeepseek().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            builder.withDetail("deepseek", "未配置（请设置 DEEPSEEK_API_KEY 环境变量）");
        } else {
            builder.withDetail("deepseek", "已配置（模型: " + aiProperties.getDeepseek().getModel() + "）");
        }
    }

    private void checkMinimax(Health.Builder builder) {
        String apiKey = aiProperties.getMinimax().getApiKey();
        if (apiKey == null || apiKey.isBlank() || apiKey.startsWith("your-")) {
            builder.withDetail("minimax", "未配置（请设置 MINIMAX_API_KEY 环境变量）");
        } else {
            builder.withDetail("minimax", "已配置（模型: " + aiProperties.getMinimax().getModel() + "）");
        }
    }

    private void checkOllama(Health.Builder builder) {
        String baseUrl = aiProperties.getOllama().getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            builder.withDetail("ollama", "未配置");
        } else {
            builder.withDetail("ollama", "已配置（地址: " + baseUrl + "，模型: " + aiProperties.getOllama().getModel() + "）");
        }
    }
}
