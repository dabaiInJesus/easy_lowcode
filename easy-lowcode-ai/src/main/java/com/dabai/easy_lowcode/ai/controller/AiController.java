package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 聊天控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiServiceFactory aiServiceFactory;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // ==================== 聊天接口 ====================

    /**
     * 聊天对话（使用默认 AI 服务）
     */
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("收到聊天请求: {}", request.getMessage());

        AiService aiService = aiServiceFactory.getDefaultService();
        if (aiService == null) {
            return Result.error("AI 服务未配置，请先在配置文件中启用至少一个 AI 厂商");
        }

        ChatResponse response = aiService.chat(request);
        return Result.success(response);
    }

    /**
     * 聊天对话（指定 AI 厂商）
     */
    @PostMapping("/chat/{provider}")
    public Result<ChatResponse> chatWithProvider(
            @PathVariable String provider,
            @RequestBody ChatRequest request) {
        log.info("收到聊天请求，厂商: {}, 消息: {}", provider, request.getMessage());

        AiProvider aiProvider = AiProvider.fromCode(provider);
        AiService aiService = aiServiceFactory.getService(aiProvider);
        ChatResponse response = aiService.chat(request);
        return Result.success(response);
    }

    /**
     * 简单文本对话
     */
    @PostMapping("/simple-chat")
    public Result<String> simpleChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");

        AiService aiService = aiServiceFactory.getDefaultService();
        if (aiService == null) {
            return Result.error("AI 服务未配置");
        }

        String response = aiService.simpleChat(message);
        return Result.success(response);
    }

    // ==================== 流式聊天接口 ====================

    /**
     * SSE 流式聊天（使用默认 AI 服务）
     */
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(300_000L);

        AiService aiService = aiServiceFactory.getDefaultService();
        if (aiService == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("AI 服务未配置"));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        if (!aiService.supportsStreaming()) {
            // 降级：非流式服务，先完整调用再逐字发送模拟流式
            executor.execute(() -> {
                try {
                    ChatResponse response = aiService.chat(request);
                    String content = response.getContent();
                    // 模拟流式：每个字符作为一个 chunk
                    for (char c : content.toCharArray()) {
                        emitter.send(SseEmitter.event().name("message").data(String.valueOf(c)));
                        Thread.sleep(10); // 模拟打字效果
                    }
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    } catch (Exception ignored) {}
                    emitter.completeWithError(e);
                }
            });
            return emitter;
        }

        // 真正的流式调用
        executor.execute(() -> {
            try {
                aiService.streamChat(request)
                        .doOnNext(chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("message").data(chunk));
                            } catch (Exception e) {
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (Exception ignored) {}
                        })
                        .doOnError(e -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                            } catch (Exception ignored) {}
                            emitter.completeWithError(e);
                        })
                        .subscribe();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        emitter.onCompletion(() -> log.debug("SSE 流式聊天完成"));
        emitter.onTimeout(emitter::complete);
        emitter.onError(e -> log.error("SSE 流式聊天异常", e));

        return emitter;
    }

    // ==================== 诊断与配置接口 ====================

    /**
     * 获取支持的 AI 厂商列表
     */
    @GetMapping("/providers")
    public Result<Map<String, Object>> getProviders() {
        List<AiProvider> supported = aiServiceFactory.getSupportedProviders();

        Map<String, Object> result = new HashMap<>();
        result.put("default", aiServiceFactory.getDefaultService() != null
                ? aiServiceFactory.getDefaultService().getProvider().getCode()
                : null);
        result.put("supported", supported.stream().map(p -> {
            Map<String, String> info = new HashMap<>();
            info.put("code", p.getCode());
            info.put("name", p.getName());
            return info;
        }).toList());

        return Result.success(result);
    }

    /**
     * 测试 AI 连接（指定厂商）
     */
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String providerCode = request.get("provider");
        String apiKey = request.get("apiKey");

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            AiProvider provider = AiProvider.fromCode(providerCode);
            AiService aiService = aiServiceFactory.getService(provider);

            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage("你好，请回复 '连接成功'");
            chatRequest.setMaxTokens(50);

            ChatResponse response = aiService.chat(chatRequest);
            long latency = System.currentTimeMillis() - startTime;

            result.put("success", true);
            result.put("provider", providerCode);
            result.put("latency", latency);
            result.put("response", response.getContent());
            result.put("model", response.getModel());

            return Result.success(result);

        } catch (IllegalArgumentException e) {
            result.put("success", false);
            result.put("error", "不支持的厂商: " + providerCode);
            return Result.error("不支持的厂商: " + providerCode);
        } catch (Exception e) {
            result.put("success", false);
            result.put("provider", providerCode);
            result.put("error", e.getMessage());
            log.error("AI 连接测试失败: provider={}", providerCode, e);
            return Result.error("连接失败: " + e.getMessage());
        }
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        List<AiProvider> supported = aiServiceFactory.getSupportedProviders();
        health.put("status", supported.isEmpty() ? "DOWN" : "UP");
        health.put("providers", supported.stream().map(AiProvider::getCode).toList());
        health.put("defaultProvider", aiServiceFactory.getDefaultService() != null
                ? aiServiceFactory.getDefaultService().getProvider().getCode()
                : null);
        return Result.success(health);
    }
}
