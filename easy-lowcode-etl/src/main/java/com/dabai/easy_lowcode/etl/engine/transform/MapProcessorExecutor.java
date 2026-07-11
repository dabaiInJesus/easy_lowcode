package com.dabai.easy_lowcode.etl.engine.transform;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 字段映射转换节点执行器
 * <p>
 * 将源字段映射到目标字段，支持重命名、类型转换、默认值。
 * 配置示例: { "mappings": [{"source": "name", "target": "user_name"}, {"source": "age", "target": "user_age", "defaultValue": 0}] }
 */
@Slf4j
@Component
public class MapProcessorExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "map"; }

    @Override
    public String getCategory() { return "TRANSFORM"; }

    @Override
    @SuppressWarnings("unchecked")
    public ItemProcessor<Map<String, Object>, Map<String, Object>> createProcessor(Map<String, Object> config) {
        List<Map<String, Object>> mappings = (List<Map<String, Object>>) config.getOrDefault("mappings", List.of());

        log.info("Map Processor: {} field mappings", mappings.size());

        return item -> {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map<String, Object> mapping : mappings) {
                String source = (String) mapping.get("source");
                String target = (String) mapping.getOrDefault("target", source);
                Object defaultValue = mapping.get("defaultValue");

                Object value = item.get(source);
                if (value == null && defaultValue != null) {
                    value = defaultValue;
                }
                result.put(target, value);
            }
            return result;
        };
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "mappings": {
                    "type": "array",
                    "label": "字段映射",
                    "itemSchema": {
                      "source": { "type": "string", "label": "源字段" },
                      "target": { "type": "string", "label": "目标字段" },
                      "defaultValue": { "type": "string", "label": "默认值" }
                    }
                  }
                }""";
    }
}
