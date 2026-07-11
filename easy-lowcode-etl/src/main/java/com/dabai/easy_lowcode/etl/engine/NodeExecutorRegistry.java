package com.dabai.easy_lowcode.etl.engine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 节点执行器注册中心
 * <p>
 * 自动扫描所有 NodeExecutor 实现，按 nodeType 注册。
 */
@Slf4j
@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> executorMap = new HashMap<>();

    @Autowired
    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        for (NodeExecutor executor : executors) {
            executorMap.put(executor.getNodeType(), executor);
            log.info("注册节点执行器: type={}, category={}, class={}",
                    executor.getNodeType(), executor.getCategory(), executor.getClass().getSimpleName());
        }
        log.info("节点执行器注册完成，共注册 {} 个", executorMap.size());
    }

    /**
     * 根据节点类型获取执行器
     */
    public NodeExecutor getExecutor(String nodeType) {
        NodeExecutor executor = executorMap.get(nodeType);
        if (executor == null) {
            throw new IllegalArgumentException("未找到节点类型对应的执行器: " + nodeType);
        }
        return executor;
    }

    /**
     * 获取所有已注册的执行器
     */
    public Map<String, NodeExecutor> getAllExecutors() {
        return Map.copyOf(executorMap);
    }

    /**
     * 检查节点类型是否已注册
     */
    public boolean isRegistered(String nodeType) {
        return executorMap.containsKey(nodeType);
    }
}
