package com.dabai.easy_lowcode.dashboard.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Text-to-SQL 响应
 */
@Data
public class TextToSqlResponse {

    /**
     * 生成的 SQL 语句
     */
    private String sql;

    /**
     * SQL 执行结果（execute=true 时有值）
     */
    private List<Map<String, Object>> data;

    /**
     * 数据条数
     */
    private Integer rowCount;

    /**
     * 推荐的图表类型
     */
    private String recommendedChartType;

    /**
     * 推荐的 ECharts 配置（JSON 字符串）
     */
    private String recommendedEchartsOption;

    /**
     * AI 原始回复内容
     */
    private String aiContent;

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 错误信息（执行失败时有值）
     */
    private String errorMessage;
}
