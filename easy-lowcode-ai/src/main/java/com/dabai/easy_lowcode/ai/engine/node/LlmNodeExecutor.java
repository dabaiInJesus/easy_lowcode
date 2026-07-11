package com.dabai.easy_lowcode.ai.engine.node;

import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import com.dabai.easy_lowcode.ai.enums.AiProvider;
import com.dabai.easy_lowcode.ai.factory.AiServiceFactory;
import com.dabai.easy_lowcode.ai.dto.ChatRequest;
import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LLM 节点执行器
 * <p>
 * 调用 AI 模型生成文本
 * 配置: { "provider": "openai", "model": "gpt-4", "prompt": "你是...", "maxTokens": 2000 }
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmNodeExecutor implements WorkflowNodeExecutor {

    private final AiServiceFactory aiServiceFactory;

    @Override
    public String getNodeType() {
        return "llm";
    }

    @Override
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        String providerCode = (String) config.getOrDefault("provider", "openai");
        String model = (String) config.get("model");
        String promptTemplate = (String) config.get("prompt");
        int maxTokens = config.containsKey("maxTokens")
                ? ((Number) config.get("maxTokens")).intValue() : 2000;

        // 替换模板变量: {{variable}}
        String prompt = replaceVariables(promptTemplate, input, context);

        log.info("LLM节点执行: provider={}, model={}", providerCode, model);

        // 构建请求
        ChatRequest request = new ChatRequest();
        request.setMessage(prompt);
        request.setMaxTokens(maxTokens);
        if (model != null) {
            request.setModel(model);
        }

        // 调用 AI 服务
        AiProvider provider = AiProvider.fromCode(providerCode);
        ChatResponse response = aiServiceFactory.getService(provider).chat(request);

        // 构建输出
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("text", response.getContent());
        output.put("model", response.getModel());
        output.put("provider", providerCode);
        if (response.getUsage() != null) {
            output.put("tokenUsage", response.getUsage());
        }

        log.info("LLM节点完成: output_length={}", response.getContent() != null ? response.getContent().length() : 0);
        return output;
    }

    /**
     * 替换模板中的变量 {{variable}}
     */
    private String replaceVariables(String template, Map<String, Object> input, Map<String, Object> context) {
        if (template == null) return "";
        String result = template;

        // 优先从 input 中取值，其次从 context 中取值
        Map<String, Object> allVars = new java.util.LinkedHashMap<>();
        if (context != null) allVars.putAll(context);
        if (input != null) allVars.putAll(input);

        for (Map.Entry<String, Object> entry : allVars.entrySet()) {
            String placeholder = "{{" + entry.getKey() + "}}";
            String value = entry.getValue() != null ? entry.getValue().toString() : "";
            result = result.replace(placeholder, value);
        }

        return result;
    }
}
