package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.common.result.Result;
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
@Slf4j
@RestController
@RequestMapping("/api/ai")
public class HealthController {
    
    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Result<Map<String, Object>> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "easy-lowcode-ai");
        health.put("timestamp", LocalDateTime.now().toString());
        health.put("version", "1.0.0-SNAPSHOT");
        
        return Result.success(health);
    }
    
    /**
     * 获取服务信息
     */
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
