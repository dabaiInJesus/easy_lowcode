package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

import static com.dabai.easy_lowcode.ai.enums.AiProvider.DASHSCOPE;

/**
 * AI Agent 服务实现
 * <p>
 * 基于 AiServiceFactory 实现简单的角色扮演 Agent。
 * 每个 Agent 有自己的 system prompt，接收任务后通过 AI 对话执行。
 * <p>
 * 高级 Agent 功能（如工具调用、多步骤推理）需要 Spring AI Alibaba Agent Framework，
 * 当前实现适用于简单的角色对话场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl implements AiAgentService {

    /**
     * Agent 注册表：name -> AgentConfig
     * 支持通过 createAgent() 动态注册自定义 Agent
     */
    private final Map<String, AgentConfig> agentRegistry = new HashMap<>();

    private final AiServiceFactory aiServiceFactory;

    /**
     * 系统内置 Agent 定义（不可删除，仅读）
     */
    private static final Map<String, AgentConfig> BUILT_IN_AGENTS = new LinkedHashMap<>();

    static {
        // 数据分析 Agent
        BUILT_IN_AGENTS.put("data-analyst", new AgentConfig(
                "data-analyst",
                "数据分析师",
                "你是一个专业的数据分析师，擅长用简洁清晰的语言解释数据、趋势和洞察。",
                DASHSCOPE
        ));
        // 代码审查 Agent
        BUILT_IN_AGENTS.put("code-reviewer", new AgentConfig(
                "code-reviewer",
                "代码审查员",
                "你是一个经验丰富的架构师，擅长审查代码质量、性能和安全问题，给出具体的改进建议。",
                DASHSCOPE
        ));
        // 文档撰写 Agent
        BUILT_IN_AGENTS.put("doc-writer", new AgentConfig(
                "doc-writer",
                "技术文档撰写员",
                "你是一个专业技术文档撰写专家，擅长将复杂的技术概念用通俗易懂的语言解释清楚。",
                DASHSCOPE
        ));
    }

    @Override
    public String executeAgent(String agentName, String task) {
        log.info("执行 Agent: {}, 任务: {}", agentName, task);

        try {
            AgentConfig config = resolveAgentConfig(agentName);
            if (config == null) {
                throw new IllegalArgumentException("Agent 不存在: " + agentName);
            }

            // 通过 AiServiceFactory 获取对应的 AI 服务
            var aiService = aiServiceFactory.getService(config.provider);

            // 构建带 system prompt 的请求
            ChatRequest request = new ChatRequest();
            request.setSystemPrompt(config.instructions);
            request.setMessage(task);
            request.setMaxTokens(2000);

            var response = aiService.chat(request);
            log.info("Agent '{}' 执行成功", agentName);
            return response.getContent();

        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            log.error("Agent '{}' 执行失败", agentName, e);
            throw new RuntimeException("Agent 执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<Map<String, Object>> listAgents() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 内置 Agent
        for (AgentConfig config : BUILT_IN_AGENTS.values()) {
            result.add(toAgentInfo(config, "built-in"));
        }
        // 自定义 Agent
        for (AgentConfig config : agentRegistry.values()) {
            result.add(toAgentInfo(config, "custom"));
        }
        return result;
    }

    @Override
    public String createAgent(String name, String description, String instructions) {
        log.info("创建自定义 Agent: {}", name);

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Agent 名称不能为空");
        }
        if (agentRegistry.containsKey(name) || BUILT_IN_AGENTS.containsKey(name)) {
            throw new IllegalArgumentException("Agent 已存在: " + name);
        }

        AgentConfig config = new AgentConfig(name, description, instructions, DASHSCOPE);
        agentRegistry.put(name, config);
        log.info("自定义 Agent 创建成功: {}", name);
        return name;
    }

    /**
     * 根据名称解析 Agent 配置（先自定义，后内置）
     */
    private AgentConfig resolveAgentConfig(String agentName) {
        if (agentRegistry.containsKey(agentName)) {
            return agentRegistry.get(agentName);
        }
        return BUILT_IN_AGENTS.get(agentName);
    }

    private Map<String, Object> toAgentInfo(AgentConfig config, String type) {
        Map<String, Object> info = new HashMap<>();
        info.put("name", config.name);
        info.put("description", config.description);
        info.put("type", type);
        info.put("provider", config.provider.getCode());
        info.put("status", "active");
        return info;
    }

    /**
     * Agent 配置内部类
     */
    private record AgentConfig(
            String name,
            String description,
            String instructions,
            AiProvider provider
    ) {}
}
