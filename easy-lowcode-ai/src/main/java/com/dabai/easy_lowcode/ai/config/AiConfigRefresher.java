package com.dabai.easy_lowcode.ai.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.ai.entity.AiConfig;
import com.dabai.easy_lowcode.ai.mapper.AiConfigMapper;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 配置刷新器
 * <p>
 * 负责从 DB 加载 AiConfig 并覆盖/同步到运行时配置。
 * 解决 application.yml 静态配置 与 DB 动态配置不同步的问题。
 * <p>
 * 优先级：DB 配置 > yml 配置
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class AiConfigRefresher {

    private final AiConfigMapper aiConfigMapper;
    private final AiProperties aiProperties;
    private final ApplicationContext applicationContext;

    /** 内存缓存：providerCode -> 加密后的 key */
    private final Map<String, String> encryptedKeyCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void refreshFromDb() {
        try {
            List<AiConfig> configs = aiConfigMapper.selectList(
                    new LambdaQueryWrapper<AiConfig>()
                            .eq(AiConfig::getStatus, 1)
                            .orderByAsc(AiConfig::getSortOrder)
            );

            if (configs == null || configs.isEmpty()) {
                log.info("AiConfig 表为空，使用 yml 配置");
                return;
            }

            log.info("从 DB 加载 {} 条 AI 配置", configs.size());
            for (AiConfig config : configs) {
                applyConfig(config);
            }

            // 标记默认 provider
            configs.stream()
                    .filter(c -> Integer.valueOf(1).equals(c.getIsDefault()))
                    .findFirst()
                    .ifPresent(defaultConfig -> {
                        aiProperties.setDefaultProvider(defaultConfig.getProvider());
                        log.info("默认 AI 供应商设为: {}", defaultConfig.getProvider());
                    });

            log.info("AI 配置刷新完成");
        } catch (Exception e) {
            log.warn("从 DB 加载 AI 配置失败，使用 yml 配置: {}", e.getMessage());
        }
    }

    /**
     * 将单条 DB 配置应用到运行时
     */
    private void applyConfig(AiConfig config) {
        String provider = config.getProvider();
        if (provider == null) return;

        String decryptedKey = decryptKey(config.getApiKey(), provider);

        switch (provider) {
            case "openai" -> {
                if (decryptedKey != null) aiProperties.getOpenai().setApiKey(decryptedKey);
                if (config.getBaseUrl() != null) aiProperties.getOpenai().setBaseUrl(config.getBaseUrl());
                if (config.getModel() != null) aiProperties.getOpenai().setModel(config.getModel());
                log.info("应用 OpenAI DB 配置: model={}", config.getModel());
            }
            case "dashscope" -> {
                if (decryptedKey != null) aiProperties.getDashscope().setApiKey(decryptedKey);
                if (config.getModel() != null) aiProperties.getDashscope().setModel(config.getModel());
                log.info("应用 DashScope DB 配置: model={}", config.getModel());
            }
            case "deepseek" -> {
                if (decryptedKey != null) aiProperties.getDeepseek().setApiKey(decryptedKey);
                if (config.getBaseUrl() != null) aiProperties.getDeepseek().setBaseUrl(config.getBaseUrl());
                if (config.getModel() != null) aiProperties.getDeepseek().setModel(config.getModel());
                log.info("应用 DeepSeek DB 配置: model={}", config.getModel());
            }
            case "ollama" -> {
                if (config.getBaseUrl() != null) aiProperties.getOllama().setBaseUrl(config.getBaseUrl());
                if (config.getModel() != null) aiProperties.getOllama().setModel(config.getModel());
                log.info("应用 Ollama DB 配置: model={}", config.getModel());
            }
            case "minimax" -> {
                if (decryptedKey != null) aiProperties.getMinimax().setApiKey(decryptedKey);
                if (config.getBaseUrl() != null) aiProperties.getMinimax().setBaseUrl(config.getBaseUrl());
                if (config.getModel() != null) aiProperties.getMinimax().setModel(config.getModel());
                log.info("应用 Minimax DB 配置: model={}", config.getModel());
            }
            default -> log.debug("未处理的 provider: {}", provider);
        }
    }

    /**
     * 解密 API Key（从加密字符串还原）
     */
    private String decryptKey(String encryptedKey, String provider) {
        if (encryptedKey == null || encryptedKey.contains("****")) {
            return null; // 前端脱敏后的值不更新
        }
        try {
            return EncryptUtil.decrypt(encryptedKey);
        } catch (Exception e) {
            log.warn("API Key 解密失败 provider={}: {}", provider, e.getMessage());
            return null;
        }
    }

    /**
     * 刷新单条配置（供 AiConfigController 调用）
     */
    public void refreshConfig(AiConfig config) {
        applyConfig(config);
        log.info("运行时 AI 配置已刷新: provider={}", config.getProvider());
    }
}
