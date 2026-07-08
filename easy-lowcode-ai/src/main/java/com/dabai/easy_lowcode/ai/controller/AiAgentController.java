package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "AI Agent", description = "AI Agent任务执行、创建、会话管理")
@Slf4j
@RestController
@RequestMapping("/api/ai/agent")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiAgentController {

    private final AiAgentService aiAgentService;

    @Operation(summary = "执行Agent任务", description = "执行指定的AI Agent任务")
    @ApiResponse(responseCode = "200", description = "执行成功")
    @PostMapping("/execute")
    public Result<String> executeAgent(@RequestBody ExecuteAgentRequest request) {
        try {
            String result = aiAgentService.executeAgent(request.getAgentCode(), request.getTask());
            return Result.success(result);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("Agent 执行失败: " + e.getMessage());
        }
    }

    @Operation(summary = "流式执行Agent任务", description = "以SSE流式方式执行AI Agent任务，实时返回响应")
    @ApiResponse(responseCode = "200", description = "流式执行开始")
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeAgentStream(@RequestBody ExecuteAgentRequest request) {
        return aiAgentService.executeAgentStream(request.getAgentCode(), request.getTask())
                .doOnError(e -> log.error("Agent 流式执行异常", e));
    }

    @Operation(summary = "获取所有可用Agent", description = "获取系统中所有可用的AI Agent列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listAgents() {
        try {
            List<Map<String, Object>> agents = aiAgentService.listAgents();
            return Result.success(agents);
        } catch (Exception e) {
            return Result.error("获取 Agent 列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "创建自定义Agent", description = "创建一个新的自定义AI Agent")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping("/create")
    public Result<String> createAgent(@RequestBody CreateAgentRequest request) {
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
            return Result.error("创建 Agent 失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取Agent对话历史", description = "获取指定Agent的对话历史记录")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/history/{agentCode}")
    public Result<List<Map<String, String>>> getChatHistory(
            @Parameter(description = "Agent编码") @PathVariable String agentCode,
            @Parameter(description = "会话ID") @RequestParam(defaultValue = "default") String sessionId) {
        try {
            List<Map<String, String>> history = aiAgentService.getChatHistory(agentCode, sessionId);
            return Result.success(history);
        } catch (Exception e) {
            return Result.error("获取聊天历史失败: " + e.getMessage());
        }
    }

    @Operation(summary = "清除会话", description = "清除指定Agent的会话对话历史")
    @ApiResponse(responseCode = "200", description = "清除成功")
    @DeleteMapping("/session/{agentCode}")
    public Result<Void> clearSession(
            @Parameter(description = "Agent编码") @PathVariable String agentCode,
            @Parameter(description = "会话ID") @RequestParam(defaultValue = "default") String sessionId) {
        try {
            aiAgentService.clearSession(agentCode, sessionId);
            return Result.success("会话已清除");
        } catch (Exception e) {
            return Result.error("清除会话失败: " + e.getMessage());
        }
    }

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
