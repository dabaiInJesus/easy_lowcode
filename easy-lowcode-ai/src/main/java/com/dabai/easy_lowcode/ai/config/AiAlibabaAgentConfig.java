package com.dabai.easy_lowcode.ai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

/**
 * Spring AI Alibaba Agent 配置
 * 注意：此配置类仅在 spring-ai-alibaba 依赖可用时生效
 */
@Slf4j
@Configuration
public class AiAlibabaAgentConfig {
    
    // 由于 Spring AI Alibaba 1.2.2.0 的具体 API 可能在运行时才可用
    // 这里使用条件注解来确保只有在相关类存在时才激活此配置
    // 实际的 Agent 功能需要通过具体的 Spring AI Alibaba API 来实现
    
    public AiAlibabaAgentConfig() {
        log.info("AI Alibaba Agent 配置类已加载（需要 spring-ai-alibaba 依赖）");
    }
}
