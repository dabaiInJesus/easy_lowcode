package com.dabai.easy_lowcode.ai.engine.node;

import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 条件分支节点执行器
 * <p>
 * 根据条件表达式决定走 true 还是 false 分支
 * 配置: { "variable": "score", "operator": "gt", "value": 60 }
 * 输出: { "branch": "true" } 或 { "branch": "false" }
 */
@Slf4j
@Component
public class ConditionNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String getNodeType() {
        return "condition";
    }

    @Override
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        String variable = (String) config.get("variable");
        String operator = (String) config.getOrDefault("operator", "eq");
        Object expected = config.get("value");

        // 获取变量值（优先从 input，其次从 context）
        Object actual = (input != null && input.containsKey(variable)) ? input.get(variable)
                : (context != null ? context.get(variable) : null);

        log.info("条件节点执行: variable={}, operator={}, actual={}, expected={}", variable, operator, actual, expected);

        boolean result = evaluateCondition(actual, operator, expected);

        Map<String, Object> output = new LinkedHashMap<>();
        output.put("branch", result ? "true" : "false");
        output.put("condition", result);
        output.put("variable", variable);
        output.put("actualValue", actual);

        log.info("条件节点结果: branch={}", result ? "true" : "false");
        return output;
    }

    private boolean evaluateCondition(Object actual, String operator, Object expected) {
        if (actual == null) return "isEmpty".equals(operator);

        String av = actual.toString();
        String ev = expected != null ? expected.toString() : "";

        return switch (operator) {
            case "eq" -> av.equals(ev);
            case "neq" -> !av.equals(ev);
            case "gt" -> Double.parseDouble(av) > Double.parseDouble(ev);
            case "gte" -> Double.parseDouble(av) >= Double.parseDouble(ev);
            case "lt" -> Double.parseDouble(av) < Double.parseDouble(ev);
            case "lte" -> Double.parseDouble(av) <= Double.parseDouble(ev);
            case "contains" -> av.contains(ev);
            case "isEmpty" -> av.isEmpty();
            case "isNotEmpty" -> !av.isEmpty();
            case "isTrue" -> "true".equalsIgnoreCase(av) || "1".equals(av);
            case "isFalse" -> "false".equalsIgnoreCase(av) || "0".equals(av);
            default -> true;
        };
    }
}
