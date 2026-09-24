package com.dabai.easy_lowcode.etl.engine;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.dabai.easy_lowcode.etl.entity.EtlFlow;
import com.dabai.easy_lowcode.etl.entity.EtlFlowExecution;
import com.dabai.easy_lowcode.etl.entity.EtlNodeConfig;
import com.dabai.easy_lowcode.etl.mapper.EtlFlowExecutionMapper;
import com.dabai.easy_lowcode.etl.mapper.EtlFlowMapper;
import com.dabai.easy_lowcode.etl.mapper.EtlNodeConfigMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.support.CompositeItemProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 流程执行引擎
 * <p>
 * 根据流程定义的节点和连线，动态构建 Spring Batch Job 并执行。
 * 执行流程：Source Reader → Transform Processor(s) → Target Writer
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FlowExecutionEngine {

    private final NodeExecutorRegistry registry;
    private final EtlFlowMapper flowMapper;
    private final EtlFlowExecutionMapper executionMapper;
    private final EtlNodeConfigMapper nodeConfigMapper;
    private final ApplicationContext applicationContext;

    private final ConcurrentHashMap<Long, EtlFlowExecution> runningExecutions = new ConcurrentHashMap<>();

    /**
     * 异步执行流程
     */
    public CompletableFuture<Long> executeAsync(EtlFlow flow) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return executeSync(flow);
            } catch (Exception e) {
                log.error("流程执行失败: flowId={}", flow.getId(), e);
                throw new RuntimeException(e);
            }
        }, getExecutor());
    }

    /**
     * 同步执行流程
     */
    @SuppressWarnings("unchecked")
    public Long executeSync(EtlFlow flow) {
        log.info("开始执行流程: flowId={}, flowName={}", flow.getId(), flow.getFlowName());

        // 1. 创建执行记录
        EtlFlowExecution execution = new EtlFlowExecution();
        execution.setFlowId(flow.getId());
        execution.setFlowName(flow.getFlowName());
        execution.setExecStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        executionMapper.insert(execution);
        runningExecutions.put(execution.getId(), execution);

        // 2. 更新流程状态
        flow.setFlowStatus("RUNNING");
        flow.setLastExecTime(LocalDateTime.now());
        flowMapper.updateById(flow);

        try {
            // 3. 解析节点和连线
            List<EtlNodeConfig> nodes = parseNodes(flow);
            List<JSONObject> edges = parseEdges(flow);

            // 4. 拓扑排序确定执行顺序
            List<EtlNodeConfig> sortedNodes = topologicalSort(nodes, edges);

            // 5. 找到 Source 节点
            EtlNodeConfig sourceNode = sortedNodes.stream()
                    .filter(n -> "SOURCE".equals(n.getNodeType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("流程中没有数据源节点"));

            // 6. 找到 Target 节点
            EtlNodeConfig targetNode = sortedNodes.stream()
                    .filter(n -> "TARGET".equals(n.getNodeType()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("流程中没有目标节点"));

            // 7. 收集 Transform 节点
            List<EtlNodeConfig> transformNodes = sortedNodes.stream()
                    .filter(n -> "TRANSFORM".equals(n.getNodeType()))
                    .toList();

            // 8. 构建 Reader
            NodeExecutor sourceExecutor = registry.getExecutor(sourceNode.getNodeSubType(), "SOURCE");
            Map<String, Object> sourceConfig = parseConfig(sourceNode.getConfigJson());
            ItemReader<Map<String, Object>> reader = sourceExecutor.createReader(sourceConfig);

            // 9. 构建 Processor 链
            ItemProcessor<Map<String, Object>, Map<String, Object>> processor = buildProcessorChain(transformNodes);

            // 10. 构建 Writer
            NodeExecutor targetExecutor = registry.getExecutor(targetNode.getNodeSubType(), "TARGET");
            Map<String, Object> targetConfig = parseConfig(targetNode.getConfigJson());
            ItemWriter<Map<String, Object>> writer = targetExecutor.createWriter(targetConfig);

            // 11. 构建 Spring Batch Step（使用 Spring Batch 5.x API）
            JobRepository jobRepository = applicationContext.getBean(JobRepository.class);
            PlatformTransactionManager transactionManager = applicationContext.getBean(PlatformTransactionManager.class);

            Step step = new StepBuilder("flow-step-" + flow.getId(), jobRepository)
                    .<Map<String, Object>, Map<String, Object>>chunk(1000, transactionManager)
                    .reader(reader)
                    .processor(processor)
                    .writer(writer)
                    .build();

            // 12. 构建 Spring Batch Job
            Job job = new org.springframework.batch.core.job.builder.JobBuilder("flow-job-" + flow.getId(), jobRepository)
                    .incrementer(new RunIdIncrementer())
                    .start(step)
                    .build();

            // 14. 根据批处理真实结果更新执行记录（回填读写统计）
            JobLauncher jobLauncher = applicationContext.getBean(JobLauncher.class);
            org.springframework.batch.core.JobExecution batchExecution = jobLauncher.run(job, new JobParametersBuilder()
                    .addLong("flowId", flow.getId())
                    .addLong("executionId", execution.getId())
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters());

            StepExecution stepExecution = batchExecution.getStepExecutions().stream()
                    .findFirst().orElse(null);
            if (stepExecution != null) {
                execution.setReadCount(stepExecution.getReadCount());
                execution.setWriteCount(stepExecution.getWriteCount());
                execution.setSkipCount(stepExecution.getSkipCount());
                execution.setErrorCount(stepExecution.getFilterCount() > 0 ? stepExecution.getFilterCount() : 0L);
            }

            String batchStatus = String.valueOf(batchExecution.getStatus());
            if (!org.springframework.batch.core.BatchStatus.COMPLETED.equals(batchExecution.getStatus())) {
                // 批处理未成功完成：提取退出描述作为错误信息
                String exitDescription = batchExecution.getExitStatus() == null
                        ? batchStatus
                        : batchExecution.getExitStatus().getExitDescription();
                if (exitDescription == null || exitDescription.isBlank()) {
                    exitDescription = batchExecution.getExitStatus() == null ? batchStatus
                            : batchExecution.getExitStatus().getExitCode();
                }
                throw new IllegalStateException("流程执行失败: " + exitDescription);
            }

            execution.setExecStatus("COMPLETED");
            execution.setEndTime(LocalDateTime.now());
            executionMapper.updateById(execution);

            // 15. 更新流程状态
            flow.setFlowStatus("COMPLETED");
            flow.setLastExecStatus("COMPLETED");
            flowMapper.updateById(flow);

            log.info("流程执行完成: flowId={}, executionId={}", flow.getId(), execution.getId());
            return execution.getId();

        } catch (Exception e) {
            log.error("流程执行异常: flowId={}", flow.getId(), e);

            execution.setExecStatus("FAILED");
            execution.setEndTime(LocalDateTime.now());
            execution.setErrorMessage(e.getMessage());
            executionMapper.updateById(execution);

            flow.setFlowStatus("FAILED");
            flow.setLastExecStatus("FAILED");
            flowMapper.updateById(flow);

            throw new RuntimeException("流程执行失败: " + e.getMessage(), e);

        } finally {
            runningExecutions.remove(execution.getId());
        }
    }

    /**
     * 停止执行中的流程
     */
    public boolean stopExecution(Long executionId) {
        EtlFlowExecution execution = runningExecutions.get(executionId);
        if (execution == null) return false;
        execution.setExecStatus("STOPPED");
        execution.setEndTime(LocalDateTime.now());
        executionMapper.updateById(execution);
        runningExecutions.remove(executionId);
        return true;
    }

    public boolean isRunning(Long flowId) {
        return runningExecutions.values().stream()
                .anyMatch(e -> e.getFlowId().equals(flowId) && "RUNNING".equals(e.getExecStatus()));
    }

    private List<EtlNodeConfig> parseNodes(EtlFlow flow) {
        if (flow.getNodes() != null && !flow.getNodes().isEmpty()) return flow.getNodes();
        // 1) 旧版 node_config 表数据
        List<EtlNodeConfig> fromTable = nodeConfigMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<EtlNodeConfig>()
                        .eq(EtlNodeConfig::getFlowId, flow.getId()));
        if (!fromTable.isEmpty()) return fromTable;
        // 2) 画板保存的 vue-flow nodesJson（前端保存流程时仅写该字段，
        //    结构为 [{id, position, data:{nodeType,nodeSubType,label,config:{...}}}]）
        return parseNodesFromJson(flow.getNodesJson(), flow.getId());
    }

    /**
     * 解析前端画板（vue-flow）保存的节点 JSON 为节点配置。
     * 前端保存流程只提交 nodesJson，此处补齐"画板所见即执行所得"的解析链路。
     */
    private List<EtlNodeConfig> parseNodesFromJson(String nodesJson, Long flowId) {
        if (nodesJson == null || nodesJson.isBlank()) return List.of();
        com.alibaba.fastjson.JSONArray arr = com.alibaba.fastjson.JSON.parseArray(nodesJson);
        List<EtlNodeConfig> result = new ArrayList<>(arr.size());
        for (int i = 0; i < arr.size(); i++) {
            com.alibaba.fastjson.JSONObject node = arr.getJSONObject(i);
            com.alibaba.fastjson.JSONObject data = node.getJSONObject("data");
            if (data == null) continue;
            EtlNodeConfig config = new EtlNodeConfig();
            config.setFlowId(flowId);
            config.setNodeId(node.getString("id"));
            config.setNodeType(data.getString("nodeType"));
            config.setNodeSubType(data.getString("nodeSubType"));
            config.setNodeName(data.getString("label"));
            com.alibaba.fastjson.JSONObject cfg = data.getJSONObject("config");
            config.setConfigJson(cfg == null ? "{}" : cfg.toJSONString());
            result.add(config);
        }
        return result;
    }

    private List<JSONObject> parseEdges(EtlFlow flow) {
        if (flow.getEdgesJson() == null || flow.getEdgesJson().isBlank()) return List.of();
        return JSON.parseArray(flow.getEdgesJson(), JSONObject.class);
    }

    private List<EtlNodeConfig> topologicalSort(List<EtlNodeConfig> nodes, List<JSONObject> edges) {
        Map<String, EtlNodeConfig> nodeMap = new HashMap<>();
        for (EtlNodeConfig node : nodes) nodeMap.put(node.getNodeId(), node);

        Map<String, List<String>> adjacency = new HashMap<>();
        Map<String, Integer> inDegree = new HashMap<>();
        for (EtlNodeConfig node : nodes) { adjacency.put(node.getNodeId(), new ArrayList<>()); inDegree.put(node.getNodeId(), 0); }
        for (JSONObject edge : edges) {
            String source = edge.getString("source");
            String target = edge.getString("target");
            if (source != null && target != null && adjacency.containsKey(source)) {
                adjacency.get(source).add(target);
                inDegree.merge(target, 1, Integer::sum);
            }
        }

        Queue<String> queue = new LinkedList<>();
        for (var entry : inDegree.entrySet()) { if (entry.getValue() == 0) queue.add(entry.getKey()); }

        List<EtlNodeConfig> sorted = new ArrayList<>();
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

    @SuppressWarnings("unchecked")
    private ItemProcessor<Map<String, Object>, Map<String, Object>> buildProcessorChain(List<EtlNodeConfig> transformNodes) {
        if (transformNodes.isEmpty()) return item -> item;

        List<ItemProcessor<Map<String, Object>, Map<String, Object>>> processors = new ArrayList<>();
        for (EtlNodeConfig node : transformNodes) {
            NodeExecutor executor = registry.getExecutor(node.getNodeSubType(), "TRANSFORM");
            Map<String, Object> config = parseConfig(node.getConfigJson());
            processors.add(executor.createProcessor(config));
        }

        if (processors.size() == 1) return processors.get(0);

        CompositeItemProcessor<Map<String, Object>, Map<String, Object>> composite = new CompositeItemProcessor<>();
        composite.setDelegates(processors);
        return composite;
    }

    private Map<String, Object> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank()) return Map.of();
        return JSON.parseObject(configJson, Map.class);
    }

    private java.util.concurrent.Executor getExecutor() {
        try {
            return applicationContext.getBean("etlExecutor", java.util.concurrent.Executor.class);
        } catch (Exception e) {
            ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
            executor.setCorePoolSize(4);
            executor.setMaxPoolSize(8);
            executor.setQueueCapacity(100);
            executor.setThreadNamePrefix("flow-exec-");
            executor.initialize();
            return executor;
        }
    }
}
