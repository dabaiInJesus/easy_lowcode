package com.dabai.easy_lowcode.ai.factory;

import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 服务工厂
 */
@Component
@RequiredArgsConstructor
public class AiServiceFactory {
    
    private final List<AiService> aiServices;
    private final Map<AiProvider, AiService> serviceCache = new ConcurrentHashMap<>();
    
    /**
     * 根据厂商标识获取 AI 服务
     */
    public AiService getService(AiProvider provider) {
        return serviceCache.computeIfAbsent(provider, this::findService);
    }
    
    /**
     * 获取默认 AI 服务
     */
    public AiService getDefaultService() {
        // 默认返回第一个可用的服务
        return aiServices.isEmpty() ? null : aiServices.get(0);
    }
    
    /**
     * 查找对应的服务实现
     */
    private AiService findService(AiProvider provider) {
        // 根据包名判断服务类型
        String providerName = provider.getCode().toLowerCase();
        
        for (AiService service : aiServices) {
            String className = service.getClass().getName().toLowerCase();
            
            if (provider == AiProvider.OPENAI && className.contains("openai")) {
                return service;
            } else if (provider == AiProvider.DASHSCOPE && className.contains("dashscope")) {
                return service;
            } else if (provider == AiProvider.OLLAMA && className.contains("ollama")) {
                return service;
            } else if (provider == AiProvider.DEEPSEEK && className.contains("deepseek")) {
                return service;
            } else if (provider == AiProvider.MINIMAX && className.contains("minimax")) {
                return service;
            }
            // 可以继续添加其他厂商的判断逻辑
        }
        
        throw new IllegalArgumentException("未找到 " + provider.getName() + " 的服务实现");
    }
}
