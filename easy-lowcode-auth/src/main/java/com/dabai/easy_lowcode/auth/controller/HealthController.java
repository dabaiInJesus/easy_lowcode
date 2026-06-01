package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.common.util.CacheUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * 系统健康检查控制器
 */
@Tag(name = "系统健康", description = "系统状态和健康检查")
@Slf4j
@RestController
@RequestMapping("/api/auth/health")
@RequiredArgsConstructor
public class HealthController {

    @Autowired(required = false)
    private DataSource dataSource;

    private final CacheUtil cacheUtil;
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Operation(summary = "健康检查", description = "检查系统各组件健康状态")
    @GetMapping("/check")
    public Map<String, Object> check() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 基本信息
        result.put("timestamp", LocalDateTime.now().format(FORMATTER));
        result.put("status", "OK");
        
        // 组件健康状态
        Map<String, Object> components = new LinkedHashMap<>();
        
        // 数据库健康
        components.put("database", checkDatabase());
        
        // Redis健康
        components.put("redis", checkRedis());
        
        result.put("components", components);
        
        // 检查是否有异常组件
        boolean hasError = components.values().stream()
            .filter(m -> m instanceof Map)
            .map(m -> (Map<?, ?>) m)
            .anyMatch(m -> !"UP".equals(m.get("status")));
        
        if (hasError) {
            result.put("status", "DEGRADED");
        }
        
        return result;
    }

    @Operation(summary = "详细信息", description = "获取系统详细信息")
    @GetMapping("/info")
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();
        
        // 系统信息
        result.put("timestamp", LocalDateTime.now().format(FORMATTER));
        result.put("application", "Easy Lowcode Platform");
        result.put("version", "1.0.0-SNAPSHOT");
        result.put("javaVersion", System.getProperty("java.version"));
        result.put("osName", System.getProperty("os.name"));
        result.put("osVersion", System.getProperty("os.version"));
        
        // 内存信息
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> memory = new LinkedHashMap<>();
        memory.put("total", formatBytes(runtime.totalMemory()));
        memory.put("used", formatBytes(runtime.totalMemory() - runtime.freeMemory()));
        memory.put("free", formatBytes(runtime.freeMemory()));
        memory.put("max", formatBytes(runtime.maxMemory()));
        result.put("memory", memory);
        
        // 线程信息
        Map<String, Object> threadInfo = new LinkedHashMap<>();
        ThreadGroup threadGroup = Thread.currentThread().getThreadGroup();
        while (threadGroup.getParent() != null) {
            threadGroup = threadGroup.getParent();
        }
        threadInfo.put("count", threadGroup.activeCount());
        threadInfo.put("peak", threadGroup.activeCount()); // 简化实现
        result.put("threads", threadInfo);
        
        // 组件版本
        Map<String, String> versions = new LinkedHashMap<>();
        versions.put("springBoot", "3.5.5");
        versions.put("mybatisPlus", "3.5.5");
        versions.put("vue", "3.4+");
        versions.put("elementPlus", "2.5+");
        result.put("versions", versions);
        
        return result;
    }

    @Operation(summary = "就绪检查", description = "Kubernetes就绪探针")
    @GetMapping("/ready")
    public Map<String, Object> ready() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ready", true);
        
        try {
            // 检查数据库
            if (dataSource != null) {
                try (Connection conn = dataSource.getConnection()) {
                    if (!conn.isValid(5)) {
                        result.put("ready", false);
                        result.put("reason", "Database connection invalid");
                    }
                }
            }
            
            // 检查Redis
            String testKey = "health:check:" + System.currentTimeMillis();
            cacheUtil.set(testKey, "ok", java.time.Duration.ofSeconds(10));
            cacheUtil.delete(testKey);
            
        } catch (Exception e) {
            result.put("ready", false);
            result.put("reason", e.getMessage());
        }
        
        return result;
    }

    @Operation(summary = "存活检查", description = "Kubernetes存活探针")
    @GetMapping("/live")
    public Map<String, Object> live() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("alive", true);
        result.put("timestamp", LocalDateTime.now().format(FORMATTER));
        return result;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            if (dataSource != null) {
                long startTime = System.currentTimeMillis();
                try (Connection conn = dataSource.getConnection()) {
                    long latency = System.currentTimeMillis() - startTime;
                    status.put("status", conn.isValid(5) ? "UP" : "DOWN");
                    status.put("latency", latency + "ms");
                    status.put("url", conn.getMetaData().getURL());
                }
            } else {
                status.put("status", "UNKNOWN");
                status.put("message", "DataSource not available");
            }
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> status = new LinkedHashMap<>();
        try {
            String testKey = "health:redis:" + System.currentTimeMillis();
            long startTime = System.currentTimeMillis();
            cacheUtil.set(testKey, "ok", java.time.Duration.ofSeconds(10));
            String value = cacheUtil.get(testKey);
            long latency = System.currentTimeMillis() - startTime;
            
            if ("ok".equals(value)) {
                status.put("status", "UP");
                status.put("latency", latency + "ms");
            } else {
                status.put("status", "DOWN");
                status.put("message", "Redis read/write failed");
            }
            cacheUtil.delete(testKey);
        } catch (Exception e) {
            status.put("status", "DOWN");
            status.put("error", e.getMessage());
        }
        return status;
    }

    private String formatBytes(long bytes) {
        if (bytes < 0) return "unknown";
        long mb = bytes / (1024 * 1024);
        return mb + " MB";
    }
}