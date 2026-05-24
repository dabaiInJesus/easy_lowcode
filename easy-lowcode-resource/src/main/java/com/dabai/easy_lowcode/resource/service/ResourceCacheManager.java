package com.dabai.easy_lowcode.resource.service;

import com.dabai.easy_lowcode.resource.model.FieldConfig;

import java.util.List;
import java.util.Set;

/**
 * 资源缓存管理器
 * 负责缓存表列名和字段配置信息，减少重复查询
 */
public interface ResourceCacheManager {

    /**
     * 获取缓存的允许列集合
     */
    Set<String> getCachedAllowedColumns(Long resourceId);

    /**
     * 缓存允许列集合
     */
    void cacheAllowedColumns(Long resourceId, Set<String> columns);

    /**
     * 获取缓存的字段配置列表
     */
    List<FieldConfig> getCachedFieldConfigs(Long resourceId);

    /**
     * 缓存字段配置列表
     */
    void cacheFieldConfigs(Long resourceId, List<FieldConfig> fields);

    /**
     * 清除指定资源的所有缓存
     */
    void evict(Long resourceId);
}
