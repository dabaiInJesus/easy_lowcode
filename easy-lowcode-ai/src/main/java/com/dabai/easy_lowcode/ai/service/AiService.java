package com.dabai.easy_lowcode.ai.service;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import reactor.core.publisher.Flux;

/**
 * AI 服务统一接口
 */
public interface AiService {

    /**
     * 聊天对话
     *
     * @param request 聊天请求
     * @return 聊天响应
     */
    ChatResponse chat(ChatRequest request);

    /**
     * 流式聊天对话
     *
     * @param request 聊天请求
     * @return 流式响应（每个 chunk 为一个字符串片段）
     */
    default Flux<String> streamChat(ChatRequest request) {
        // 默认实现：完整调用后返回单个 Flux
        String content = chat(request).getContent();
        return Flux.just(content);
    }

    /**
     * 简单文本对话
     *
     * @param message 用户消息
     * @return AI 回复
     */
    default String simpleChat(String message) {
        ChatRequest request = new ChatRequest();
        request.setMessage(message);
        ChatResponse response = chat(request);
        return response.getContent();
    }

    /**
     * 获取该服务对应的 AI 厂商
     *
     * @return AiProvider
     */
    AiProvider getProvider();

    /**
     * 是否支持流式输出
     *
     * @return true if streaming is supported
     */
    default boolean supportsStreaming() {
        return false;
    }
}
