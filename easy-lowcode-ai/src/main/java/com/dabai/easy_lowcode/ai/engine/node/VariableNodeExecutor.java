package com.dabai.easy_lowcode.ai.engine.node;

import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 变量节点执行器
 * <p>
 * 变量赋值/转换
 * 配置: { "assignments": { "outputVar": "{{inputVar}}", "constant": "hello" } }
 */
@Slf4j
@Component
public class VariableNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String getNodeType() {
        return "variable";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        Map<String, Object> assignments = (Map<String, Object>) config.getOrDefault("assignments", Map.of());

        Map<String, Object> output = new LinkedHashMap<>();

        // 合并所有变量源
        Map<String, Object> allVars = new LinkedHashMap<>();
        if (context != null) allVars.putAll(context);
        if (input != null) allVars.putAll(input);

        for (Map.Entry<String, Object> entry : assignments.entrySet()) {
            String targetVar = entry.getKey();
            Object source = entry.getValue();

            if (source instanceof String) {
                String sourceStr = (String) source;
                // 替换模板变量
                String value = sourceStr;
                for (Map.Entry<String, Object> var : allVars.entrySet()) {
                    value = value.replace("{{" + var.getKey() + "}}",
                            var.getValue() != null ? var.getValue().toString() : "");
                }
                output.put(targetVar, value);
            } else {
                output.put(targetVar, source);
            }
        }

        log.info("变量节点完成: 输出 {} 个变量", output.size());
        return output;
    }
}
