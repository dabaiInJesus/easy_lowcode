package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康检查控制器
 */
@Tag(name = "AI健康检查", description = "AI模块健康检查及服务信息")
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class HealthController {
    
    @Operation(summary = "健康检查", description = "检查AI模块的运行状态")
    @ApiResponse(responseCode = "200", description = "检查完成")
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "easy-lowcode-ai");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "1.0.0-SNAPSHOT");
        
        return Result.success(health);
    }
    
    @Operation(summary = "获取服务信息", description = "获取AI模块的版本和支持的提供商信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/info")
    public Result<Map<String, Object>> info() {
        Map<String, Object> info = new HashMap<>();
        info.put("name", "Easy Lowcode AI Module");
        info.put("description", "AI 模块 - 支持多种大模型提供商");
        info.put("version", "1.0.0-SNAPSHOT");
        info.put("supportedProviders", new String[]{
            "openai",
            "dashscope",
            "ollama",
            "deepseek",
            "minimax"
        });
        
        return Result.success(info);
    }
}
