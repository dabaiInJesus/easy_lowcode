package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
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
        log.info("调用 OpenAI 接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            var response = chatModel.call(prompt);

            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setModel(defaultModel);

            // 提取内容
            try {
                var result = response.getClass().getMethod("getResult").invoke(response);
                if (result != null) {
                    var output = result.getClass().getMethod("getOutput").invoke(result);
                    if (output != null) {
                        var getText = output.getClass().getMethod("getTextContent");
                        chatResponse.setContent((String) getText.invoke(output));
                    }
                }
            } catch (Exception e) {
                log.warn("提取 OpenAI 响应内容失败", e);
                chatResponse.setContent("");
            }

            log.info("OpenAI 响应成功");
            return chatResponse;

        } catch (Exception e) {
            log.error("OpenAI 调用失败", e);
            throw new RuntimeException("OpenAI 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("调用 OpenAI 流式接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            return chatModel.stream(prompt)
                    .map(chunk -> {
                        try {
                            return chunk.getChoices().get(0).getDelta().getContent();
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .filter(content -> content != null && !content.isEmpty());
        } catch (Exception e) {
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
