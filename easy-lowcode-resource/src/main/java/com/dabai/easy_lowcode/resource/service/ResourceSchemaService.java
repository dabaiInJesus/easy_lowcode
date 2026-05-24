package com.dabai.easy_lowcode.resource.service;

import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.resource.model.FieldConfig;

import java.sql.Connection;
import java.util.List;
import java.util.Set;

/**
 * 资源元数据服务
 * 负责解析表结构、字段配置、主键信息等元数据
 */
public interface ResourceSchemaService {

    /**
     * 获取资源允许的列集合（从缓存或数据库扫描）
     */
    Set<String> getAllowedColumns(TableResource tableResource, Connection conn);

    /**
     * 获取表的完整字段配置列表
     */
    List<FieldConfig> getTableFields(TableResource tableResource, Connection conn);

    /**
     * 获取表的主键列名
     */
    String getPrimaryKeyColumn(String tableName, Connection conn);
}
