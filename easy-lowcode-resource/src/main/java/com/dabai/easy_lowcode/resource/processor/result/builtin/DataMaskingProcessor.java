package com.dabai.easy_lowcode.resource.processor.result.builtin;

import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.result.ResultProcessor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class DataMaskingProcessor implements ResultProcessor, ConfigurableProcessor {

    private List<MaskingRule> rules = new ArrayList<>();

    @Override
    public String getType() { return "dataMasking"; }

    @Override
    public int getOrder() { return 30; }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config != null && config.get("rules") instanceof List) {
            this.rules = new ArrayList<>();
            for (Object obj : (List<Object>) config.get("rules")) {
                if (obj instanceof Map) {
                    Map<String, String> ruleMap = (Map<String, String>) obj;
                    MaskingRule rule = new MaskingRule();
                    rule.setField(ruleMap.get("field"));
                    rule.setPattern(ruleMap.get("pattern"));
                    rule.setReplacement(ruleMap.get("replacement"));
                    if (rule.getField() != null) {
                        this.rules.add(rule);
                    }
                }
            }
        }
    }

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> input, ProcessorContext context) {
        for (Map<String, Object> row : input) {
            for (MaskingRule rule : rules) {
                Object val = row.get(rule.getField());
                if (val instanceof String) {
                    String masked = ((String) val).replaceAll(rule.getPattern(), rule.getReplacement());
                    row.put(rule.getField(), masked);
                }
            }
        }
        return input;
    }

    private static class MaskingRule {
        private String field;
        private String pattern;
        private String replacement;

        public String getField() { return field; }
        public void setField(String field) { this.field = field; }
        public String getPattern() { return pattern; }
        public void setPattern(String pattern) { this.pattern = pattern; }
        public String getReplacement() { return replacement; }
        public void setReplacement(String replacement) { this.replacement = replacement; }
    }
}
