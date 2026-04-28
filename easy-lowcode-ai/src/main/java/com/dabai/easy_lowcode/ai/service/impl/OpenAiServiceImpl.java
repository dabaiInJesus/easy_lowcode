package com.dabai.easy_lowcode.ai.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * OpenAI 聊天服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "ai.provider.default", havingValue = "openai", matchIfMissing = true)
public class OpenAiServiceImpl implements AiService {
    
    private final ChatModel chatModel;
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("调用 OpenAI 接口，消息: {}", request.getMessage());
        
        try {
            // 构建消息
            var messages = new java.util.ArrayList<org.springframework.ai.chat.messages.Message>();
            
            // 添加系统提示词
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
                messages.add(new SystemMessage(request.getSystemPrompt()));
            }
            
            // 添加用户消息
            messages.add(new UserMessage(request.getMessage()));
            
            // 构建 Prompt
            Prompt prompt = new Prompt(messages);
            
            // 调用 OpenAI
            var response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getText();
            
            // 构建响应
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setContent(content);
            chatResponse.setModel("gpt-3.5-turbo");
            
            log.info("OpenAI 响应成功");
            return chatResponse;
            
        } catch (Exception e) {
            log.error("OpenAI 调用失败", e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }
}
