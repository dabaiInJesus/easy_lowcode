package com.dabai.easy_lowcode.resource.processor.result.builtin;

import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.result.ResultProcessor;

import java.util.*;
import java.util.stream.Collectors;

public class FieldFilterProcessor implements ResultProcessor, ConfigurableProcessor {

    private String mode = "WHITELIST";
    private Set<String> fields = new HashSet<>();

    @Override
    public String getType() { return "fieldFilter"; }

    @Override
    public int getOrder() { return 10; }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config != null) {
            if (config.get("mode") != null) this.mode = (String) config.get("mode");
            if (config.get("fields") instanceof List) {
                this.fields = ((List<String>) config.get("fields")).stream()
                        .map(String::toLowerCase).collect(Collectors.toSet());
            }
        }
    }

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> input, ProcessorContext context) {
        if (fields.isEmpty()) return input;
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> row : input) {
            Map<String, Object> filtered = new LinkedHashMap<>();
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                String key = entry.getKey().toLowerCase();
                boolean inList = fields.contains(key);
                if ("WHITELIST".equalsIgnoreCase(mode) && inList) {
                    filtered.put(entry.getKey(), entry.getValue());
                } else if ("BLACKLIST".equalsIgnoreCase(mode) && !inList) {
                    filtered.put(entry.getKey(), entry.getValue());
                }
            }
            result.add(filtered);
        }
        return result;
    }
}
