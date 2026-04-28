package com.dabai.easy_lowcode.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Alibaba 模型配置
 * 注意：具体的 ChatModel Bean 需要根据实际的 Spring AI Alibaba API 来配置
 */
@Slf4j
@Configuration
public class AiAlibabaModelConfig {
    
    @Value("${ai.provider.default:openai}")
    private String defaultProvider;
    
    public AiAlibabaModelConfig() {
        log.info("AI Alibaba 模型配置类已加载，默认提供商: {}", defaultProvider);
    }
    
    // 注意：由于 Spring AI Alibaba 1.2.2.0 的具体 API 结构可能有所不同
    // 实际的 ChatModel Bean 配置需要根据官方文档进行调整
    // 这里提供的是配置框架，具体实现需要在依赖可用后完善
}
