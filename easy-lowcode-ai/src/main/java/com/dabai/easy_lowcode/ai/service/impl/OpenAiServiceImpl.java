package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.ai.util.AiCallLogger;
import com.dabai.easy_lowcode.ai.util.ChatResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;

/**
 * OpenAI 聊天服务实现
 * 使用 Spring AI OpenAI ChatModel
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.openai.enabled", havingValue = "true", matchIfMissing = false)
public class OpenAiServiceImpl implements AiService {

    private final ChatModel chatModel;
    private final String defaultModel;

    public OpenAiServiceImpl(
            @Qualifier("openAiChatModel") ChatModel chatModel,
            @Value("${spring.ai.openai.chat.options.model:gpt-3.5-turbo}") String defaultModel) {
        this.chatModel = chatModel;
        this.defaultModel = defaultModel;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        long start = System.currentTimeMillis();
        AiCallLogger.logRequest(AiProvider.OPENAI, defaultModel, request.getMessage(), request.getSystemPrompt());

        try {
            Prompt prompt = buildPrompt(request);
            org.springframework.ai.chat.model.ChatResponse response = chatModel.call(prompt);
            ChatResponse chatResponse = ChatResponseUtil.toDto(response, defaultModel);

            long elapsed = System.currentTimeMillis() - start;
            AiCallLogger.logResponse(AiProvider.OPENAI, defaultModel, elapsed,
                    chatResponse.getContent() != null ? chatResponse.getContent().length() : 0, true);
            return chatResponse;

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            AiCallLogger.logResponse(AiProvider.OPENAI, defaultModel, elapsed, 0, false);
            log.error("OpenAI 调用失败", e);
            throw new RuntimeException("OpenAI 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        long start = System.currentTimeMillis();
        AiCallLogger.logStreamStart(AiProvider.OPENAI, defaultModel, request.getMessage());
        final long[] chunkCount = {0};

        try {
            Prompt prompt = buildPrompt(request);
            return chatModel.stream(prompt)
                    .map(chunk -> {
                        chunkCount[0]++;
                        return ChatResponseUtil.extractText(chunk);
                    })
                    .filter(content -> content != null && !content.isEmpty())
                    .doOnComplete(() -> AiCallLogger.logStreamEnd(AiProvider.OPENAI,
                            System.currentTimeMillis() - start, (int) chunkCount[0], true))
                    .doOnError(e -> AiCallLogger.logStreamEnd(AiProvider.OPENAI,
                            System.currentTimeMillis() - start, (int) chunkCount[0], false));

        } catch (Exception e) {
            AiCallLogger.logStreamEnd(AiProvider.OPENAI, System.currentTimeMillis() - start, 0, false);
            log.error("OpenAI 流式调用失败", e);
            return Flux.error(new RuntimeException("OpenAI 流式服务调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.OPENAI;
    }

    @Override
    public boolean supportsStreaming() {
        return true;
    }

    private Prompt buildPrompt(ChatRequest request) {
        var messages = new ArrayList<Message>();
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
            messages.add(new SystemMessage(request.getSystemPrompt()));
        }
        messages.add(new UserMessage(request.getMessage()));
        return new Prompt(messages);
    }
}
