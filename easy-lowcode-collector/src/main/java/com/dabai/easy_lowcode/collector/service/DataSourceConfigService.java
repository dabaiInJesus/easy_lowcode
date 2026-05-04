package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;

import java.util.List;
import java.util.Map;

/**
 * 数据源配置服务接口
 */
public interface DataSourceConfigService extends IService<DataSourceConfig> {
    
    /**
     * 测试数据库连接
     * 
     * @param config 数据源配置
     * @return 测试结果
     */
    boolean testConnection(DataSourceConfig config);
    
    /**
     * 扫描数据库表列表
     * 
     * @param datasourceId 数据源ID
     * @return 表列表
     */
    List<Map<String, Object>> scanTables(Long datasourceId);
    
    /**
     * 获取表结构信息
     * 
     * @param datasourceId 数据源ID
     * @param tableName 表名
     * @return 表结构信息
     */
    List<Map<String, Object>> getTableColumns(Long datasourceId, String tableName);
}
