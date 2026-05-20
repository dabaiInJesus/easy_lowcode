package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * AI Agent 控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
public class AiAgentController {

    private final AiAgentService aiAgentService;

    // ==================== 核心接口 ====================

    /**
     * 执行 Agent 任务
     */
    @PostMapping("/execute")
    public Result<String> executeAgent(@RequestBody ExecuteAgentRequest request) {
        log.info("执行 Agent: code={}, task={}", request.getAgentCode(), request.getTask());

        try {
            String result = aiAgentService.executeAgent(request.getAgentCode(), request.getTask());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("Agent 执行失败", e);
            return Result.error("Agent 执行失败: " + e.getMessage());
        }
    }

    /**
     * 流式执行 Agent 任务（SSE）
     */
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeAgentStream(@RequestBody ExecuteAgentRequest request) {
        log.info("流式执行 Agent: code={}", request.getAgentCode());
        return aiAgentService.executeAgentStream(request.getAgentCode(), request.getTask())
                .doOnError(e -> log.error("Agent 流式执行异常", e));
    }

    // ==================== Agent 管理接口 ====================

    /**
     * 获取所有可用的 Agent
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listAgents() {
        try {
            List<Map<String, Object>> agents = aiAgentService.listAgents();
            return Result.success(agents);
        } catch (Exception e) {
            log.error("获取 Agent 列表失败", e);
            return Result.error("获取 Agent 列表失败: " + e.getMessage());
        }
    }

    /**
     * 创建自定义 Agent
     */
    @PostMapping("/create")
    public Result<String> createAgent(@RequestBody CreateAgentRequest request) {
        log.info("创建 Agent: {}", request.getName());

        try {
            String code = aiAgentService.createAgent(
                    request.getName(),
                    request.getDescription(),
                    request.getInstructions()
            );
            return Result.success(code);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            log.error("创建 Agent 失败", e);
            return Result.error("创建 Agent 失败: " + e.getMessage());
        }
    }

    // ==================== 会话管理接口 ====================

    /**
     * 获取 Agent 对话历史
     */
    @GetMapping("/history/{agentCode}")
    public Result<List<Map<String, String>>> getChatHistory(
            @PathVariable String agentCode,
            @RequestParam(defaultValue = "default") String sessionId) {
        try {
            List<Map<String, String>> history = aiAgentService.getChatHistory(agentCode, sessionId);
            return Result.success(history);
        } catch (Exception e) {
            log.error("获取聊天历史失败", e);
            return Result.error("获取聊天历史失败: " + e.getMessage());
        }
    }

    /**
     * 清除指定会话
     */
    @DeleteMapping("/session/{agentCode}")
    public Result<Void> clearSession(
            @PathVariable String agentCode,
            @RequestParam(defaultValue = "default") String sessionId) {
        try {
            aiAgentService.clearSession(agentCode, sessionId);
            return Result.success("会话已清除");
        } catch (Exception e) {
            log.error("清除会话失败", e);
            return Result.error("清除会话失败: " + e.getMessage());
        }
    }

    // ==================== 请求对象 ====================

    @lombok.Data
    public static class ExecuteAgentRequest {
        private String agentCode;
        private String task;
        private String sessionId;
    }

    @lombok.Data
    public static class CreateAgentRequest {
        private String name;
        private String description;
        private String instructions;
    }
}
