package com.dabai.easy_lowcode.ai.agent;

import com.alibaba.fastjson.JSON;
import com.dabai.easy_lowcode.ai.agent.tool.AgentTool;
import com.dabai.easy_lowcode.ai.agent.tool.ToolRegistry;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiService;
import com.dabai.easy_lowcode.ai.service.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;

/**
 * Agent 执行引擎（ReAct 循环）
 * <p>
 * 实现 Thought → Action → Observation 循环：
 * 1. LLM 接收用户输入 + 系统提示 + 工具描述
 * 2. LLM 输出思考过程和行动决策
 * 3. 如果是工具调用 → 执行工具 → 将结果反馈
 * 4. 如果是最终回答 → 返回给用户
 * 5. 重复直到完成或达到最大轮次
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentExecutor {

    private final ToolRegistry toolRegistry;
    private final AiServiceFactory aiServiceFactory;
    private final SessionManager sessionManager;

    /**
     * 执行 Agent 对话（SSE 流式）
     */
    public void executeAgent(AiAgent agent, String userMessage, String sessionId, SseEmitter emitter) {
        try {
            // 1. 构建系统提示词
            String systemPrompt = buildSystemPrompt(agent);

            // 2. 获取对话历史
            List<Map<String, String>> history = sessionManager.getHistory(
                    agent.getAgentCode(), sessionId);

            // 3. 添加用户消息到历史
            history.add(Map.of("role", "user", "content", userMessage));
            sessionManager.addMessage(agent.getAgentCode(), sessionId, "user", userMessage);

            // 4. ReAct 循环
            int maxIterations = agent.getMaxIterations() != null ? agent.getMaxIterations() : 10;
            for (int i = 0; i < maxIterations; i++) {
                log.info("Agent 执行轮次: {}/{}", i + 1, maxIterations);

                // 构建工具描述
                List<Map<String, Object>> toolDescriptions = getAgentToolDescriptions(agent);

                // 调用 LLM
                ChatRequest request = buildChatRequest(systemPrompt, history, toolDescriptions);
                AiService aiService = getAgentService(agent);
                ChatResponse response = aiService.chat(request);

                String content = response.getContent();
                if (content == null || content.isBlank()) {
                    content = "（无响应）";
                }

                // 5. 解析 LLM 输出，检查是否有工具调用
                ToolCall toolCall = parseToolCall(content);

                if (toolCall != null) {
                    // 推送思考过程
                    sendEvent(emitter, "thought", Map.of(
                            "content", content,
                            "iteration", i + 1
                    ));

                    // 执行工具
                    sendEvent(emitter, "tool_start", Map.of(
                            "toolName", toolCall.toolName,
                            "params", toolCall.params
                    ));

                    Object toolResult = executeTool(agent, toolCall.toolName, toolCall.params);

                    sendEvent(emitter, "tool_result", Map.of(
                            "toolName", toolCall.toolName,
                            "result", toolResult
                    ));

                    // 将工具结果添加到历史
                    String toolResultStr = JSON.toJSONString(toolResult);
                    history.add(Map.of("role", "assistant", "content", content));
                    history.add(Map.of("role", "user", "content",
                            "工具 " + toolCall.toolName + " 的执行结果:\n" + toolResultStr));

                    sessionManager.addMessage(agent.getAgentCode(), sessionId, "assistant", content);
                    sessionManager.addMessage(agent.getAgentCode(), sessionId, "user",
                            "工具 " + toolCall.toolName + " 的执行结果:\n" + toolResultStr);

                } else {
                    // 6. 无工具调用，返回最终回答
                    sendEvent(emitter, "response", Map.of(
                            "content", content,
                            "iteration", i + 1
                    ));

                    sessionManager.addMessage(agent.getAgentCode(), sessionId, "assistant", content);

                    // 推送完成事件
                    sendEvent(emitter, "done", Map.of(
                            "sessionId", sessionId
                    ));
                    emitter.complete();
                    return;
                }
            }

            // 达到最大轮次
            sendEvent(emitter, "response", Map.of(
                    "content", "已达到最大执行轮次，请重新提问。",
                    "iteration", maxIterations
            ));
            sendEvent(emitter, "done", Map.of("sessionId", sessionId));
            emitter.complete();

        } catch (Exception e) {
            log.error("Agent 执行异常: agentCode={}", agent.getAgentCode(), e);
            sendEvent(emitter, "error", Map.of("error", e.getMessage()));
            emitter.complete();
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt(AiAgent agent) {
        StringBuilder prompt = new StringBuilder();

        // 基础指令
        if (agent.getInstructions() != null && !agent.getInstructions().isBlank()) {
            prompt.append(agent.getInstructions()).append("\n\n");
        }

        // 开场白
        if (agent.getOpeningStatement() != null && !agent.getOpeningStatement().isBlank()) {
            prompt.append("开场白: ").append(agent.getOpeningStatement()).append("\n\n");
        }

        // 工具使用说明
        List<Map<String, Object>> tools = getAgentToolDescriptions(agent);
        if (!tools.isEmpty()) {
            prompt.append("你可以使用以下工具来帮助回答问题：\n");
            for (Map<String, Object> tool : tools) {
                prompt.append("- ").append(tool.get("name")).append(": ").append(tool.get("description")).append("\n");
            }
            prompt.append("\n当你需要使用工具时，请按以下格式输出：\n");
            prompt.append("```tool\n{\"name\": \"工具名\", \"params\": {参数}}\n```\n");
            prompt.append("当不需要使用工具时，直接回答用户的问题。\n\n");
        }

        return prompt.toString();
    }

    /**
     * 获取 Agent 启用的工具描述
     */
    private List<Map<String, Object>> getAgentToolDescriptions(AiAgent agent) {
        List<Map<String, Object>> descriptions = new ArrayList<>();

        if (agent.getToolsConfig() != null && !agent.getToolsConfig().isBlank()) {
            try {
                @SuppressWarnings("unchecked")
                List<String> toolNames = JSON.parseArray(agent.getToolsConfig(), String.class);
                for (String toolName : toolNames) {
                    AgentTool tool = toolRegistry.getTool(toolName);
                    if (tool != null) {
                        descriptions.add(Map.of(
                                "name", tool.getName(),
                                "description", tool.getDescription(),
                                "parameters", tool.getParametersSchema()
                        ));
                    }
                }
            } catch (Exception e) {
                log.warn("解析工具配置失败: {}", e.getMessage());
            }
        }

        return descriptions;
    }

    /**
     * 构建 ChatRequest
     */
    private ChatRequest buildChatRequest(String systemPrompt, List<Map<String, String>> history,
                                          List<Map<String, Object>> tools) {
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt(systemPrompt);

        // 构建消息列表
        StringBuilder messageBuilder = new StringBuilder();
        for (Map<String, String> msg : history) {
            String role = msg.get("role");
            String content = msg.get("content");
            messageBuilder.append(role).append(": ").append(content).append("\n");
        }
        request.setMessage(messageBuilder.toString());

        return request;
    }

    /**
     * 解析 LLM 输出中的工具调用
     */
    private ToolCall parseToolCall(String content) {
        if (content == null) return null;

        // 查找 ```tool ... ``` 块
        int start = content.indexOf("```tool");
        int end = content.indexOf("```", start + 7);
        if (start == -1 || end == -1) return null;

        String toolBlock = content.substring(start + 7, end).trim();

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> toolCallJson = JSON.parseObject(toolBlock, Map.class);
            String toolName = (String) toolCallJson.get("name");
            @SuppressWarnings("unchecked")
            Map<String, Object> params = (Map<String, Object>) toolCallJson.getOrDefault("params", Map.of());

            if (toolName != null && !toolName.isBlank()) {
                return new ToolCall(toolName, params);
            }
        } catch (Exception e) {
            log.debug("解析工具调用失败: {}", e.getMessage());
        }

        return null;
    }

    /**
     * 执行工具
     */
    private Object executeTool(AiAgent agent, String toolName, Map<String, Object> params) {
        AgentTool tool = toolRegistry.getTool(toolName);
        if (tool == null) {
            return Map.of("error", "未找到工具: " + toolName);
        }

        try {
            return tool.execute(params);
        } catch (Exception e) {
            log.error("工具执行失败: tool={}", toolName, e);
            return Map.of("error", "工具执行失败: " + e.getMessage());
        }
    }

    /**
     * 获取 Agent 对应的 AiService
     */
    private AiService getAgentService(AiAgent agent) {
        String providerCode = agent.getProvider();
        if (providerCode != null && !providerCode.isBlank()) {
            try {
                var provider = com.dabai.easy_lowcode.ai.enums.AiProvider.fromCode(providerCode);
                return aiServiceFactory.getService(provider);
            } catch (Exception e) {
                log.warn("获取指定Provider失败，使用默认: {}", e.getMessage());
            }
        }
        return aiServiceFactory.getDefaultService();
    }

    /**
     * 推送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", eventName);
            event.put("data", data);
            event.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event().name(eventName).data(JSON.toJSONString(event)));
        } catch (IOException e) {
            log.warn("SSE推送失败: event={}", eventName, e);
        }
    }

    /**
     * 工具调用记录
     */
    private record ToolCall(String toolName, Map<String, Object> params) {}
}
