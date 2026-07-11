package com.dabai.easy_lowcode.ai.agent.tool;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具注册中心
 * <p>
 * 自动扫描所有 AgentTool 实现，按名称注册。
 */
@Slf4j
@Component
public class ToolRegistry {

    private final Map<String, AgentTool> toolMap = new HashMap<>();

    @Autowired
    public ToolRegistry(List<AgentTool> tools) {
        for (AgentTool tool : tools) {
            toolMap.put(tool.getName(), tool);
            log.info("注册Agent工具: name={}, type={}", tool.getName(), tool.getToolType());
        }
        log.info("Agent工具注册完成，共注册 {} 个", toolMap.size());
    }

    /**
     * 根据名称获取工具
     */
    public AgentTool getTool(String name) {
        return toolMap.get(name);
    }

    /**
     * 获取所有已注册工具
     */
    public Map<String, AgentTool> getAllTools() {
        return Map.copyOf(toolMap);
    }

    /**
     * 获取工具描述列表（给 LLM 用）
     */
    public List<Map<String, Object>> getToolDescriptions() {
        return toolMap.values().stream()
                .map(tool -> {
                    Map<String, Object> desc = new HashMap<>();
                    desc.put("name", tool.getName());
                    desc.put("description", tool.getDescription());
                    desc.put("parameters", tool.getParametersSchema());
                    return desc;
                })
                .toList();
    }
}
