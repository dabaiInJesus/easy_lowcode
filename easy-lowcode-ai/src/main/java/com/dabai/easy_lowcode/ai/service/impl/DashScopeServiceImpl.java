package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.ai.util.ChatResponseUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;

/**
 * 阿里云通义千问聊天服务实现
 * 使用 Spring AI Alibaba DashScopeChatModel
 */
@Slf4j
@Service
public class DashScopeServiceImpl implements AiService {

    private final ChatModel dashScopeChatModel;

    /**
     * 构造器注入 DashScopeChatModel（由 Spring AI Alibaba Starter 自动配置）
     * 如果 DashScope 未配置，则 dashScopeChatModel 为 null，
     * getProvider() 会抛出异常告知用户配置问题
     */
    @Autowired(required = false)
    public DashScopeServiceImpl(ChatModel dashScopeChatModel) {
        this.dashScopeChatModel = dashScopeChatModel;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        ensureAvailable();
        log.info("调用通义千问接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            org.springframework.ai.chat.model.ChatResponse response = dashScopeChatModel.call(prompt);

            ChatResponse chatResponse = ChatResponseUtil.toDto(response, "dashscope");
            log.info("通义千问响应成功");
            return chatResponse;

        } catch (Exception e) {
            log.error("通义千问调用失败", e);
            throw new RuntimeException("通义千问服务调用失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> streamChat(ChatRequest request) {
        ensureAvailable();
        log.info("调用通义千问流式接口，消息: {}", request.getMessage());

        try {
            Prompt prompt = buildPrompt(request);
            return dashScopeChatModel.stream(prompt)
                    .map(chunk -> ChatResponseUtil.extractText(chunk))
                    .filter(content -> content != null && !content.isEmpty());

        } catch (Exception e) {
            log.error("通义千问流式调用失败", e);
            return Flux.error(new RuntimeException("通义千问流式服务调用失败: " + e.getMessage(), e));
        }
    }

    @Override
    public AiProvider getProvider() {
        return AiProvider.DASHSCOPE;
    }

    @Override
    public boolean supportsStreaming() {
        return dashScopeChatModel != null;
    }

    private void ensureAvailable() {
        if (dashScopeChatModel == null) {
            throw new IllegalStateException(
                    "通义千问未配置。请在 application.yml 中配置 spring.ai.dashscope.api-key");
        }
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
