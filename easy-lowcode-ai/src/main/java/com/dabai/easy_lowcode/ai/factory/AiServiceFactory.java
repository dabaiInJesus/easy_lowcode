package com.dabai.easy_lowcode.ai.factory;

import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 服务工厂
 *
 * 使用 Map 注入，根据 AiProvider 自动匹配对应的 AiService 实现
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiServiceFactory {

    private final List<AiService> aiServices;

    private final Map<AiProvider, AiService> serviceCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("初始化 AI 服务工厂，注册服务数量: {}", aiServices.size());
        for (AiService service : aiServices) {
            AiProvider provider = service.getProvider();
            serviceCache.put(provider, service);
            log.info("注册 AI 服务: {} -> {}", provider.getCode(), service.getClass().getSimpleName());
        }
        log.info("AI 服务缓存初始化完成，共缓存 {} 个服务", serviceCache.size());
    }

    /**
     * 根据厂商标识获取 AI 服务
     */
    public AiService getService(AiProvider provider) {
        return serviceCache.computeIfAbsent(provider, p -> {
            for (AiService service : aiServices) {
                if (service.getProvider() == p) {
                    return service;
                }
            }
            throw new IllegalArgumentException("未找到 " + p.getName() + " 的服务实现");
        });
    }

    /**
     * 获取默认 AI 服务（返回第一个注册的服务）
     */
    public AiService getDefaultService() {
        return aiServices.isEmpty() ? null : aiServices.get(0);
    }

    /**
     * 检查是否支持某个厂商
     */
    public boolean supports(AiProvider provider) {
        return aiServices.stream().anyMatch(s -> s.getProvider() == provider);
    }

    /**
     * 获取所有已注册的厂商
     */
    public List<AiProvider> getSupportedProviders() {
        return aiServices.stream()
                .map(AiService::getProvider)
                .toList();
    }
}
