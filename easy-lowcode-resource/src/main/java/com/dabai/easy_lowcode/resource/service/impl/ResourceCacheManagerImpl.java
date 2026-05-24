package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.common.util.SimpleCache;
import com.dabai.easy_lowcode.resource.model.FieldConfig;
import com.dabai.easy_lowcode.resource.service.ResourceCacheManager;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 资源缓存管理器实现
 * 使用 LRU 缓存策略，避免无界内存增长
 */
@Component
public class ResourceCacheManagerImpl implements ResourceCacheManager {

    private final SimpleCache<Long, Set<String>> columnCache = new SimpleCache<>(100);
    private final SimpleCache<Long, List<FieldConfig>> fieldConfigCache = new SimpleCache<>(100);

    @Override
    public Set<String> getCachedAllowedColumns(Long resourceId) {
        return columnCache.get(resourceId);
    }

    @Override
    public void cacheAllowedColumns(Long resourceId, Set<String> columns) {
        columnCache.put(resourceId, columns);
    }

    @Override
    public List<FieldConfig> getCachedFieldConfigs(Long resourceId) {
        return fieldConfigCache.get(resourceId);
    }

    @Override
    public void cacheFieldConfigs(Long resourceId, List<FieldConfig> fields) {
        fieldConfigCache.put(resourceId, fields);
    }

    @Override
    public void evict(Long resourceId) {
        columnCache.remove(resourceId);
        fieldConfigCache.remove(resourceId);
    }
}
