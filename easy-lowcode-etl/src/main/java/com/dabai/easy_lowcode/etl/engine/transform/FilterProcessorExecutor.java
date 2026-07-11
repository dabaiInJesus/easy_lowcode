package com.dabai.easy_lowcode.etl.engine.transform;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 过滤转换节点执行器
 * <p>
 * 根据条件过滤数据行，条件不满足的行被丢弃。
 * 配置示例: { "field": "age", "operator": "gt", "value": 18 }
 */
@Slf4j
@Component
public class FilterProcessorExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "filter"; }

    @Override
    public String getCategory() { return "TRANSFORM"; }

    @Override
    public ItemProcessor<Map<String, Object>, Map<String, Object>> createProcessor(Map<String, Object> config) {
        String field = (String) config.get("field");
        String operator = (String) config.getOrDefault("operator", "eq");
        Object value = config.get("value");

        log.info("Filter Processor: field={}, operator={}, value={}", field, operator, value);

        return item -> {
            Object fieldValue = item.get(field);
            if (evaluateCondition(fieldValue, operator, value)) {
                return item;
            }
            return null; // 过滤掉
        };
    }

    private boolean evaluateCondition(Object fieldValue, String operator, Object expected) {
        if (fieldValue == null) return false;

        String sv = fieldValue.toString();
        String ev = expected != null ? expected.toString() : "";

        return switch (operator) {
            case "eq" -> sv.equals(ev);
            case "neq" -> !sv.equals(ev);
            case "gt" -> Double.parseDouble(sv) > Double.parseDouble(ev);
            case "gte" -> Double.parseDouble(sv) >= Double.parseDouble(ev);
            case "lt" -> Double.parseDouble(sv) < Double.parseDouble(ev);
            case "lte" -> Double.parseDouble(sv) <= Double.parseDouble(ev);
            case "contains" -> sv.contains(ev);
            case "startsWith" -> sv.startsWith(ev);
            case "endsWith" -> sv.endsWith(ev);
            case "isEmpty" -> sv.isEmpty();
            case "isNotEmpty" -> !sv.isEmpty();
            default -> true;
        };
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "field": { "type": "string", "label": "过滤字段" },
                  "operator": { "type": "select", "label": "操作符", "options": ["eq","neq","gt","gte","lt","lte","contains","startsWith","endsWith","isEmpty","isNotEmpty"] },
                  "value": { "type": "string", "label": "比较值" }
                }""";
    }
}
