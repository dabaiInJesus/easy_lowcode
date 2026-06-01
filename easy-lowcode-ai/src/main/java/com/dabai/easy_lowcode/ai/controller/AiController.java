package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * AI 聊天控制器
 */
@Tag(name = "AI聊天", description = "AI对话、流式聊天、连接测试")
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Validated
public class AiController {

    private final AiServiceFactory aiServiceFactory;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    @Operation(summary = "聊天对话（默认AI）", description = "使用默认AI服务进行对话")
    @ApiResponse(responseCode = "200", description = "对话成功")
    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody @Validated ChatRequest request) {
        AiService aiService = aiServiceFactory.getDefaultService();
        if (aiService == null) {
            return Result.error("AI 服务未配置，请先在配置文件中启用至少一个 AI 厂商");
        }

        ChatResponse response = aiService.chat(request);
        return Result.success(response);
    }

    @Operation(summary = "聊天对话（指定AI厂商）", description = "使用指定的AI厂商进行对话")
    @ApiResponse(responseCode = "200", description = "对话成功")
    @PostMapping("/chat/{provider}")
    public Result<ChatResponse> chatWithProvider(
            @Parameter(description = "AI厂商（openai/dashscope/ollama/deepseek/minimax）") @PathVariable String provider,
            @RequestBody @Validated ChatRequest request) {
        AiProvider aiProvider = AiProvider.fromCode(provider);
        AiService aiService = aiServiceFactory.getService(aiProvider);
        ChatResponse response = aiService.chat(request);
        return Result.success(response);
    }

    @Operation(summary = "简单文本对话", description = "使用默认AI服务进行简单文本对话")
    @ApiResponse(responseCode = "200", description = "对话成功")
    @PostMapping("/simple-chat")
    public Result<String> simpleChat(@RequestBody @Validated Map<String, @NotBlank String> request) {
        String message = request.get("message");

        AiService aiService = aiServiceFactory.getDefaultService();
        if (aiService == null) {
            return Result.error("AI 服务未配置");
        }

        String response = aiService.simpleChat(message);
        return Result.success(response);
    }

    @Operation(summary = "SSE流式聊天", description = "使用SSE进行流式对话，实时返回AI响应")
    @ApiResponse(responseCode = "200", description = "流式对话开始")
    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(@RequestBody @Validated ChatRequest request) {
        long startTime = System.currentTimeMillis();
        SseEmitter emitter = new SseEmitter(300_000L);

        AiService aiService = aiServiceFactory.getDefaultService();
        if (aiService == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("AI 服务未配置"));
            } catch (Exception e) {
                log.warn("SSE发送失败: {}", e.getMessage());
            }
            emitter.complete();
            return emitter;
        }

        emitter.onTimeout(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            log.warn("SSE 流式聊天超时，历时: {}ms");
        });

        emitter.onCompletion(() -> {
            long elapsed = System.currentTimeMillis() - startTime;
            log.info("SSE 流式聊天完成，历时: {}ms", elapsed);
        });

        emitter.onError(e -> {
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("SSE 流式聊天异常，历时: {}ms, 错误: {}", elapsed, e.getMessage());
        });

        if (!aiService.supportsStreaming()) {
            executor.execute(() -> {
                try {
                    ChatResponse response = aiService.chat(request);
                    String content = response.getContent();
                    for (char c : content.toCharArray()) {
                        emitter.send(SseEmitter.event().name("message").data(String.valueOf(c)));
                        Thread.sleep(10);
                    }
                    emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                    emitter.complete();
                } catch (Exception e) {
                    try {
                        emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    } catch (Exception ex) {
                        log.warn("SSE发送错误消息失败: {}", ex.getMessage());
                    }
                    emitter.completeWithError(e);
                }
            });
            return emitter;
        }

        executor.execute(() -> {
            try {
                aiService.streamChat(request)
                        .doOnNext(chunk -> {
                            try {
                                emitter.send(SseEmitter.event().name("message").data(chunk));
                            } catch (Exception e) {
                                log.warn("SSE发送chunk失败: {}", e.getMessage());
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                                emitter.complete();
                            } catch (Exception e) {
                                log.warn("SSE发送done失败: {}", e.getMessage());
                            }
                        })
                        .doOnError(e -> {
                            try {
                                emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                            } catch (Exception ex) {
                                log.warn("SSE发送error失败: {}", ex.getMessage());
                            }
                            emitter.completeWithError(e);
                        })
                        .subscribe();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                } catch (Exception ex) {
                    log.warn("SSE发送外层错误失败: {}", ex.getMessage());
                }
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    @Operation(summary = "获取支持的AI厂商列表", description = "获取所有已配置和可用的AI厂商信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
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

    @Operation(summary = "测试AI连接", description = "测试指定AI厂商的连接是否正常")
    @ApiResponse(responseCode = "200", description = "测试完成")
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection(@RequestBody Map<String, String> request) {
        String providerCode = request.get("provider");
        String apiKey = request.get("apiKey");
        String apiUrl = request.get("apiUrl");
        String model = request.get("model");

        log.info("AI测试连接请求 - provider: {}, apiUrl: {}, model: {}", providerCode, apiUrl, model);

        Map<String, Object> result = new HashMap<>();
        long startTime = System.currentTimeMillis();

        try {
            AiProvider provider = AiProvider.fromCode(providerCode);
            log.info("成功解析 provider: {}", provider);

            org.springframework.ai.chat.model.ChatModel chatModel;
            if (apiUrl != null && !apiUrl.isEmpty()) {
                log.info("使用自定义 apiUrl 创建 ChatModel");
                chatModel = createTestChatModel(provider, apiUrl, apiKey, model);
            } else {
                log.info("使用默认配置获取 AiService");
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
            }

            log.info("ChatModel 创建成功，开始测试连接");
            ChatRequest chatRequest = new ChatRequest();
            chatRequest.setMessage("你好，请回复 '连接成功'");
            chatRequest.setMaxTokens(50);

            var messages = new ArrayList<Message>();
            messages.add(new UserMessage(chatRequest.getMessage()));
            var prompt = new Prompt(messages);

            log.info("调用 AI API...");
            var chatResponse = chatModel.call(prompt);
            String content = chatResponse.getResult().getOutput().getText();
            long latency = System.currentTimeMillis() - startTime;

            log.info("AI 测试成功，耗时: {}ms", latency);
            result.put("success", true);
            result.put("provider", providerCode);
            result.put("latency", latency);
            result.put("response", content);
            result.put("model", model);
            return Result.success(result);

        } catch (IllegalArgumentException e) {
            log.error("不支持的 AI 厂商: {}", providerCode, e);
            result.put("success", false);
            result.put("error", "不支持的厂商: " + providerCode);
            return Result.error("不支持的厂商: " + providerCode);
        } catch (Exception e) {
            log.error("AI测试连接失败 - 异常类型: {}, 完整消息: {}", e.getClass().getName(), e.getMessage(), e);
            result.put("success", false);
            result.put("provider", providerCode);
            String errMsg = e.getMessage() == null ? e.toString() : e.getMessage();
            result.put("error", errMsg);

            Integer httpStatus = extractHttpStatus(e);
            log.info("提取到 HTTP 状态码: {}", httpStatus);

            String friendlyMsg;
            if (httpStatus != null) {
                switch (httpStatus) {
                    case 401, 403 -> friendlyMsg = "AI 服务鉴权失败 (" + httpStatus + ")。请检查 API Key 是否正确";
                    case 404 -> friendlyMsg = "AI 服务地址不存在 (404)。请检查 API 地址是否正确，当前地址: " + apiUrl;
                    case 429 -> friendlyMsg = "AI 服务请求频率超限 (429)，请稍后重试";
                    case 500, 502, 503, 504 -> friendlyMsg = "AI 服务异常 (" + httpStatus + ")，请稍后重试";
                    default -> friendlyMsg = "AI 服务返回错误 (" + httpStatus + "): " + errMsg;
                }
            } else if (errMsg.toLowerCase().contains("timeout") || errMsg.toLowerCase().contains("timed out")) {
                friendlyMsg = "AI 服务连接超时，请检查网络或 API 地址";
            } else if (errMsg.toLowerCase().contains("connection refused") || errMsg.toLowerCase().contains("unknownhost")) {
                friendlyMsg = "无法连接到 AI 服务，请检查 API 地址: " + apiUrl;
            } else {
                friendlyMsg = "AI 连接失败: " + errMsg;
            }
            return Result.error(friendlyMsg);
        }
    }

    private Integer extractHttpStatus(Throwable e) {
        Throwable cur = e;
        while (cur != null) {
            String cls = cur.getClass().getName();
            String msg = cur.getMessage() == null ? "" : cur.getMessage();
            if (cls.contains("Unauthorized") || msg.startsWith("401") || msg.contains(" 401 ")) return 401;
            if (cls.contains("Forbidden") || msg.startsWith("403") || msg.contains(" 403 ")) return 403;
            if (cls.contains("NotFound") || msg.startsWith("404") || msg.contains(" 404 ")) return 404;
            if (cls.contains("TooManyRequests") || msg.startsWith("429") || msg.contains(" 429 ")) return 429;
            if (cls.contains("InternalServerError") || msg.startsWith("500")) return 500;
            if (cls.contains("BadGateway") || msg.startsWith("502")) return 502;
            if (cls.contains("ServiceUnavailable") || msg.startsWith("503")) return 503;
            if (cls.contains("GatewayTimeout") || msg.startsWith("504")) return 504;
            if (cls.contains("NonTransientAiException") || cls.contains("AiException")) {
                java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)\\s*-").matcher(msg);
                if (m.find()) {
                    return Integer.parseInt(m.group(1));
                }
            }
            cur = cur.getCause();
        }
        return null;
    }

    private org.springframework.ai.chat.model.ChatModel createTestChatModel(
            AiProvider provider, String apiUrl, String apiKey, String model) {
        String baseUrl = apiUrl != null ? apiUrl.trim() : "";
        baseUrl = baseUrl.replaceAll("/+$", "");
        
        // 避免重复添加 /v1 后缀
        String normalizedUrl = baseUrl;
        if (normalizedUrl.endsWith("/v1") || normalizedUrl.endsWith("/v1/chat")) {
            // baseUrl 已经包含 /v1，不需要再添加
        } else if (!normalizedUrl.endsWith("/chat/completions")) {
            // 如果不是以 /chat/completions 结尾，添加 /v1
            normalizedUrl = normalizedUrl + "/v1";
        }
        
        String modelName = model != null && !model.isEmpty() ? model.trim() : "default";
        log.info("Creating chat model for provider: {}, baseUrl: {}, model: {}", provider, normalizedUrl, modelName);

        return switch (provider) {
            case OPENAI, DASHSCOPE, DEEPSEEK, MINIMAX, WENXIN, HUNYUAN, ZHIPU, MOONSHOT -> {
                OpenAiApi openAiApi = OpenAiApi.builder()
                        .baseUrl(normalizedUrl)
                        .apiKey(apiKey != null ? apiKey : "")
                        .build();
                yield OpenAiChatModel.builder()
                        .openAiApi(openAiApi)
                        .defaultOptions(OpenAiChatOptions.builder().model(modelName).build())
                        .build();
            }
            case OLLAMA -> {
                String ollamaUrl = apiUrl != null ? apiUrl.trim().replaceAll("/+$", "") : "http://localhost:11434";
                log.info("Creating Ollama chat model with url: {}", ollamaUrl);
                OllamaApi ollamaApi = OllamaApi.builder()
                        .baseUrl(ollamaUrl)
                        .build();
                yield OllamaChatModel.builder()
                        .ollamaApi(ollamaApi)
                        .defaultOptions(OllamaChatOptions.builder().model(modelName).build())
                        .build();
            }
        };
    }

    @Operation(summary = "AI服务健康检查", description = "检查AI服务的运行状态")
    @ApiResponse(responseCode = "200", description = "检查完成")
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
