package com.dabai.easy_lowcode.etl.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ETL数据源映射（源→目标字段映射配置）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("etl_datasource_mapping")
public class EtlDatasourceMapping extends BaseEntity {

    /**
     * 任务ID
     */
    private Long taskId;

    /**
     * 源字段名
     */
    private String sourceField;

    /**
     * 源字段类型
     */
    private String sourceFieldType;

    /**
     * 目标字段名
     */
    private String targetField;

    /**
     * 目标字段类型
     */
    private String targetFieldType;

    /**
     * 转换类型 (NONE-不转换, FUNC-函数转换, EXPR-表达式转换, DICT-字典映射)
     */
    private String transformType = "NONE";

    /**
     * 转换规则（函数名、表达式或字典JSON）
     */
    private String transformRule;

    /**
     * 默认值（源字段为空时使用）
     */
    private String defaultValue;

    /**
     * 是否为主键字段 (0-否 1-是)
     */
    private Integer isPrimaryKey = 0;

    /**
     * 排序号
     */
    private Integer sortOrder = 0;
}
