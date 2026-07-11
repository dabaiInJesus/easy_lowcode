package com.dabai.easy_lowcode.ai.engine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dabai.easy_lowcode.ai.entity.AiWorkflow;
import com.dabai.easy_lowcode.ai.entity.AiWorkflowExecution;
import com.dabai.easy_lowcode.ai.mapper.AiWorkflowExecutionMapper;
import com.dabai.easy_lowcode.ai.mapper.AiWorkflowMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 工作流执行引擎
 * <p>
 * 解析工作流定义（nodes + edges），拓扑排序后按序执行各节点，
 * 通过 SSE 实时推送执行状态给前端。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WorkflowEngine {

    private final List<WorkflowNodeExecutor> executors;
    private final AiWorkflowMapper workflowMapper;
    private final AiWorkflowExecutionMapper executionMapper;

    private final Map<Long, SseEmitter> runningEmitters = new ConcurrentHashMap<>();

    /**
     * 执行工作流（SSE 流式）
     */
    public void executeWorkflow(AiWorkflow workflow, AiWorkflowExecution execution,
                                Map<String, Object> inputVariables, SseEmitter emitter) {
        runningEmitters.put(execution.getId(), emitter);

        emitter.onCompletion(() -> runningEmitters.remove(execution.getId()));
        emitter.onTimeout(() -> runningEmitters.remove(execution.getId()));
        emitter.onError(e -> runningEmitters.remove(execution.getId()));

        try {
            // 1. 解析节点和连线
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> nodes = (List<Map<String, Object>>) (List<?>) JSON.parseArray(workflow.getNodesJson(), Map.class);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> edges = (List<Map<String, Object>>) (List<?>) JSON.parseArray(workflow.getEdgesJson(), Map.class);

            // 2. 构建执行器映射
            Map<String, WorkflowNodeExecutor> executorMap = new HashMap<>();
            for (WorkflowNodeExecutor executor : executors) {
                executorMap.put(executor.getNodeType(), executor);
            }

            // 3. 拓扑排序
            List<Map<String, Object>> sortedNodes = topologicalSort(nodes, edges);

            // 4. 构建邻接表（用于条件分支）
            Map<String, List<Map<String, Object>>> adjacency = buildAdjacency(edges);

            // 5. 执行节点
            Map<String, Object> globalContext = new LinkedHashMap<>(inputVariables != null ? inputVariables : Map.of());
            String currentNodeId = findStartNode(sortedNodes);

            while (currentNodeId != null) {
                Map<String, Object> currentNode = findNodeById(sortedNodes, currentNodeId);
                if (currentNode == null) break;

                String nodeType = (String) currentNode.get("type");
                String nodeName = (String) currentNode.get("data");

                // 推送节点开始事件
                sendEvent(emitter, "node_start", Map.of(
                        "nodeId", currentNodeId,
                        "nodeType", nodeType,
                        "nodeName", nodeName != null ? nodeName : nodeType
                ));

                // 执行节点
                WorkflowNodeExecutor executor = executorMap.get(nodeType);
                if (executor == null) {
                    sendEvent(emitter, "node_error", Map.of(
                            "nodeId", currentNodeId,
                            "error", "未找到节点类型: " + nodeType
                    ));
                    break;
                }

                @SuppressWarnings("unchecked")
                Map<String, Object> nodeConfig = (Map<String, Object>) currentNode.get("data") != null
                        ? (Map<String, Object>) currentNode.get("data") : Map.of();

                Map<String, Object> output;
                try {
                    output = executor.execute(currentNodeId, nodeConfig, globalContext, globalContext);
                } catch (Exception e) {
                    sendEvent(emitter, "node_error", Map.of(
                            "nodeId", currentNodeId,
                            "error", e.getMessage()
                    ));
                    updateExecutionStatus(execution, "FAILED", e.getMessage());
                    emitter.complete();
                    return;
                }

                // 合并输出到全局上下文
                globalContext.putAll(output);

                // 推送节点完成事件
                sendEvent(emitter, "node_complete", Map.of(
                        "nodeId", currentNodeId,
                        "output", output
                ));

                // 6. 确定下一个节点
                currentNodeId = findNextNode(currentNodeId, nodeType, output, adjacency, sortedNodes);
            }

            // 7. 推送工作流完成事件
            sendEvent(emitter, "workflow_complete", Map.of(
                    "output", globalContext
            ));

            // 8. 更新执行记录
            updateExecutionStatus(execution, "SUCCESS", null);
            execution.setOutputVariables(JSON.toJSONString(globalContext));
            executionMapper.updateById(execution);

            emitter.complete();

        } catch (Exception e) {
            log.error("工作流执行异常: workflowId={}", workflow.getId(), e);
            sendEvent(emitter, "workflow_error", Map.of("error", e.getMessage()));
            updateExecutionStatus(execution, "FAILED", e.getMessage());
            emitter.complete();
        } finally {
            runningEmitters.remove(execution.getId());
        }
    }

    /**
     * 停止执行
     */
    public boolean stopExecution(Long executionId) {
        SseEmitter emitter = runningEmitters.remove(executionId);
        if (emitter != null) {
            emitter.complete();
            return true;
        }
        return false;
    }

    /**
     * 拓扑排序
     */
    private List<Map<String, Object>> topologicalSort(List<Map<String, Object>> nodes, List<Map<String, Object>> edges) {
        Map<String, Map<String, Object>> nodeMap = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            nodeMap.put((String) node.get("id"), node);
        }

        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (Map<String, Object> node : nodes) {
            String id = (String) node.get("id");
            adjacency.put(id, new ArrayList<>());
            inDegree.put(id, 0);
        }
        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get("source");
            String target = (String) edge.get("target");
            if (source != null && target != null && adjacency.containsKey(source)) {
                adjacency.get(source).add(target);
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (Map.Entry<String, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) queue.add(entry.getKey());
        }

        List<Map<String, Object>> sorted = new ArrayList<>();
        while (!queue.isEmpty()) {
            String nodeId = queue.poll();
            sorted.add(nodeMap.get(nodeId));
            for (String neighbor : adjacency.getOrDefault(nodeId, List.of())) {
                inDegree.merge(neighbor, -1, Integer::sum);
                if (inDegree.get(neighbor) == 0) queue.add(neighbor);
            }
        }
        return sorted;
    }

    /**
     * 构建邻接表
     */
    private Map<String, List<Map<String, Object>>> buildAdjacency(List<Map<String, Object>> edges) {
        Map<String, List<Map<String, Object>>> adjacency = new HashMap<>();
        for (Map<String, Object> edge : edges) {
            String source = (String) edge.get("source");
            adjacency.computeIfAbsent(source, k -> new ArrayList<>()).add(edge);
        }
        return adjacency;
    }

    /**
     * 找到开始节点
     */
    private String findStartNode(List<Map<String, Object>> nodes) {
        for (Map<String, Object> node : nodes) {
            if ("start".equals(node.get("type"))) {
                return (String) node.get("id");
            }
        }
        return nodes.isEmpty() ? null : (String) nodes.get(0).get("id");
    }

    /**
     * 根据 ID 查找节点
     */
    private Map<String, Object> findNodeById(List<Map<String, Object>> nodes, String nodeId) {
        for (Map<String, Object> node : nodes) {
            if (nodeId.equals(node.get("id"))) return node;
        }
        return null;
    }

    /**
     * 确定下一个执行节点
     */
    private String findNextNode(String currentNodeId, String nodeType, Map<String, Object> output,
                                Map<String, List<Map<String, Object>>> adjacency,
                                List<Map<String, Object>> sortedNodes) {
        List<Map<String, Object>> outgoingEdges = adjacency.getOrDefault(currentNodeId, List.of());

        if (outgoingEdges.isEmpty()) return null;

        if ("condition".equals(nodeType)) {
            // 条件节点：根据 branch 输出选择对应的边
            String branch = (String) output.get("branch");
            for (Map<String, Object> edge : outgoingEdges) {
                String edgeLabel = (String) edge.get("label");
                if (branch.equals(edgeLabel)) {
                    return (String) edge.get("target");
                }
            }
            // 未找到匹配的分支，走第一条边
            return (String) outgoingEdges.get(0).get("target");
        }

        // 普通节点：走第一条边
        return (String) outgoingEdges.get(0).get("target");
    }

    /**
     * 推送 SSE 事件
     */
    private void sendEvent(SseEmitter emitter, String eventName, Map<String, Object> data) {
        try {
            Map<String, Object> event = new LinkedHashMap<>();
            event.put("type", eventName);
            event.put("data", data);
            event.put("timestamp", System.currentTimeMillis());
            emitter.send(SseEmitter.event().name(eventName).data(JSON.toJSONString(event)));
        } catch (IOException e) {
            log.warn("SSE推送失败: event={}", eventName, e);
        }
    }

    /**
     * 更新执行记录状态
     */
    private void updateExecutionStatus(AiWorkflowExecution execution, String status, String errorMessage) {
        execution.setExecStatus(status);
        execution.setEndTime(LocalDateTime.now());
        if (errorMessage != null) {
            execution.setErrorMessage(errorMessage);
        }
        executionMapper.updateById(execution);
    }
}
