package com.dabai.easy_lowcode.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图表数据源配置（数据预处理配置）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dashboard_chart_datasource")
public class ChartDataSource extends BaseEntity {

    /**
     * 图表ID
     */
    private Long chartId;

    /**
     * 数据源类型 (SQL-自定义SQL, API-接口, RESOURCE-表资源)
     */
    private String sourceType;

    /**
     * 数据源ID
     */
    private Long datasourceId;

    /**
     * 查询SQL（SQL模式时使用）
     */
    private String querySql;

    /**
     * API路径（API模式时使用）
     */
    private String apiPath;

    /**
     * API方法 (GET/POST)
     */
    private String apiMethod = "GET";

    /**
     * 请求参数JSON
     */
    private String requestParams;

    /**
     * 数据预处理脚本（JavaScript表达式，用于对返回数据做二次处理）
     */
    private String dataTransformScript;

    /**
     * 缓存时间(秒)
     */
    private Integer cacheSeconds = 0;

    /**
     * 排序号
     */
    private Integer sortOrder = 0;
}
