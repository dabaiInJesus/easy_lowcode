package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 表资源注册实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collector_table_resource")
public class TableResource extends BaseEntity {
    
    /**
     * 数据源ID
     */
    private Long datasourceId;
    
    /**
     * 表名
     */
    private String tableName;
    
    /**
     * 表注释
     */
    private String tableComment;
    
    /**
     * 资源编码（唯一标识）
     */
    private String resourceCode;
    
    /**
     * API路径
     */
    private String apiPath;
    
    /**
     * 支持的HTTP方法 (GET,POST,PUT,DELETE)
     */
    private String methods;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
    
    /**
     * 配置JSON（字段映射、验证规则等）
     */
    private String configJson;
    
    /**
     * 数据源名称（非数据库字段，用于展示）
     */
    @TableField(exist = false)
    private String datasourceName;
}
