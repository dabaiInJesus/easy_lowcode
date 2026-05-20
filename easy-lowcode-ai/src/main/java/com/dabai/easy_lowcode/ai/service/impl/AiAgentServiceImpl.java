package com.dabai.easy_lowcode.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.entity.PromptTemplate;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.mapper.AiAgentMapper;
import com.dabai.easy_lowcode.ai.mapper.PromptTemplateMapper;
import com.dabai.easy_lowcode.ai.service.AiAgentService;
import com.dabai.easy_lowcode.ai.util.AiCallLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * AI Agent 服务实现
 * <p>
 * 功能：
 * - 从 DB 加载 Agent 配置（ai_agent 表）
 * - 支持 PromptTemplate 变量替换（{{variable}}）
 * - 多轮对话（基于 sessionId 的内存会话）
 * - 使用次数统计
 * - 流式执行
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiAgentServiceImpl implements AiAgentService {

    private final AiAgentMapper aiAgentMapper;
    private final PromptTemplateMapper promptTemplateMapper;
    private final AiServiceFactory aiServiceFactory;

    /** sessionId -> 聊天历史 */
    private final Map<String, ChatSession> sessionMap = new ConcurrentHashMap<>();

    /** Agent 缓存：agentCode -> AiAgent */
    private final Map<String, AiAgent> agentCache = new ConcurrentHashMap<>();

    /** PromptTemplate 缓存：templateId -> PromptTemplate */
    private final Map<Long, PromptTemplate> templateCache = new ConcurrentHashMap<>();

    // ==================== 生命周期 ====================

    /**
     * 启动时从 DB 加载所有已发布的 Agent 和模板
     */
    @jakarta.annotation.PostConstruct
    public void loadFromDb() {
        // 加载所有已发布的 Agent
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

        // 加载所有启用的模板
        List<PromptTemplate> templates = promptTemplateMapper.selectList(
                new LambdaQueryWrapper<PromptTemplate>()
                        .eq(PromptTemplate::getStatus, 1)
        );
        for (PromptTemplate template : templates) {
            templateCache.put(template.getId(), template);
        }
        log.info("PromptTemplate 缓存加载完成，共 {} 个", templateCache.size());
    }

    /**
     * 每 10 分钟清理过期会话
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanupExpiredSessions() {
        long now = System.currentTimeMillis();
        int before = sessionMap.size();
        sessionMap.entrySet().removeIf(entry ->
                now - entry.getValue().lastAccessTime > 30 * 60 * 1000L);
        int removed = before - sessionMap.size();
        if (removed > 0) {
            log.debug("清理过期会话 {} 个，剩余 {} 个", removed, sessionMap.size());
        }
    }

    // ==================== 核心接口实现 ====================

    @Override
    public String executeAgent(String agentCode, String task) {
        long start = System.currentTimeMillis();
        AiAgent agent = resolveAgent(agentCode);
        String systemPrompt = buildSystemPrompt(agent);

        try {
            AiProvider provider = resolveProvider(agent);
            var aiService = aiServiceFactory.getService(provider);

            ChatRequest request = buildRequest(agent, systemPrompt, task);
            ChatResponse response = aiService.chat(request);

            // 更新会话历史
            updateSession(agentCode, "user", task);
            updateSession(agentCode, "assistant", response.getContent());

            // 统计使用次数
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
        String systemPrompt = buildSystemPrompt(agent);
        AiProvider provider = resolveProvider(agent);
        var aiService = aiServiceFactory.getService(provider);

        long start = System.currentTimeMillis();
        AiCallLogger.logStreamStart(provider, agent.getModel(), task);

        final long[] chunkCount = {0};

        // 更新用户消息到会话
        updateSession(agentCode, "user", task);

        try {
            ChatRequest request = buildRequest(agent, systemPrompt, task);

            Flux<String> rawStream = aiService.streamChat(request);

        final StringBuilder fullResponse = new StringBuilder();

        return rawStream
                .doOnNext(chunk -> fullResponse.append(chunk))
                .doOnComplete(() -> {
                    long elapsed = System.currentTimeMillis() - start;
                    AiCallLogger.logStreamEnd(provider, elapsed, (int) chunkCount[0], true);
                    if (fullResponse.length() > 0) {
                        updateSession(agentCode, "assistant", fullResponse.toString());
                    }
                })
                .doOnError(e -> {
                    long elapsed = System.currentTimeMillis() - start;
                    AiCallLogger.logStreamEnd(provider, elapsed, (int) chunkCount[0], false);
                });

        } catch (Exception e) {
            log.error("Agent '{}' 流式执行失败", agentCode, e);
            return Flux.error(e);
        }
    }

    @Override
    public List<Map<String, Object>> listAgents() {
        List<Map<String, Object>> result = new ArrayList<>();

        // 内置/DB 中的 Agent
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

        // 插入 DB
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

        // 写入缓存
        agentCache.put(code, agent);
        log.info("创建 Agent 成功: {} ({})", name, code);
        return code;
    }

    /**
     * 获取 Agent 对话历史
     */
    public List<Map<String, String>> getChatHistory(String agentCode, String sessionId) {
        String key = buildSessionKey(agentCode, sessionId);
        ChatSession session = sessionMap.get(key);
        if (session == null) {
            return Collections.emptyList();
        }
        return session.messages;
    }

    /**
     * 清除指定会话
     */
    public void clearSession(String agentCode, String sessionId) {
        String key = buildSessionKey(agentCode, sessionId);
        sessionMap.remove(key);
        log.info("清除会话: agent={}, sessionId={}", agentCode, sessionId);
    }

    // ==================== 私有方法 ====================

    /**
     * 解析 Agent（优先缓存，再 DB）
     */
    private AiAgent resolveAgent(String agentCode) {
        AiAgent agent = agentCache.get(agentCode);
        if (agent != null) {
            return agent;
        }
        // 查 DB（支持动态创建后立即使用）
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

    /**
     * 构建系统提示词（包含 PromptTemplate 变量替换）
     */
    private String buildSystemPrompt(AiAgent agent) {
        String prompt = agent.getInstructions();
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        // 如果配置了模板 ID，读取模板内容
        if (agent.getPromptTemplateId() != null) {
            PromptTemplate template = templateCache.get(agent.getPromptTemplateId());
            if (template != null) {
                prompt = template.getContent();
            }
        }

        // 变量替换：{{variable}} -> 从 variablesConfig JSON 中提取
        // variablesConfig 格式: {"name": "张三", "age": "18"}
        String variablesConfig = agent.getVariablesConfig();
        if (variablesConfig != null && !variablesConfig.isBlank()) {
            try {
                // 简单的手动解析（不用引入 JSON 库）
                prompt = substituteVariables(prompt, variablesConfig);
            } catch (Exception e) {
                log.warn("变量替换失败: {}", e.getMessage());
            }
        }

        return prompt;
    }

    /**
     * 替换 prompt 中的 {{variable}} 占位符
     */
    private String substituteVariables(String template, String variablesConfig) {
        // 匹配 {{key}} 形式的占位符
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        var matcher = pattern.matcher(template);
        StringBuffer sb = new StringBuffer();

        // 手动解析 JSON: {"key": "value"} 或 {"key": 123}
        Map<String, String> vars = parseSimpleJson(variablesConfig);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = vars.getOrDefault(key, "{{" + key + "}}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    /**
     * 解析简单的 JSON 对象（无嵌套）
     */
    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || !json.startsWith("{")) {
            return result;
        }
        // 去掉外层大括号
        String inner = json.trim();
        if (inner.endsWith("}")) {
            inner = inner.substring(1, inner.length() - 1);
        }
        // 按逗号分隔（不考虑引号内的逗号，简单解析）
        String[] pairs = inner.split(",");
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx < 0) continue;
            String k = pair.substring(0, colonIdx).trim();
            String v = pair.substring(colonIdx + 1).trim();
            // 去掉引号
            k = stripQuotes(k);
            v = stripQuotes(v);
            result.put(k, v);
        }
        return result;
    }

    private String stripQuotes(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            return s.substring(1, s.length() - 1);
        }
        if (s.startsWith("'") && s.endsWith("'")) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    /**
     * 构建 ChatRequest
     */
    private ChatRequest buildRequest(AiAgent agent, String systemPrompt, String task) {
        ChatRequest request = new ChatRequest();
        request.setSystemPrompt(systemPrompt);
        request.setMessage(task);
        request.setMaxTokens(agent.getMaxTokens() != null ? agent.getMaxTokens() : 2000);
        request.setTemperature(agent.getTemperature() != null ? agent.getTemperature() : 0.7);
        return request;
    }

    /**
     * 解析 provider
     */
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

    /**
     * 更新会话历史
     */
    private void updateSession(String agentCode, String role, String content) {
        if (content == null || content.isBlank()) return;
        String key = buildSessionKey(agentCode, "default");
        ChatSession session = sessionMap.computeIfAbsent(key, k -> new ChatSession());
        session.messages.add(Map.of("role", role, "content", content));
        session.lastAccessTime = System.currentTimeMillis();
    }

    /**
     * 构建会话 key
     */
    private String buildSessionKey(String agentCode, String sessionId) {
        return agentCode + ":" + (sessionId != null ? sessionId : "default");
    }

    /**
     * 增加使用次数
     */
    private void incrementUsageCount(Long agentId) {
        try {
            AiAgent agent = aiAgentMapper.selectById(agentId);
            if (agent != null) {
                agent.setUsageCount(
                        (agent.getUsageCount() != null ? agent.getUsageCount() : 0) + 1
                );
                aiAgentMapper.updateById(agent);
                // 同步更新缓存
                AiAgent cached = agentCache.get(agent.getAgentCode());
                if (cached != null) {
                    cached.setUsageCount(agent.getUsageCount());
                }
            }
        } catch (Exception e) {
            log.warn("更新使用次数失败: {}", e.getMessage());
        }
    }

    /**
     * Agent 名称转编码
     */
    private String toAgentCode(String name) {
        return name.replaceAll("[^a-zA-Z0-9\\-_]", "-").toLowerCase()
                + "-" + System.currentTimeMillis() % 10000;
    }

    // ==================== 内部类 ====================

    /** 聊天会话 */
    private static class ChatSession {
        List<Map<String, String>> messages = new ArrayList<>();
        long lastAccessTime = System.currentTimeMillis();
    }
}
