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
 * 自动扫描所有 NodeExecutor 实现，按「类别 + 节点类型」双键注册。
 * <p>
 * 同一 nodeType 在不同类别下有不同执行器（如 mysql 既有 SOURCE 的读取器
 * 也有 TARGET 的写入器），早期单键实现会导致 SOURCE/TARGET 互相覆盖，
 * 谁存活完全取决于扫描顺序，属于隐患缺陷，现改为双键彻底区分。
 */
@Slf4j
@Component
public class NodeExecutorRegistry {

    private final Map<String, NodeExecutor> executorMap = new HashMap<>();

    @Autowired
    public NodeExecutorRegistry(List<NodeExecutor> executors) {
        for (NodeExecutor executor : executors) {
            String key = key(executor.getCategory(), executor.getNodeType());
            NodeExecutor prev = executorMap.put(key, executor);
            if (prev != null) {
                log.warn("节点执行器键冲突覆盖: key={}, 新={}, 旧={}",
                        key, executor.getClass().getSimpleName(), prev.getClass().getSimpleName());
            }
            log.info("注册节点执行器: key={}, category={}, type={}, class={}",
                    key, executor.getCategory(), executor.getNodeType(),
                    executor.getClass().getSimpleName());
        }
        log.info("节点执行器注册完成，共注册 {} 个", executorMap.size());
    }

    /**
     * 根据节点类别与类型获取执行器
     *
     * @param nodeType 节点子类型（mysql/csv/filter 等）
     * @param category 节点类别（SOURCE/TRANSFORM/TARGET）
     */
    public NodeExecutor getExecutor(String nodeType, String category) {
        NodeExecutor executor = executorMap.get(key(category, nodeType));
        if (executor == null) {
            throw new IllegalArgumentException(
                    "未找到节点类型对应的执行器: category=" + category + ", type=" + nodeType);
        }
        return executor;
    }

    /**
     * 获取所有已注册的执行器（键格式: category:nodeType）
     */
    public Map<String, NodeExecutor> getAllExecutors() {
        return Map.copyOf(executorMap);
    }

    /**
     * 检查指定类别的节点类型是否已注册
     */
    public boolean isRegistered(String nodeType, String category) {
        return executorMap.containsKey(key(category, nodeType));
    }

    private static String key(String category, String nodeType) {
        return category + ":" + nodeType;
    }
}
