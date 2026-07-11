package com.dabai.easy_lowcode.ai.engine.node;

import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.Bindings;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 代码节点执行器
 * <p>
 * 执行 JavaScript 代码片段
 * 配置: { "language": "javascript", "code": "return input.name.toUpperCase();" }
 */
@Slf4j
@Component
public class CodeNodeExecutor implements WorkflowNodeExecutor {

    @Override
    public String getNodeType() {
        return "code";
    }

    @Override
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        String language = (String) config.getOrDefault("language", "javascript");
        String code = (String) config.get("code");

        log.info("代码节点执行: language={}", language);

        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName(language);
            if (engine == null) {
                throw new RuntimeException("不支持的脚本语言: " + language);
            }

            // 绑定变量
            Bindings bindings = engine.createBindings();
            bindings.put("input", input != null ? input : Map.of());
            bindings.put("context", context != null ? context : Map.of());

            // 执行代码
            Object result = engine.eval(code, bindings);

            Map<String, Object> output = new LinkedHashMap<>();
            if (result instanceof Map) {
                output.putAll((Map<String, Object>) result);
            } else {
                output.put("result", result);
            }

            log.info("代码节点完成: result={}", result);
            return output;

        } catch (Exception e) {
            log.error("代码节点执行失败", e);
            throw new RuntimeException("代码执行失败: " + e.getMessage(), e);
        }
    }
}
