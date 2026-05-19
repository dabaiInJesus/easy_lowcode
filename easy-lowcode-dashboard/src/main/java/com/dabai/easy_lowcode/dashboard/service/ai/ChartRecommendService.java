package com.dabai.easy_lowcode.dashboard.service.ai;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 图表推荐服务
 * <p>
 * 根据数据特征（字段类型、值分布、行数）自动推荐最合适的图表类型。
 * <p>
 * 规则逻辑（按优先级）：
 * <ol>
 *   <li>时间序列 + 数值 → 折线图</li>
 *   <li>枚举值 ≤ 5 个 → 饼图 / 柱状图</li>
 *   <li>数值 + 分组 → 柱状图</li>
 *   <li>少量行 + 单数值 → 数字卡片</li>
 *   <li>默认 → 表格</li>
 * </ol>
 */
@Slf4j
@Service
public class ChartRecommendService {

    /**
     * 推荐的图表类型结果
     */
    public record ChartRecommendation(
            String chartType,
            String echartsOption,
            String reason
    ) {}

    /**
     * 图表推荐请求（用于 REST API）
     */
    @Data
    public static class ChartRecommendRequest {
        /** 查询结果数据（List of Map） */
        private List<Map<String, Object>> data;
        /** 最大行数 */
        private Integer limit = 100;
    }

    /**
     * 根据查询数据推荐图表
     *
     * @param request 推荐请求（含数据和行数）
     * @return 推荐结果
     */
    public ChartRecommendation recommend(ChartRecommendRequest request) {
        if (request.getData() == null || request.getData().isEmpty()) {
            return new ChartRecommendation("table", null, "空数据，默认表格");
        }
        return recommend(request.getData(), request.getLimit() != null ? request.getLimit() : 100);
    }

    /**
     * 根据查询数据推荐图表（兼容旧接口）
     */
    public ChartRecommendation recommend(List<Map<String, Object>> data, Integer limit) {
        if (data == null || data.isEmpty()) {
            return new ChartRecommendation("table", null, "空数据，默认表格");
        }

        Map<String, FieldType> fieldTypes = analyzeFieldTypes(data.get(0));
        List<String> dateFields = new ArrayList<>();
        List<String> numberFields = new ArrayList<>();
        List<String> enumFields = new ArrayList<>();
        List<String> textFields = new ArrayList<>();

        for (var entry : fieldTypes.entrySet()) {
            switch (entry.getValue()) {
                case DATE, DATETIME -> dateFields.add(entry.getKey());
                case NUMBER -> numberFields.add(entry.getKey());
                case ENUM -> enumFields.add(entry.getKey());
                case TEXT, STRING -> textFields.add(entry.getKey());
            }
        }

        log.debug("字段分析: date={}, number={}, enum={}, text={}",
                dateFields, numberFields, enumFields, textFields);

        if (!dateFields.isEmpty() && !numberFields.isEmpty()) {
            return lineChartRecommendation(dateFields.get(0), numberFields.get(0));
        }

        if (!enumFields.isEmpty()) {
            String enumField = enumFields.get(0);
            Integer distinctCount = countDistinct(data, enumField);
            if (distinctCount <= 5) {
                return pieChartRecommendation(enumField,
                        numberFields.isEmpty() ? textFields.get(0) : numberFields.get(0));
            } else {
                return barChartRecommendation(enumField,
                        numberFields.isEmpty() ? textFields.get(0) : numberFields.get(0));
            }
        }

        if (!numberFields.isEmpty() && textFields.size() == 1) {
            return barChartRecommendation(textFields.get(0), numberFields.get(0));
        }

        if (data.size() <= 3 && numberFields.size() == 1) {
            return gaugeRecommendation(numberFields.get(0), data);
        }

        return new ChartRecommendation("table", null, "无明确特征，默认表格展示");
    }

    private Map<String, FieldType> analyzeFieldTypes(Map<String, Object> sampleRow) {
        Map<String, FieldType> result = new LinkedHashMap<>();
        for (var entry : sampleRow.entrySet()) {
            if (entry.getValue() == null) {
                result.put(entry.getKey(), FieldType.TEXT);
                continue;
            }
            result.put(entry.getKey(), inferFieldType(entry.getKey(), entry.getValue()));
        }
        return result;
    }

    private FieldType inferFieldType(String fieldName, Object value) {
        String strValue = value.toString().toLowerCase();
        String nameLower = fieldName.toLowerCase();

        if (nameLower.contains("time") || nameLower.contains("date") || nameLower.contains("dt")) {
            return FieldType.DATETIME;
        }
        if (nameLower.contains("id") || nameLower.contains("no") || nameLower.contains("code")) {
            return FieldType.TEXT;
        }
        if (nameLower.contains("count") || nameLower.contains("num") ||
                nameLower.contains("amount") || nameLower.contains("sum") ||
                nameLower.contains("price") || nameLower.contains("total") ||
                nameLower.contains("sales") || nameLower.contains("revenue")) {
            return FieldType.NUMBER;
        }

        if (value instanceof Number) return FieldType.NUMBER;
        if (value instanceof Date || value instanceof java.time.LocalDate ||
                value instanceof java.time.LocalDateTime) {
            return FieldType.DATETIME;
        }
        if (strValue.matches("\\d{4}-\\d{2}-\\d{2}.*")) return FieldType.DATETIME;
        if (strValue.matches("-?\\d+(\\.\\d+)?")) return FieldType.NUMBER;

        return FieldType.TEXT;
    }

    private Integer countDistinct(List<Map<String, Object>> data, String field) {
        Set<Object> distinct = new HashSet<>();
        for (Map<String, Object> row : data) {
            distinct.add(row.get(field));
        }
        return distinct.size();
    }

    private ChartRecommendation lineChartRecommendation(String xField, String yField) {
        String option = String.format("""
                {
                  "xAxis": {"type": "category", "data": [], "name": "%s"},
                  "yAxis": {"type": "value", "name": "%s"},
                  "series": [{"name": "%s", "type": "line", "data": [], "smooth": true,
                    "lineStyle": {"width": 2}}],
                  "tooltip": {"trigger": "axis"}
                }
                """, xField, yField, yField);
        return new ChartRecommendation("line", option,
                String.format("时间字段【%s】+ 数值字段【%s】，适合折线图展示趋势", xField, yField));
    }

    private ChartRecommendation pieChartRecommendation(String nameField, String valueField) {
        String option = """
                {
                  "tooltip": {"trigger": "item", "formatter": "{b}: {c} ({d}%)"},
                  "legend": {"orient": "vertical", "left": "left"},
                  "series": [{"type": "pie", "radius": ["40%", "70%"],
                    "label": {"show": true, "formatter": "{b}: {d}%"},
                    "itemStyle": {"borderRadius": 5, "borderColor": "#fff", "borderWidth": 2}}]
                }
                """;
        return new ChartRecommendation("pie", option,
                String.format("枚举字段【%s】，枚举值少，适合饼图", nameField));
    }

    private ChartRecommendation barChartRecommendation(String xField, String yField) {
        String option = String.format("""
                {
                  "xAxis": {"type": "category", "data": [], "name": "%s"},
                  "yAxis": {"type": "value", "name": "%s"},
                  "series": [{"name": "%s", "type": "bar", "data": [],
                    "itemStyle": {"color": "#5470C6"}}],
                  "tooltip": {"trigger": "axis"}
                }
                """, xField, yField, yField);
        return new ChartRecommendation("bar", option,
                String.format("分类字段【%s】+ 数值字段【%s】，适合柱状图", xField, yField));
    }

    private ChartRecommendation gaugeRecommendation(String valueField, List<Map<String, Object>> data) {
        Object currentValue = data.get(data.size() - 1).get(valueField);
        String option = String.format("""
                {
                  "series": [{
                    "type": "gauge",
                    "startAngle": 180,
                    "endAngle": 0,
                    "min": 0,
                    "max": null,
                    "splitNumber": 4,
                    "itemStyle": {"color": "#5470C6"},
                    "detail": {"formatter": "{value}", "fontSize": 24, "offsetCenter": [0, "10%"]},
                    "data": [{"value": %s, "name": "%s"}],
                    "pointer": {"length": "60%%"}
                  }]
                }
                """, currentValue, valueField);
        return new ChartRecommendation("number", option,
                String.format("单值汇总，适合数字卡片展示【%s】", valueField));
    }

    private enum FieldType {
        DATE, DATETIME, NUMBER, ENUM, TEXT, STRING
    }
}
