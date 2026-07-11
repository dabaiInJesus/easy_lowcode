package com.dabai.easy_lowcode.ai.engine;

import java.util.Map;

/**
 * 工作流节点执行器接口
 */
public interface WorkflowNodeExecutor {

    /**
     * 节点类型标识
     */
    String getNodeType();

    /**
     * 执行节点
     *
     * @param nodeId   节点ID
     * @param config   节点配置
     * @param input    上游节点输出的变量
     * @param context  执行上下文（全局变量等）
     * @return 节点输出
     */
    Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                Map<String, Object> input, Map<String, Object> context);
}
