package com.dabai.easy_lowcode.ai.agent.tool;

import java.util.Map;

/**
 * Agent 工具接口
 * <p>
 * 每个工具实现此接口，提供名称、描述、参数 Schema 和执行方法。
 * LLM 通过工具描述决定是否调用，通过参数 Schema 生成调用参数。
 */
public interface AgentTool {

    /**
     * 工具唯一名称
     */
    String getName();

    /**
     * 工具描述（给 LLM 看，决定是否调用）
     */
    String getDescription();

    /**
     * 参数 Schema（JSON Schema 格式，LLM 用此生成调用参数）
     */
    Map<String, Object> getParametersSchema();

    /**
     * 执行工具
     *
     * @param params LLM 生成的参数
     * @return 工具执行结果
     */
    Object execute(Map<String, Object> params);

    /**
     * 工具类型标识
     */
    default String getToolType() {
        return "custom";
    }
}
