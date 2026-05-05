package com.dabai.easy_lowcode.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 大屏图表配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dashboard_chart")
public class DashboardChart extends BaseEntity {

    /**
     * 大屏ID
     */
    private Long dashboardId;

    /**
     * 图表标题
     */
    private String title;

    /**
     * 图表类型 (bar-柱状图, line-折线图, pie-饼图, scatter-散点图,
     *          map-地图, radar-雷达图, gauge-仪表盘, text-文本,
     *          number-数字, table-表格, iframe-嵌入页面)
     */
    private String chartType;

    /**
     * 数据源ID (关联collector_datasource表)
     */
    private Long datasourceId;

    /**
     * 表资源ID (关联collector_table_resource表，可选)
     */
    private Long tableResourceId;

    /**
     * 查询SQL/API路径
     */
    private String querySql;

    /**
     * X轴字段
     */
    private String xField;

    /**
     * Y轴字段（多个用逗号分隔）
     */
    private String yField;

    /**
     * 分组字段
     */
    private String groupField;

    /**
     * 筛选条件JSON
     */
    private String filterConfig;

    /**
     * 排序字段
     */
    private String orderField;

    /**
     * 排序方向 (ASC/DESC)
     */
    private String orderDirection = "ASC";

    /**
     * 限制记录数
     */
    private Integer limitRecords = 100;

    /**
     * 图表样式配置JSON (ECharts option覆盖)
     */
    private String chartOption;

    /**
     * 位置X (网格坐标)
     */
    private Integer posX;

    /**
     * 位置Y (网格坐标)
     */
    private Integer posY;

    /**
     * 宽度(网格单位)
     */
    private Integer width;

    /**
     * 高度(网格单位)
     */
    private Integer height;

    /**
     * 自动刷新间隔(秒)，0表示不自动刷新
     */
    private Integer refreshInterval = 0;

    /**
     * 排序号
     */
    private Integer sortOrder = 0;

    /**
     * 备注
     */
    private String remark;
}
