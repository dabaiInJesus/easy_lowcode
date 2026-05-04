package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据源配置实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collector_datasource")
public class DataSourceConfig extends BaseEntity {
    
    /**
     * 数据源名称
     */
    private String name;
    
    /**
     * 数据源编码（唯一标识）
     */
    private String code;
    
    /**
     * 数据库类型 (mysql/postgresql/oracle/sqlserver)
     */
    private String dbType;
    
    /**
     * JDBC URL
     */
    private String url;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * 密码（加密存储）
     */
    private String password;
    
    /**
     * 驱动类名
     */
    private String driverClassName;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
    
    /**
     * 备注
     */
    private String remark;
}
