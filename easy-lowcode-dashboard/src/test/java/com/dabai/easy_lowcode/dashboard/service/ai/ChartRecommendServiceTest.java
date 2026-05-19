package com.dabai.easy_lowcode.dashboard.service.ai;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChartRecommendServiceTest {

    private final ChartRecommendService service = new ChartRecommendService();

    private List<Map<String, Object>> mapOf(String k1, Object v1, String k2, Object v2, String k3, Object v3) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>(); m1.put(k1, v1); m1.put(k2, v2); m1.put(k3, v3); list.add(m1);
        return list;
    }

    private List<Map<String, Object>> mapOf(String k1, Object v1, String k2, Object v2) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> m1 = new HashMap<>(); m1.put(k1, v1); m1.put(k2, v2); list.add(m1);
        return list;
    }

    private List<Map<String, Object>> singleMap(String k, Object v) {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> m = new HashMap<>(); m.put(k, v); list.add(m);
        return list;
    }

    @Test
    void recommend_timeSeriesData_returnsLineChart() {
        List<Map<String, Object>> data = mapOf("日期", "2024-01-01", "销售额", 1000.0, "日期2", "2024-01-02");
        data.add(mapOf("日期", "2024-01-02", "销售额", 1500.0, "日期2", "").get(0));
        data.add(mapOf("日期", "2024-01-03", "销售额", 1200.0, "日期2", "").get(0));
        // 简化：用单列时间序列
        List<Map<String, Object>> tsData = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>(); r1.put("日期", "2024-01-01"); r1.put("销售额", 1000.0); tsData.add(r1);
        Map<String, Object> r2 = new HashMap<>(); r2.put("日期", "2024-01-02"); r2.put("销售额", 1500.0); tsData.add(r2);
        Map<String, Object> r3 = new HashMap<>(); r3.put("日期", "2024-01-03"); r3.put("销售额", 1200.0); tsData.add(r3);

        var result = service.recommend(tsData, 100);

        assertEquals("line", result.chartType());
        assertNotNull(result.echartsOption());
    }

    @Test
    void recommend_categoryData_returnsPieChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>(); r1.put("省份", "广东"); r1.put("订单数", 5000); data.add(r1);
        Map<String, Object> r2 = new HashMap<>(); r2.put("省份", "浙江"); r2.put("订单数", 3000); data.add(r2);

        var result = service.recommend(data, 100);

        assertEquals("pie", result.chartType());
    }

    @Test
    void recommend_rankingData_returnsBarChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>(); r1.put("商品", "商品A"); r1.put("销量", 800); data.add(r1);
        Map<String, Object> r2 = new HashMap<>(); r2.put("商品", "商品B"); r2.put("销量", 650); data.add(r2);

        var result = service.recommend(data, 100);

        assertEquals("bar", result.chartType());
    }

    @Test
    void recommend_singleMetric_returnsNumber() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> r = new HashMap<>(); r.put("总销售额", 99999.9); data.add(r);

        var result = service.recommend(data, 10);

        assertEquals("number", result.chartType());
    }

    @Test
    void recommend_emptyData_returnsLineAsDefault() {
        var data = new ArrayList<Map<String, Object>>();

        var result = service.recommend(data, 0);

        assertNotNull(result.chartType());
    }

    @Test
    void recommend_withNullValues_handlesGracefully() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>(); r1.put("日期", "2024-01-01"); r1.put("销售额", 1000); data.add(r1);
        Map<String, Object> r2 = new HashMap<>(); r2.put("日期", "2024-01-02"); r2.put("销售额", null); data.add(r2);

        var result = service.recommend(data, 100);

        assertNotNull(result.chartType());
    }

    @Test
    void recommend_geographicData_returnsBarChart() {
        List<Map<String, Object>> data = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>(); r1.put("city", "深圳"); r1.put("users", 500000); data.add(r1);
        Map<String, Object> r2 = new HashMap<>(); r2.put("city", "广州"); r2.put("users", 400000); data.add(r2);

        var result = service.recommend(data, 100);

        assertEquals("bar", result.chartType());
    }
}
