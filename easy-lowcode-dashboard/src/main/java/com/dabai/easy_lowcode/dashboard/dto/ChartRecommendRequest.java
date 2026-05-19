package com.dabai.easy_lowcode.dashboard.dto;

import lombok.Data;

import java.util.List;

/**
 * 图表推荐请求
 */
@Data
public class ChartRecommendRequest {

    /**
     * 字段列表（用于分析数据特征）
     */
    private List<FieldInfo> fields;

    /**
     * 数据行数
     */
    private Integer rowCount;

    /**
     * 已有图表类型偏好（可选）
     */
    private String preferredChartType;

    /**
     * 字段信息
     */
    @Data
    public static class FieldInfo {
        /**
         * 字段名
         */
        private String name;
        /**
         * 字段类型：string / number / date / enum
         */
        private String type;
        /**
         * 枚举值数量（仅 enum 类型有值）
         */
        private Integer distinctCount;
        /**
         * 示例值
         */
        private String sampleValue;
    }
}
