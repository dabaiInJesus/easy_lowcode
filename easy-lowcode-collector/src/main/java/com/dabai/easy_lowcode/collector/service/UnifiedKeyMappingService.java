package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.collector.entity.UnifiedKeyMapping;

import java.util.List;
import java.util.Map;

public interface UnifiedKeyMappingService extends IService<UnifiedKeyMapping> {

    List<UnifiedKeyMapping> getDistinctKeys();

    List<UnifiedKeyMapping> getMappingsByKey(String unifiedKey);

    List<UnifiedKeyMapping> getMappingsByResourceCode(String resourceCode);

    /**
     * 自动检测映射建议
     * @param unifiedKey 统一Key
     * @param displayName 显示名
     * @return 建议的映射列表，需要人工确认
     */
    List<Map<String, Object>> suggestMappings(String unifiedKey, String displayName);

    /**
     * 批量保存映射（人工确认后）
     */
    void batchSave(List<UnifiedKeyMapping> mappings);
}
