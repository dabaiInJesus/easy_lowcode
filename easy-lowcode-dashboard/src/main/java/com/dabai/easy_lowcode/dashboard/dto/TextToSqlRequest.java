package com.dabai.easy_lowcode.dashboard.dto;

import lombok.Data;

/**
 * Text-to-SQL 请求
 */
@Data
public class TextToSqlRequest {

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 目标表名
     */
    private String tableName;

    /**
     * 用户问题（自然语言）
     * 例如："展示近7天每日销售额趋势"
     */
    private String question;

    /**
     * 限制返回条数
     */
    private Integer limit = 100;

    /**
     * 是否直接执行生成的 SQL
     */
    private boolean execute = true;
}
