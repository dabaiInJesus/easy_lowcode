package com.dabai.easy_lowcode.etl.service;

import com.dabai.easy_lowcode.etl.model.TransformRule;

import java.util.List;
import java.util.Map;

/**
 * 转换规则处理器
 * 负责解析字段映射和转换规则，对数据行应用转换逻辑
 */
public interface TransformRuleProcessor {

    /**
     * 解析字段映射配置
     */
    Map<String, String> parseFieldMapping(String fieldMappingJson, List<String> sourceColumns);

    /**
     * 解析转换规则列表
     */
    List<TransformRule> parseTransformRules(String transformRulesJson);

    /**
     * 对单个值应用所有匹配的转换规则
     */
    Object applyTransforms(String sourceField, String targetField, Object value, List<TransformRule> rules);
}
