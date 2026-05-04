package com.dabai.easy_lowcode.collector.service;

import java.util.List;
import java.util.Map;

/**
 * 数据预览服务接口
 */
public interface DataPreviewService {
    
    /**
     * 预览表数据
     * 
     * @param resourceId 资源ID
     * @param limit 限制条数
     * @return 数据列表
     */
    List<Map<String, Object>> previewTableData(Long resourceId, int limit);
}
