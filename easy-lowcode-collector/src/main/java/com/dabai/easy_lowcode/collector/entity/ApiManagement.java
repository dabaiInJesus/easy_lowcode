package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * API管理实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collector_api_management")
public class ApiManagement extends BaseEntity {
    
    /**
     * API名称
     */
    private String apiName;
    
    /**
     * API路径
     */
    private String apiPath;
    
    /**
     * HTTP方法 (GET/POST/PUT/DELETE/PATCH)
     */
    private String apiMethod;
    
    /**
     * API类型 (TABLE_RESOURCE-表资源, EXTERNAL-外部接口)
     */
    private String apiType;
    
    /**
     * 来源ID (表资源ID或外部接口配置ID)
     */
    private Long sourceId;
    
    /**
     * API描述
     */
    private String description;
    
    /**
     * 请求配置JSON (参数、headers等)
     */
    private String requestConfig;
    
    /**
     * 响应配置JSON (字段映射等)
     */
    private String responseConfig;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
    
    /**
     * API版本
     */
    private String version = "v1";
    
    /**
     * 是否需要认证 (0-否 1-是)
     */
    private Boolean authRequired = false;
    
    /**
     * 限流次数/分钟
     */
    private Integer rateLimit;
    
    /**
     * 排序号
     */
    private Integer sortOrder = 0;
    
    /**
     * 数据源名称（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private String datasourceName;
    
    /**
     * 表名（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private String tableName;
}
