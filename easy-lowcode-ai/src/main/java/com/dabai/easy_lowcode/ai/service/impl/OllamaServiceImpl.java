package com.dabai.easy_lowcode.ai.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Ollama 聊天服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider.default", havingValue = "ollama")
public class OllamaServiceImpl implements AiService {
    
    @Value("${ai.ollama.base-url:http://localhost:11434}")
    private String baseUrl;
    
    @Value("${ai.ollama.model:llama2}")
    private String defaultModel;
    
    private static final String API_URL_SUFFIX = "/api/chat";
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("调用 Ollama 接口，消息: {}", request.getMessage());
        
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", request.getModel() != null ? request.getModel() : defaultModel);
            
            // 添加消息
            cn.hutool.json.JSONArray messages = new cn.hutool.json.JSONArray();
            
            // 添加系统提示词
            if (request.getSystemPrompt() != null && !request.getSystemPrompt().isEmpty()) {
                JSONObject systemMsg = new JSONObject();
                systemMsg.set("role", "system");
                systemMsg.set("content", request.getSystemPrompt());
                messages.add(systemMsg);
            }
            
            // 添加用户消息
            JSONObject userMsg = new JSONObject();
            userMsg.set("role", "user");
            userMsg.set("content", request.getMessage());
            messages.add(userMsg);
            
            requestBody.set("messages", messages);
            
            // 添加参数
            requestBody.set("stream", false);
            if (request.getTemperature() != null) {
                requestBody.set("temperature", request.getTemperature());
            }
            
            // 发送请求
            String apiUrl = baseUrl + API_URL_SUFFIX;
            HttpResponse response = HttpRequest.post(apiUrl)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(60000)
                    .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("Ollama API 调用失败: " + response.body());
            }
            
            // 解析响应
            JSONObject responseBody = JSONUtil.parseObj(response.body());
            String content = responseBody.getJSONObject("message").getStr("content");
            
            // 构建响应
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setContent(content);
            chatResponse.setModel(defaultModel);
            
            log.info("Ollama 响应成功");
            return chatResponse;
            
        } catch (Exception e) {
            log.error("Ollama 调用失败", e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }
}
