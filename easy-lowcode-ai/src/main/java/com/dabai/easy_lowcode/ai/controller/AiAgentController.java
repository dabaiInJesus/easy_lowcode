package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.agent.AgentExecutor;
import com.dabai.easy_lowcode.ai.agent.tool.ToolRegistry;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.mapper.AiAgentMapper;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
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
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

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
    private final AgentExecutor agentExecutor;
    private final ToolRegistry toolRegistry;
    private final AiAgentMapper aiAgentMapper;

    @Operation(summary = "执行Agent任务", description = "执行指定的AI Agent任务")
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

    @Operation(summary = "流式执行Agent任务", description = "以SSE流式方式执行AI Agent任务")
    @PostMapping(value = "/execute/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> executeAgentStream(@RequestBody ExecuteAgentRequest request) {
        return aiAgentService.executeAgentStream(request.getAgentCode(), request.getTask())
                .doOnError(e -> log.error("Agent 流式执行异常", e));
    }

    @Operation(summary = "增强Agent对话（SSE）", description = "支持工具调用的Agent对话，实时推送思考过程和工具执行结果")
    @PostMapping(value = "/{agentCode}/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatWithAgent(
            @Parameter(description = "Agent编码") @PathVariable String agentCode,
            @RequestBody Map<String, String> request) {

        SseEmitter emitter = new SseEmitter(300_000L);
        String message = request.get("message");
        String sessionId = request.getOrDefault("sessionId", UUID.randomUUID().toString());

        if (message == null || message.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"消息不能为空\"}"));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // 查找 Agent
        AiAgent agent = aiAgentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getAgentCode, agentCode)
                        .eq(AiAgent::getStatus, 1));
        if (agent == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"Agent不存在: " + agentCode + "\"}"));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // 异步执行
        CompletableFuture.runAsync(() -> {
            try {
                agentExecutor.executeAgent(agent, message, sessionId, emitter);
            } catch (Exception e) {
                log.error("Agent对话异常: agentCode={}", agentCode, e);
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Operation(summary = "获取所有可用Agent")
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> listAgents() {
        try {
            List<Map<String, Object>> agents = aiAgentService.listAgents();
            return Result.success(agents);
        } catch (Exception e) {
            return Result.error("获取 Agent 列表失败: " + e.getMessage());
        }
    }

    @Operation(summary = "获取Agent详情")
    @GetMapping("/{id}")
    public Result<AiAgent> getAgentDetail(@Parameter(description = "AgentID") @PathVariable Long id) {
        AiAgent agent = aiAgentMapper.selectById(id);
        if (agent == null) throw new BusinessException("Agent不存在");
        // 掩码处理
        return Result.success(agent);
    }

    @Operation(summary = "创建自定义Agent")
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

    @Operation(summary = "更新Agent")
    @PutMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updateAgent(@RequestBody AiAgent agent) {
        aiAgentMapper.updateById(agent);
        return Result.success("更新成功");
    }

    @Operation(summary = "获取可用工具列表")
    @GetMapping("/tools")
    public Result<List<Map<String, Object>>> getTools() {
        return Result.success(toolRegistry.getToolDescriptions());
    }

    @Operation(summary = "获取Agent对话历史")
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

    @Operation(summary = "清除会话")
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
