package com.dabai.easy_lowcode.ai.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.service.AiService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * 阿里云通义千问聊天服务实现
 */
@Slf4j
@Service
@ConditionalOnProperty(name = "ai.provider.default", havingValue = "dashscope")
public class DashScopeServiceImpl implements AiService {
    
    @Value("${ai.dashscope.api-key:}")
    private String apiKey;
    
    @Value("${ai.dashscope.model:qwen-turbo}")
    private String defaultModel;
    
    private static final String API_URL = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
    
    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("调用通义千问接口，消息: {}", request.getMessage());
        
        try {
            // 构建请求体
            JSONObject requestBody = new JSONObject();
            requestBody.set("model", request.getModel() != null ? request.getModel() : defaultModel);
            
            JSONObject input = new JSONObject();
            JSONArray messages = new JSONArray();
            
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
            
            input.set("messages", messages);
            requestBody.set("input", input);
            
            // 添加参数
            JSONObject parameters = new JSONObject();
            parameters.set("temperature", request.getTemperature());
            parameters.set("max_tokens", request.getMaxTokens());
            requestBody.set("parameters", parameters);
            
            // 发送请求
            HttpResponse response = HttpRequest.post(API_URL)
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .body(requestBody.toString())
                    .timeout(30000)
                    .execute();
            
            if (!response.isOk()) {
                throw new RuntimeException("通义千问 API 调用失败: " + response.body());
            }
            
            // 解析响应
            JSONObject responseBody = JSONUtil.parseObj(response.body());
            JSONObject output = responseBody.getJSONObject("output");
            String content = output.getStr("text");
            
            // 构建响应
            ChatResponse chatResponse = new ChatResponse();
            chatResponse.setContent(content);
            chatResponse.setModel(defaultModel);
            
            // 解析 usage
            if (responseBody.containsKey("usage")) {
                JSONObject usage = responseBody.getJSONObject("usage");
                ChatResponse.Usage usageInfo = new ChatResponse.Usage();
                usageInfo.setPromptTokens(usage.getInt("input_tokens", 0));
                usageInfo.setCompletionTokens(usage.getInt("output_tokens", 0));
                usageInfo.setTotalTokens(usage.getInt("total_tokens", 0));
                chatResponse.setUsage(usageInfo);
            }
            
            log.info("通义千问响应成功");
            return chatResponse;
            
        } catch (Exception e) {
            log.error("通义千问调用失败", e);
            throw new RuntimeException("AI 服务调用失败: " + e.getMessage(), e);
        }
    }
}
