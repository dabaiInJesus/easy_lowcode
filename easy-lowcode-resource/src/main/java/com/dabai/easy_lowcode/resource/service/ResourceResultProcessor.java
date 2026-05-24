package com.dabai.easy_lowcode.resource.service;

import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.resource.model.DisplayFieldSetting;
import com.dabai.easy_lowcode.resource.model.DisplaySettings;

import java.util.List;
import java.util.Map;

/**
 * 结果处理器服务
 * 负责执行结果处理器链和应用显示设置（日期格式化、枚举映射等）
 */
public interface ResourceResultProcessor {

    /**
     * 应用结果处理管道（处理器链 + 显示设置）
     */
    void applyResultPipeline(String resourceCode, List<Map<String, Object>> records);

    /**
     * 计算记录与关键词的匹配度（用于全文检索排序）
     */
    int countKeywordMatches(Map<String, Object> record, String keyword);
}
