package com.dabai.easy_lowcode.etl.service.impl;

import com.dabai.easy_lowcode.etl.model.TransformRule;
import com.dabai.easy_lowcode.etl.service.TransformRuleProcessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 转换规则处理器实现
 * 支持 UPPER/LOWER/TRIM/DEFAULT/CONCAT/SUBSTRING/DATE_FORMAT 等转换类型
 */
@Slf4j
@Service
public class TransformRuleProcessorImpl implements TransformRuleProcessor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Map<String, String> parseFieldMapping(String fieldMappingJson, List<String> sourceColumns) {
        Map<String, String> map = new LinkedHashMap<>();
        if (fieldMappingJson != null && !fieldMappingJson.isEmpty()) {
            try {
                List<Map<String, String>> mappings = objectMapper.readValue(fieldMappingJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                for (Map<String, String> m : mappings) {
                    map.put(m.get("source"), m.get("target"));
                }
            } catch (Exception e) {
                log.warn("解析字段映射失败，使用全量字段映射: {}", e.getMessage());
                for (String col : sourceColumns) {
                    map.put(col, col);
                }
            }
        } else {
            for (String col : sourceColumns) {
                map.put(col, col);
            }
        }
        return map;
    }

    @Override
    public List<TransformRule> parseTransformRules(String transformRulesJson) {
        List<TransformRule> rules = new ArrayList<>();
        if (transformRulesJson == null || transformRulesJson.isBlank()) {
            return rules;
        }
        try {
            rules = objectMapper.readValue(transformRulesJson, new TypeReference<List<TransformRule>>() {});
        } catch (Exception e) {
            log.warn("解析转换规则失败: {}", e.getMessage());
        }
        return rules;
    }

    @Override
    public Object applyTransforms(String sourceField, String targetField, Object value, List<TransformRule> rules) {
        for (TransformRule rule : rules) {
            boolean matchesSource = rule.getSourceField() != null && rule.getSourceField().equals(sourceField);
            boolean matchesTarget = rule.getTargetField() != null && rule.getTargetField().equals(targetField);
            if (!matchesSource && !matchesTarget) {
                continue;
            }
            if (rule.getTransformType() == null || "NONE".equals(rule.getTransformType())) {
                continue;
            }

            switch (rule.getTransformType().toUpperCase()) {
                case "UPPER":
                    if (value instanceof String) return ((String) value).toUpperCase();
                    break;
                case "LOWER":
                    if (value instanceof String) return ((String) value).toLowerCase();
                    break;
                case "TRIM":
                    if (value instanceof String) return ((String) value).trim();
                    break;
                case "DEFAULT":
                    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                        return rule.getDefaultValue();
                    }
                    break;
                case "CONCAT":
                    if (rule.getExpression() != null) {
                        return rule.getExpression()
                                .replace("${value}", value != null ? value.toString() : "");
                    }
                    break;
                case "SUBSTRING":
                    if (value instanceof String && rule.getExpression() != null) {
                        return applySubstring((String) value, rule.getExpression());
                    }
                    break;
                case "DATE_FORMAT":
                    if (value instanceof Date && rule.getExpression() != null) {
                        return applyDateFormat((Date) value, rule.getExpression());
                    }
                    break;
            }
        }
        return value;
    }

    private String applySubstring(String value, String expression) {
        String[] parts = expression.split(",");
        int start = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0;
        int len = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : value.length();
        return value.substring(Math.min(start, value.length()), Math.min(start + len, value.length()));
    }

    private String applyDateFormat(Date value, String expression) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat(expression);
            return sdf.format(value);
        } catch (Exception e) {
            log.warn("日期格式化失败: {}", e.getMessage());
            return value.toString();
        }
    }
}
