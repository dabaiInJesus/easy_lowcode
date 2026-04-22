package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AI 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {
    
    private final AiServiceFactory aiServiceFactory;
    
    /**
     * 聊天对话（使用默认 AI 服务）
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", request.getMessage());
        
        try {
            AiService aiService = aiServiceFactory.getDefaultService();
            if (aiService == null) {
                return Result.error("AI 服务未配置");
            }
            
            ChatResponse response = aiService.chat(request);
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("聊天失败", e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 聊天对话（指定 AI 厂商）
     */
    @PostMapping("/chat/{provider}")
    public Result<ChatResponse> chatWithProvider(
            @PathVariable String provider,
            @RequestBody ChatRequest request) {
        log.info("收到聊天请求，厂商: {}, 消息: {}", provider, request.getMessage());
        
        try {
            AiProvider aiProvider = AiProvider.fromCode(provider);
            AiService aiService = aiServiceFactory.getService(aiProvider);
            
            ChatResponse response = aiService.chat(request);
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("聊天失败", e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 简单文本对话
     */
    @PostMapping("/simple-chat")
    public Result<String> simpleChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        
        try {
            AiService aiService = aiServiceFactory.getDefaultService();
            if (aiService == null) {
                return Result.error("AI 服务未配置");
            }
            
            String response = aiService.simpleChat(message);
            return Result.success(response);
            
        } catch (Exception e) {
            log.error("简单聊天失败", e);
            return Result.error("聊天失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取支持的 AI 厂商列表
     */
    @GetMapping("/providers")
    public Result<Map<String, Object>> getProviders() {
        Map<String, Object> providers = new HashMap<>();
        
        for (AiProvider provider : AiProvider.values()) {
            Map<String, String> info = new HashMap<>();
            info.put("code", provider.getCode());
            info.put("name", provider.getName());
            providers.put(provider.getCode(), info);
        }
        
        return Result.success(providers);
    }
}
