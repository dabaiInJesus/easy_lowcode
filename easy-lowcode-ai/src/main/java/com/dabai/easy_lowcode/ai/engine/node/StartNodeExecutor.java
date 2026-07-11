package com.dabai.easy_lowcode.ai.engine.node;

import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 开始节点执行器
 * <p>
 * 将输入参数传递到下游节点
 */
@Component
public class StartNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String getNodeType() {
        return "start";
    }

    @Override
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        // 开始节点直接透传输入变量
        Map<String, Object> output = new LinkedHashMap<>(input != null ? input : Map.of());

        // 如果配置中定义了默认值，填充缺失的变量
        if (config != null && config.containsKey("defaults")) {
            @SuppressWarnings("unchecked")
            Map<String, Object> defaults = (Map<String, Object>) config.get("defaults");
            for (Map.Entry<String, Object> entry : defaults.entrySet()) {
                output.putIfAbsent(entry.getKey(), entry.getValue());
            }
        }

        return output;
    }
}
