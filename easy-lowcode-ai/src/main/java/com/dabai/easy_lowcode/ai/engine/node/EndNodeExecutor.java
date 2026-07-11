package com.dabai.easy_lowcode.ai.engine.node;

import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 结束节点执行器
 * <p>
 * 输出最终结果
 */
@Component
public class EndNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String getNodeType() {
        return "end";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        // 结束节点直接透传上游输出
        Map<String, Object> output = new LinkedHashMap<>();

        if (config != null && config.containsKey("outputFields")) {
            // 如果配置了输出字段列表，只保留指定的字段
            java.util.List<String> outputFields = (java.util.List<String>) config.get("outputFields");
            Map<String, Object> source = input != null ? input : Map.of();
            for (String field : outputFields) {
                if (source.containsKey(field)) {
                    output.put(field, source.get(field));
                }
            }
        } else {
            // 否则透传所有输入
            if (input != null) output.putAll(input);
        }

        return output;
    }
}
