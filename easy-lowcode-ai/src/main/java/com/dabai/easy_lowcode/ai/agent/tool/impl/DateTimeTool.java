package com.dabai.easy_lowcode.ai.agent.tool.impl;

import com.dabai.easy_lowcode.ai.agent.tool.AgentTool;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * 日期时间工具
 */
@Component
public class DateTimeTool implements AgentTool {

    @Override
    public String getName() {
        return "datetime";
    }

    @Override
    public String getDescription() {
        return "获取当前日期和时间，或格式化日期时间。例如: 获取当前时间、格式化为 yyyy-MM-dd 格式";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "action", Map.of(
                                "type", "string",
                                "description", "操作类型: now/format",
                                "enum", List.of("now", "format")
                        ),
                        "format", Map.of(
                                "type", "string",
                                "description", "日期格式，如 yyyy-MM-dd HH:mm:ss"
                        )
                )
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String action = (String) params.getOrDefault("action", "now");
        String format = (String) params.getOrDefault("format", "yyyy-MM-dd HH:mm:ss");

        LocalDateTime now = LocalDateTime.now();

        return switch (action) {
            case "format" -> Map.of(
                    "datetime", now.format(DateTimeFormatter.ofPattern(format)),
                    "format", format
            );
            default -> Map.of(
                    "datetime", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                    "date", now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                    "time", now.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    "year", now.getYear(),
                    "month", now.getMonthValue(),
                    "day", now.getDayOfMonth(),
                    "dayOfWeek", now.getDayOfWeek().toString()
            );
        };
    }

    @Override
    public String getToolType() {
        return "datetime";
    }
}
