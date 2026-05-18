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
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.ArrayList;

/**
 * Minimax 聊天服务实现
 * 使用 OpenAI 兼容 API
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.minimax.enabled", havingValue = "true")
public class MinimaxServiceImpl implements AiService {

    private final ChatModel minimaxChatModel;
    private final String defaultModel;

    public MinimaxServiceImpl(
            @Qualifier("minimaxChatModel") ChatModel minimaxChatModel,
            @Value("${ai.minimax.model:abab6-chat}") String defaultModel) {
        this.minimaxChatModel = minimaxChatModel;
        this.defaultModel = defaultModel;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("调用 Minimax 接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            var response = minimaxChatModel.call(prompt);
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
                log.warn("提取 Minimax 响应内容失败", e);
                chatResponse.setContent("");
            }

            log.info("Minimax 响应成功");
            return chatResponse;

        } catch (Exception e) {
            log.error("Minimax 调用失败", e);
            throw new RuntimeException("Minimax 服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        log.info("调用 Minimax 流式接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            return minimaxChatModel.stream(prompt)
                    .map(chunk -> {
                        try {
                            return chunk.getChoices().get(0).getDelta().getContent();
                        } catch (Exception e) {
                            return "";
                        }
                    })
                    .filter(content -> content != null && !content.isEmpty());
        } catch (Exception e) {
            log.error("Minimax 流式调用失败", e);
            return Flux.error(new RuntimeException("Minimax 流式服务调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.MINIMAX;
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
