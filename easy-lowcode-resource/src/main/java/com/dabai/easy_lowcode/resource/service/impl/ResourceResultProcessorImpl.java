package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.resource.model.ConfigJson;
import com.dabai.easy_lowcode.resource.model.ConfigParser;
import com.dabai.easy_lowcode.resource.model.DisplayFieldSetting;
import com.dabai.easy_lowcode.resource.model.DisplaySettings;
import com.dabai.easy_lowcode.resource.processor.ProcessorChain;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.ProcessorRegistry;
import com.dabai.easy_lowcode.resource.service.ResourceResultProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * 结果处理器服务实现
 * 执行配置的结果处理器链，并应用显示设置（日期格式化、枚举映射、空值隐藏）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceResultProcessorImpl implements ResourceResultProcessor {

    private final TableResourceMapper tableResourceMapper;
    private final ProcessorRegistry processorRegistry;

    @Override
    public void applyResultPipeline(String resourceCode, List<Map<String, Object>> records) {
        TableResource tableResource = tableResourceMapper.selectByResourceCode(resourceCode);
        if (tableResource == null || tableResource.getConfigJson() == null) {
            return;
        }

        ConfigJson config = ConfigParser.parse(tableResource.getConfigJson());

        applyProcessorChain(tableResource, config, records, resourceCode);
        applyDisplaySettings(config, records);
    }

    @Override
    public int countKeywordMatches(Map<String, Object> record, String keyword) {
        int count = 0;
        for (Object value : record.values()) {
            if (value != null && value.toString().toLowerCase().contains(keyword)) {
                count++;
            }
        }
        return count;
    }

    private void applyProcessorChain(TableResource tableResource, ConfigJson config, List<Map<String, Object>> records, String resourceCode) {
        if (config.getResultProcessors() == null || config.getResultProcessors().isEmpty()) {
            return;
        }

        ProcessorChain<List<Map<String, Object>>> chain = processorRegistry.buildResultChain(config.getResultProcessors());
        ProcessorContext context = ProcessorContext.builder()
                .tableResource(tableResource)
                .build();
        context.getExtendedProps().put("resourceCode", resourceCode);
        chain.execute(records, context);
    }

    private void applyDisplaySettings(ConfigJson config, List<Map<String, Object>> records) {
        DisplaySettings ds = config.getDisplaySettings();
        if (ds == null) {
            return;
        }

        for (Map<String, Object> record : records) {
            for (Map.Entry<String, Object> entry : record.entrySet()) {
                DisplayFieldSetting dfs = ds.getFields().get(entry.getKey().toLowerCase());
                if (dfs == null) {
                    continue;
                }

                applyDateFormat(dfs, entry);
                applyEnumMapping(dfs, entry);
            }
            record.values().removeIf(v -> v == null);
        }
    }

    private void applyDateFormat(DisplayFieldSetting dfs, Map.Entry<String, Object> entry) {
        if (dfs.getFormat() != null && entry.getValue() instanceof Date) {
            entry.setValue(new SimpleDateFormat(dfs.getFormat()).format((Date) entry.getValue()));
        }
    }

    private void applyEnumMapping(DisplayFieldSetting dfs, Map.Entry<String, Object> entry) {
        if (dfs.getEnumMapping() != null && entry.getValue() != null) {
            String mapped = dfs.getEnumMapping().get(entry.getValue().toString());
            if (mapped != null) {
                entry.setValue(mapped);
            }
        }
    }
}
