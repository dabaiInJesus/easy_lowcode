package com.dabai.easy_lowcode.ai.service.impl;

import com.dabai.easy_lowcode.ai.entity.AiAgent;
import com.dabai.easy_lowcode.ai.entity.PromptTemplate;
import com.dabai.easy_lowcode.ai.mapper.PromptTemplateMapper;
import com.dabai.easy_lowcode.ai.service.PromptProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Prompt 处理器实现
 * 支持从 PromptTemplate 加载内容，并替换 {{variable}} 占位符
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PromptProcessorImpl implements PromptProcessor {

    private final PromptTemplateMapper promptTemplateMapper;

    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{(\\w+)\\}\\}");

    @Override
    public String buildSystemPrompt(AiAgent agent) {
        String prompt = agent.getInstructions();
        if (prompt == null || prompt.isBlank()) {
            return "";
        }

        if (agent.getPromptTemplateId() != null) {
            PromptTemplate template = promptTemplateMapper.selectById(agent.getPromptTemplateId());
            if (template != null) {
                prompt = template.getContent();
            }
        }

        String variablesConfig = agent.getVariablesConfig();
        if (variablesConfig != null && !variablesConfig.isBlank()) {
            try {
                prompt = substituteVariables(prompt, variablesConfig);
            } catch (Exception e) {
                log.warn("变量替换失败: {}", e.getMessage());
            }
        }

        return prompt;
    }

    @Override
    public String substituteVariables(String template, String variablesConfig) {
        Matcher matcher = VARIABLE_PATTERN.matcher(template);
        StringBuffer sb = new StringBuffer();

        Map<String, String> vars = parseSimpleJson(variablesConfig);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = vars.getOrDefault(key, "{{" + key + "}}");
            matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private Map<String, String> parseSimpleJson(String json) {
        Map<String, String> result = new HashMap<>();
        if (json == null || !json.startsWith("{")) {
            return result;
        }
        String inner = json.trim();
        if (inner.endsWith("}")) {
            inner = inner.substring(1, inner.length() - 1);
        }
        String[] pairs = inner.split(",");
        for (String pair : pairs) {
            int colonIdx = pair.indexOf(':');
            if (colonIdx < 0) continue;
            String k = pair.substring(0, colonIdx).trim();
            String v = pair.substring(colonIdx + 1).trim();
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
}
