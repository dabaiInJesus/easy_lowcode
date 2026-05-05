package com.dabai.easy_lowcode.etl.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * ETL任务配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("etl_task")
public class EtlTask extends BaseEntity {

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务编码（唯一标识）
     */
    private String taskCode;

    /**
     * 源数据源ID
     */
    private Long sourceDatasourceId;

    /**
     * 源表/视图名
     */
    private String sourceTable;

    /**
     * 源查询SQL（自定义SQL模式）
     */
    private String sourceSql;

    /**
     * 读取模式 (TABLE-全表, SQL-自定义SQL)
     */
    private String readMode = "TABLE";

    /**
     * 目标数据源ID
     */
    private Long targetDatasourceId;

    /**
     * 目标表名
     */
    private String targetTable;

    /**
     * 写入模式 (INSERT-插入, MERGE-合并, REPLACE-替换, TRUNCATE-清空后插入)
     */
    private String writeMode = "INSERT";

    /**
     * 字段映射JSON (源字段->目标字段映射关系)
     */
    private String fieldMapping;

    /**
     * 转换规则JSON (字段转换/清洗规则配置)
     */
    private String transformRules;

    /**
     * 调度方式 (MANUAL-手动, CRON-定时, INTERVAL-间隔)
     */
    private String scheduleType = "MANUAL";

    /**
     * CRON表达式（定时调度时使用）
     */
    private String cronExpression;

    /**
     * 间隔秒数（间隔调度时使用）
     */
    private Integer intervalSeconds;

    /**
     * 批处理大小（每次提交的记录数）
     */
    private Integer batchSize = 1000;

    /**
     * 是否跳过错误行 (0-否 1-是)
     */
    private Integer skipError = 0;

    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;

    /**
     * 备注
     */
    private String remark;

    /** 非数据库字段 */
    @TableField(exist = false)
    private String sourceDatasourceName;

    @TableField(exist = false)
    private String targetDatasourceName;

    @TableField(exist = false)
    private String lastExecStatus;

    @TableField(exist = false)
    private String lastExecTime;
}
