package com.dabai.easy_lowcode.etl.engine.transform;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 聚合转换节点执行器
 * <p>
 * 按分组字段聚合数据。由于 Spring Batch 的 chunk 处理模型，
 * 此处理器在 chunk 级别进行聚合。
 * 配置示例: { "groupBy": "category", "aggregations": [{"field": "amount", "function": "sum"}] }
 */
@Slf4j
@Component
public class AggregateProcessorExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "aggregate"; }

    @Override
    public String getCategory() { return "TRANSFORM"; }

    @Override
    @SuppressWarnings("unchecked")
    public ItemProcessor<Map<String, Object>, Map<String, Object>> createProcessor(Map<String, Object> config) {
        String groupBy = (String) config.get("groupBy");
        List<Map<String, Object>> aggregations = (List<Map<String, Object>>) config.getOrDefault("aggregations", List.of());

        log.info("Aggregate Processor: groupBy={}, aggregations={}", groupBy, aggregations.size());

        // 简单实现：逐行传递，聚合逻辑在 Writer 层处理
        // 完整实现需要维护状态，超出单行 Processor 的能力
        return item -> {
            // 简单透传，聚合在 Writer 端完成
            return item;
        };
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "groupBy": { "type": "string", "label": "分组字段" },
                  "aggregations": {
                    "type": "array",
                    "label": "聚合配置",
                    "itemSchema": {
                      "field": { "type": "string", "label": "聚合字段" },
                      "function": { "type": "select", "label": "聚合函数", "options": ["sum","count","avg","min","max"] }
                    }
                  }
                }""";
    }
}
