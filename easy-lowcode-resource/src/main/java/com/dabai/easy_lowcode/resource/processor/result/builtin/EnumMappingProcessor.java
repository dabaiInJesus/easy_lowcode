package com.dabai.easy_lowcode.resource.processor.result.builtin;

import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.result.ResultProcessor;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnumMappingProcessor implements ResultProcessor, ConfigurableProcessor {

    private Map<String, Map<String, String>> mappings = new LinkedHashMap<>();

    @Override
    public String getType() { return "enumMapping"; }

    @Override
    public int getOrder() { return 40; }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config != null && config.get("mappings") instanceof Map) {
            Map<String, Object> raw = (Map<String, Object>) config.get("mappings");
            for (Map.Entry<String, Object> entry : raw.entrySet()) {
                if (entry.getValue() instanceof Map) {
                    mappings.put(entry.getKey(), (Map<String, String>) entry.getValue());
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> input, ProcessorContext context) {
        for (Map<String, Object> row : input) {
            for (Map.Entry<String, Map<String, String>> mapping : mappings.entrySet()) {
                String field = mapping.getKey();
                Map<String, String> valueMap = mapping.getValue();
                Object rawVal = row.get(field);
                if (rawVal != null && valueMap != null) {
                    String mapped = valueMap.get(rawVal.toString());
                    if (mapped != null) {
                        row.put(field, mapped);
                    }
                }
            }
        }
        return input;
    }
}
