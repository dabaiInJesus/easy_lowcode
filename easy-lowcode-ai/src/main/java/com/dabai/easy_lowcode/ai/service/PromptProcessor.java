package com.dabai.easy_lowcode.ai.service;

import com.dabai.easy_lowcode.ai.entity.AiAgent;

/**
 * Prompt 处理器
 * 负责构建系统提示词，支持模板变量替换
 */
public interface PromptProcessor {

    /**
     * 构建系统提示词（包含 PromptTemplate 变量替换）
     */
    String buildSystemPrompt(AiAgent agent);

    /**
     * 替换模板中的 {{variable}} 占位符
     */
    String substituteVariables(String template, String variablesConfig);
}
