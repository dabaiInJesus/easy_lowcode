package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.*;

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
    
    /**
     * 执行 Agent 任务
     */
    @PostMapping("/execute")
    public Result<String> executeAgent(@RequestBody ExecuteAgentRequest request) {
        log.info("执行 Agent 任务: {}", request.getAgentName());
        
        try {
            String result = aiAgentService.executeAgent(request.getAgentName(), request.getTask());
            return Result.success(result);
        } catch (Exception e) {
            log.error("Agent 执行失败", e);
            return Result.error("Agent 执行失败: " + e.getMessage());
        }
    }
    
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
            String agentId = aiAgentService.createAgent(
                    request.getName(), 
                    request.getDescription(), 
                    request.getInstructions()
            );
            return Result.success(agentId);
        } catch (Exception e) {
            log.error("创建 Agent 失败", e);
            return Result.error("创建 Agent 失败: " + e.getMessage());
        }
    }
    
    /**
     * 执行 Agent 任务请求对象
     */
    @lombok.Data
    public static class ExecuteAgentRequest {
        private String agentName;
        private String task;
    }
    
    /**
     * 创建 Agent 请求对象
     */
    @lombok.Data
    public static class CreateAgentRequest {
        private String name;
        private String description;
        private String instructions;
    }
}
