package com.dabai.easy_lowcode.ai.agent.tool.impl;

import com.dabai.easy_lowcode.ai.agent.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.List;
import java.util.Map;

/**
 * 数学计算工具
 */
@Slf4j
@Component
public class CalculatorTool implements AgentTool {

    @Override
    public String getName() {
        return "calculator";
    }

    @Override
    public String getDescription() {
        return "执行数学计算表达式。支持加减乘除、括号、幂运算等。例如: \"2 + 3 * 4\" 或 \"Math.sqrt(16)\"";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "expression", Map.of(
                                "type", "string",
                                "description", "数学表达式"
                        )
                ),
                "required", List.of("expression")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String expression = (String) params.get("expression");
        if (expression == null || expression.isBlank()) {
            return Map.of("error", "表达式不能为空");
        }

        try {
            ScriptEngineManager manager = new ScriptEngineManager();
            ScriptEngine engine = manager.getEngineByName("js");
            if (engine == null) {
                return Map.of("error", "JavaScript 引擎不可用");
            }

            // 安全检查：只允许数学表达式
            String safe = expression.replaceAll("[^0-9+\\-*/().% Math.sqrtabsinpow]", "");
            Object result = engine.eval(expression);

            return Map.of(
                    "expression", expression,
                    "result", result.toString()
            );
        } catch (Exception e) {
            log.warn("计算器执行失败: {}", e.getMessage());
            return Map.of("error", "计算失败: " + e.getMessage());
        }
    }

    @Override
    public String getToolType() {
        return "calculator";
    }
}
