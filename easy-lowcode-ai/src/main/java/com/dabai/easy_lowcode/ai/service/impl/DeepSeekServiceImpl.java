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
 * DeepSeek 聊天服务实现
 * 使用 OpenAI 兼容 API（基于 Spring AI OpenAiChatModel）
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.deepseek.enabled", havingValue = "true")
public class DeepSeekServiceImpl implements AiService {

    private final ChatModel deepSeekChatModel;
    private final String defaultModel;

    public DeepSeekServiceImpl(
            @Qualifier("deepSeekChatModel") ChatModel deepSeekChatModel,
            @Value("${ai.deepseek.model:deepseek-chat}") String defaultModel) {
        this.deepSeekChatModel = deepSeekChatModel;
        this.defaultModel = defaultModel;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("调用 DeepSeek 接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            var response = deepSeekChatModel.call(prompt);
            return toChatResponse(response, defaultModel);

        } catch (Exception e) {
            log.error("DeepSeek 调用失败", e);
            throw new RuntimeException("DeepSeek 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("调用 DeepSeek 流式接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            return deepSeekChatModel.stream(prompt)
                    .map(chunk -> {
                        try {
                            return chunk.getChoices().get(0).getDelta().getContent();
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .filter(content -> content != null && !content.isEmpty());

        } catch (Exception e) {
            log.error("DeepSeek 流式调用失败", e);
            return Flux.error(new RuntimeException("DeepSeek 流式服务调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.DEEPSEEK;
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

    private ChatResponse toChatResponse(Object response, String model) {
        ChatResponse chatResponse = new ChatResponse();
        chatResponse.setModel(model);
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
            log.warn("解析 DeepSeek 响应内容失败", e);
            chatResponse.setContent("");
        }
        return chatResponse;
    }
}
