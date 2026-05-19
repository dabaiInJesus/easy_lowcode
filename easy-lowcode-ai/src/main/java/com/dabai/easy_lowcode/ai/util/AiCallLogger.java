package com.dabai.easy_lowcode.ai.util;

import com.dabai.easy_lowcode.ai.enums.AiProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * AI 调用日志工具
 * 统一日志格式：耗时、provider、model、消息长度、状态
 */
@Slf4j
public final class AiCallLogger {

    private AiCallLogger() {}

    public static void logRequest(AiProvider provider, String model, String message, String systemPrompt) {
        log.info("[AI调用] provider={}, model={}, msgLen={}, hasSystemPrompt={}",
                provider.getCode(), model,
                message != null ? message.length() : 0,
                systemPrompt != null && !systemPrompt.isEmpty());
    }

    public static void logResponse(AiProvider provider, String model, long elapsedMs, int responseLen, boolean success) {
        if (success) {
            log.info("[AI响应] provider={}, model={}, elapsed={}ms, responseLen={}",
                    provider.getCode(), model, elapsedMs, responseLen);
        } else {
            log.warn("[AI响应失败] provider={}, model={}, elapsed={}ms",
                    provider.getCode(), model, elapsedMs);
        }
    }

    public static void logStreamStart(AiProvider provider, String model, String message) {
        log.info("[AI流式开始] provider={}, model={}, msgLen={}",
                provider.getCode(), model,
                message != null ? message.length() : 0);
    }

    public static void logStreamEnd(AiProvider provider, long elapsedMs, int chunksCount, boolean success) {
        if (success) {
            log.info("[AI流式结束] provider={}, elapsed={}ms, chunks={}",
                    provider.getCode(), elapsedMs, chunksCount);
        } else {
            log.warn("[AI流式中断] provider={}, elapsed={}ms",
                    provider.getCode(), elapsedMs);
        }
    }
}
