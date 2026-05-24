package com.dabai.easy_lowcode.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.mapper.AiAgentMapper;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.ai.service.PromptProcessor;
import com.dabai.easy_lowcode.ai.service.SessionManager;
import com.dabai.easy_lowcode.ai.util.AiCallLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI Agent 服务实现（重构后）
 * 仅保留 Agent 生命周期管理，会话管理和 Prompt 处理已拆分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl implements AiAgentService {

    private final AiAgentMapper aiAgentMapper;
    private final AiServiceFactory aiServiceFactory;
    private final PromptProcessor promptProcessor;
    private final SessionManager sessionManager;

    private final Map<String, AiAgent> agentCache = new ConcurrentHashMap<>();

    @jakarta.annotation.PostConstruct
    public void loadFromDb() {
        List<AiAgent> agents = aiAgentMapper.selectList(
                new LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getStatus, 1)
                        .eq(AiAgent::getPublishStatus, 1)
        );
        for (AiAgent agent : agents) {
            agentCache.put(agent.getAgentCode(), agent);
            log.info("加载 Agent: {} ({})", agent.getAgentName(), agent.getAgentCode());
        }
        log.info("Agent 缓存加载完成，共 {} 个", agentCache.size());
    }

    @Scheduled(fixedRate = 600_000)
    public void cleanupExpiredSessions() {
        sessionManager.cleanupExpiredSessions();
    }

    @Override
    public String executeAgent(String agentCode, String task) {
        long start = System.currentTimeMillis();
        AiAgent agent = resolveAgent(agentCode);
        String systemPrompt = promptProcessor.buildSystemPrompt(agent);

        try {
            AiProvider provider = resolveProvider(agent);
            var aiService = aiServiceFactory.getService(provider);

            ChatRequest request = buildRequest(agent, systemPrompt, task);
            ChatResponse response = aiService.chat(request);

            sessionManager.addMessage(agentCode, "default", "user", task);
            sessionManager.addMessage(agentCode, "default", "assistant", response.getContent());

            incrementUsageCount(agent.getId());

            long elapsed = System.currentTimeMillis() - start;
            AiCallLogger.logResponse(provider, agent.getModel(), elapsed,
                    response.getContent() != null ? response.getContent().length() : 0, true);

            log.info("Agent '{}' 执行成功，耗时 {}ms", agentCode, elapsed);
            return response.getContent();

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            log.error("Agent '{}' 执行失败，耗时 {}ms", agentCode, elapsed, e);
            throw new RuntimeException("Agent 执行失败: " + e.getMessage(), e);
        }
    }

    @Override
    public Flux<String> executeAgentStream(String agentCode, String task) {
        AiAgent agent = resolveAgent(agentCode);
        String systemPrompt = promptProcessor.buildSystemPrompt(agent);
        AiProvider provider = resolveProvider(agent);
        var aiService = aiServiceFactory.getService(provider);

        long start = System.currentTimeMillis();
        AiCallLogger.logStreamStart(provider, agent.getModel(), task);

        sessionManager.addMessage(agentCode, "default", "user", task);

        try {
            ChatRequest request = buildRequest(agent, systemPrompt, task);

            Flux<String> rawStream = aiService.streamChat(request);
            final StringBuilder fullResponse = new StringBuilder();

            return rawStream
                    .doOnNext(fullResponse::append)
                    .doOnComplete(() -> {
                        long elapsed = System.currentTimeMillis() - start;
                        AiCallLogger.logStreamEnd(provider, elapsed, fullResponse.length(), true);
                        if (fullResponse.length() > 0) {
                            sessionManager.addMessage(agentCode, "default", "assistant", fullResponse.toString());
                        }
                    })
                    .doOnError(e -> {
                        long elapsed = System.currentTimeMillis() - start;
                        AiCallLogger.logStreamEnd(provider, elapsed, fullResponse.length(), false);
                    });

        } catch (Exception e) {
            log.error("Agent '{}' 流式执行失败", agentCode, e);
            return Flux.error(e);
        }
    }

    @Override
    public List<Map<String, Object>> listAgents() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiAgent agent : agentCache.values()) {
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("code", agent.getAgentCode());
            info.put("name", agent.getAgentName());
            info.put("description", agent.getDescription());
            info.put("type", "db");
            info.put("provider", agent.getProvider());
            info.put("model", agent.getModel());
            info.put("status", agent.getStatus());
            info.put("usageCount", agent.getUsageCount());
            result.add(info);
        }
        return result;
    }

    @Override
    public String createAgent(String name, String description, String instructions) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Agent 名称不能为空");
        }
        String code = toAgentCode(name);

        AiAgent agent = new AiAgent();
        agent.setAgentName(name);
        agent.setAgentCode(code);
        agent.setDescription(description);
        agent.setInstructions(instructions);
        agent.setProvider("dashscope");
        agent.setModel("qwen-turbo");
        agent.setStatus(1);
        agent.setPublishStatus(1);
        agent.setUsageCount(0);

        aiAgentMapper.insert(agent);
        agentCache.put(code, agent);
        log.info("创建 Agent 成功: {} ({})", name, code);
        return code;
    }

    @Override
    public List<Map<String, String>> getChatHistory(String agentCode, String sessionId) {
        return sessionManager.getHistory(agentCode, sessionId);
    }

    @Override
    public void clearSession(String agentCode, String sessionId) {
        sessionManager.clearSession(agentCode, sessionId);
    }

    private AiAgent resolveAgent(String agentCode) {
        AiAgent agent = agentCache.get(agentCode);
        if (agent != null) {
            return agent;
        }
        agent = aiAgentMapper.selectOne(
                new LambdaQueryWrapper<AiAgent>()
                        .eq(AiAgent::getAgentCode, agentCode)
                        .eq(AiAgent::getStatus, 1)
        );
        if (agent == null) {
            throw new IllegalArgumentException("Agent 不存在: " + agentCode);
        }
        agentCache.put(agentCode, agent);
        return agent;
    }

    private ChatRequest buildRequest(AiAgent agent, String systemPrompt, String task) {
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt(systemPrompt);
        request.setMessage(task);
        request.setMaxTokens(agent.getMaxTokens() != null ? agent.getMaxTokens() : 2000);
        request.setTemperature(agent.getTemperature() != null ? agent.getTemperature() : 0.7);
        return request;
    }

    private AiProvider resolveProvider(AiAgent agent) {
        String code = agent.getProvider();
        if (code == null || code.isBlank()) {
            return AiProvider.DASHSCOPE;
        }
        try {
            return AiProvider.fromCode(code);
        } catch (IllegalArgumentException e) {
            log.warn("未知的 provider '{}'，使用 DASHSCOPE", code);
            return AiProvider.DASHSCOPE;
        }
    }

    private void incrementUsageCount(Long agentId) {
        try {
            AiAgent agent = aiAgentMapper.selectById(agentId);
            if (agent != null) {
                agent.setUsageCount(
                        (agent.getUsageCount() != null ? agent.getUsageCount() : 0) + 1
                );
                aiAgentMapper.updateById(agent);
                AiAgent cached = agentCache.get(agent.getAgentCode());
                if (cached != null) {
                    cached.setUsageCount(agent.getUsageCount());
                }
            }
        } catch (Exception e) {
            log.warn("更新使用次数失败: {}", e.getMessage());
        }
    }

    private String toAgentCode(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\-_]", "-").toLowerCase()
                + "-" + System.currentTimeMillis() % 10000;
    }
}
