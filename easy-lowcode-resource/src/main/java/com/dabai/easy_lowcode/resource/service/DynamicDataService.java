package com.dabai.easy_lowcode.resource.service;

import java.util.List;
import java.util.Map;

/**
 * 动态数据查询服务接口
 */
public interface DynamicDataService {
    
    /**
     * 根据资源编码查询数据
     * 
     * @param resourceCode 资源编码
     * @param params 查询参数
     * @return 数据列表
     */
    List<Map<String, Object>> queryDataByResourceCode(String resourceCode, Map<String, Object> params);
    
    /**
     * 根据资源ID查询数据
     * 
     * @param resourceId 资源ID
     * @param params 查询参数
     * @return 数据列表
     */
    List<Map<String, Object>> queryDataByResourceId(Long resourceId, Map<String, Object> params);
    
    /**
     * 预览数据（限制条数）
     * 
     * @param resourceId 资源ID
     * @param limit 限制条数
     * @return 数据列表
     */
    List<Map<String, Object>> previewData(Long resourceId, int limit);
}
