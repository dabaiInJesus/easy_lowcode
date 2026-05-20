package com.dabai.easy_lowcode.dashboard.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.dashboard.entity.ChartDataSource;
import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;
import com.dabai.easy_lowcode.dashboard.mapper.ChartDataSourceMapper;
import com.dabai.easy_lowcode.dashboard.mapper.DashboardChartMapper;
import com.dabai.easy_lowcode.dashboard.mapper.DashboardMapper;
import com.dabai.easy_lowcode.dashboard.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 大屏服务单元测试
 * <p>
 * 注意: DashboardServiceImpl 继承 ServiceImpl，其 baseMapper 由框架管理。
 * 测试通过 ReflectionTestUtils 手动注入 baseMapper。
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMapper dashboardMapper;
    @Mock
    private DashboardChartMapper chartMapper;
    @Mock
    private ChartDataSourceMapper chartDataSourceMapper;
    @Mock
    private DataSourceConfigMapper dataSourceConfigMapper;
    @Mock
    private ChartCacheService chartCacheService;

    private DashboardServiceImpl dashboardService;

    private Dashboard dashboard;
    private DashboardChart chart;

    @BeforeEach
    void setUp() {
        // 手动创建 service 并注入 baseMapper（ServiceImpl 的核心依赖）
        dashboardService = new DashboardServiceImpl(chartMapper, chartDataSourceMapper, dataSourceConfigMapper, chartCacheService);
        ReflectionTestUtils.setField((ServiceImpl<?, Dashboard>) dashboardService, "baseMapper", dashboardMapper);

        dashboard = new Dashboard();
        dashboard.setId(1L);
        dashboard.setName("测试大屏");
        dashboard.setCode("test-dashboard");
        dashboard.setTitle("测试大屏标题");
        dashboard.setStatus(0);

        chart = new DashboardChart();
        chart.setId(10L);
        chart.setDashboardId(1L);
        chart.setTitle("测试图表");
        chart.setChartType("line");
        chart.setSortOrder(1);
    }

    // ==================== createDashboard ====================

    @Test
    void createDashboard_success() {
        when(dashboardMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dashboardMapper.insert(any(Dashboard.class))).thenReturn(1);
        assertTrue(dashboardService.createDashboard(dashboard));
        verify(dashboardMapper).insert(any(Dashboard.class));
    }

    @Test
    void createDashboard_emptyName_throws() {
        dashboard.setName("   ");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.createDashboard(dashboard));
        assertEquals("大屏名称不能为空", ex.getMessage());
    }

    @Test
    void createDashboard_emptyCode_throws() {
        dashboard.setCode("");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.createDashboard(dashboard));
        assertEquals("大屏编码不能为空", ex.getMessage());
    }

    @Test
    void createDashboard_duplicateCode_throws() {
        when(dashboardMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.createDashboard(dashboard));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    @Test
    void createDashboard_setsDefaults() {
        dashboard.setStatus(null);
        dashboard.setWidth(null);
        dashboard.setHeight(null);
        when(dashboardMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(dashboardMapper.insert(any(Dashboard.class))).thenAnswer(inv -> {
            Dashboard d = inv.getArgument(0);
            assertEquals(0, d.getStatus());
            assertEquals(1920, d.getWidth());
            assertEquals(1080, d.getHeight());
            return 1;
        });
        dashboardService.createDashboard(dashboard);
    }

    // ==================== updateDashboard ====================

    @Test
    void updateDashboard_notFound_throws() {
        dashboard.setId(999L);
        when(dashboardMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.updateDashboard(dashboard));
        assertEquals("大屏不存在", ex.getMessage());
    }

    @Test
    void updateDashboard_success() {
        Dashboard existing = new Dashboard();
        existing.setId(1L);
        existing.setCode("test-dashboard");
        when(dashboardMapper.selectById(1L)).thenReturn(existing);
        when(dashboardMapper.updateById(any(Dashboard.class))).thenReturn(1);
        dashboardService.updateDashboard(dashboard);
        verify(dashboardMapper).updateById(any(Dashboard.class));
    }

    @Test
    void updateDashboard_duplicateCode_throws() {
        Dashboard existing = new Dashboard();
        existing.setId(1L);
        existing.setCode("test-dashboard");
        dashboard.setCode("new-code");
        when(dashboardMapper.selectById(1L)).thenReturn(existing);
        when(dashboardMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.updateDashboard(dashboard));
        assertTrue(ex.getMessage().contains("已存在"));
    }

    // ==================== publishDashboard ====================

    @Test
    void publishDashboard_noCharts_throws() {
        when(dashboardMapper.selectById(1L)).thenReturn(dashboard);
        when(chartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.publishDashboard(1L));
        assertEquals("大屏没有图表，无法发布", ex.getMessage());
    }

    @Test
    void publishDashboard_success() {
        when(dashboardMapper.selectById(1L)).thenReturn(dashboard);
        when(chartMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
        when(dashboardMapper.updateById(any(Dashboard.class))).thenReturn(1);
        assertTrue(dashboardService.publishDashboard(1L));
    }

    // ==================== copyDashboard ====================

    @Test
    void copyDashboard_success() {
        Dashboard source = new Dashboard();
        source.setId(1L);
        source.setName("原始大屏");
        source.setCode("source-code");
        source.setTitle("标题");
        source.setDescription("描述");
        source.setWidth(1920);
        source.setHeight(1080);
        source.setBackgroundColor("#000");
        source.setStatus(1);
        source.setCategory("生产");
        source.setTags("tag1,tag2");
        source.setRefreshInterval(30);

        DashboardChart chart1 = new DashboardChart();
        chart1.setId(10L);
        chart1.setDashboardId(1L);
        chart1.setTitle("图表1");
        chart1.setChartType("line");

        when(dashboardMapper.selectById(1L)).thenReturn(source);
        // copyDashboard 调用 chartMapper.selectList 查图表列表
        when(chartMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(chart1));
        when(dashboardMapper.insert(any(Dashboard.class))).thenAnswer(inv -> {
            Dashboard d = inv.getArgument(0);
            ReflectionTestUtils.setField(d, "id", 99L);
            return 1;
        });
        when(chartMapper.insert(any(DashboardChart.class))).thenReturn(1);

        Long newId = dashboardService.copyDashboard(1L);
        assertEquals(99L, newId);
        // 验证大屏和图表都被插入了
        verify(dashboardMapper).insert(argThat((Dashboard d) ->
            d.getName().equals("原始大屏 (副本)") && d.getStatus() == 0));
        verify(chartMapper).insert(any(DashboardChart.class));
    }

    @Test
    void copyDashboard_sourceNotFound_throws() {
        when(dashboardMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.copyDashboard(999L));
        assertEquals("大屏不存在", ex.getMessage());
    }

    // ==================== getCharts ====================

    @Test
    void getCharts_returnsOrderedList() {
        DashboardChart c1 = new DashboardChart();
        c1.setSortOrder(2);
        DashboardChart c2 = new DashboardChart();
        c2.setSortOrder(1);
        when(chartMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Arrays.asList(c2, c1));
        List<DashboardChart> result = dashboardService.getCharts(1L);
        assertEquals(2, result.size());
        verify(chartMapper).selectList(any(LambdaQueryWrapper.class));
    }

    // ==================== addChart ====================

    @Test
    void addChart_success() {
        chart.setDashboardId(1L);
        when(chartMapper.insert(any(DashboardChart.class))).thenReturn(1);
        assertTrue(dashboardService.addChart(chart));
    }

    @Test
    void addChart_noDashboardId_throws() {
        chart.setDashboardId(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.addChart(chart));
        assertEquals("所属大屏不能为空", ex.getMessage());
    }

    @Test
    void addChart_emptyTitle_throws() {
        chart.setDashboardId(1L);
        chart.setTitle("   ");
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.addChart(chart));
        assertEquals("图表标题不能为空", ex.getMessage());
    }

    @Test
    void addChart_noChartType_throws() {
        chart.setDashboardId(1L);
        chart.setTitle("标题");
        chart.setChartType(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.addChart(chart));
        assertEquals("图表类型不能为空", ex.getMessage());
    }

    // ==================== removeChart ====================

    @Test
    void removeChart_success() {
        DashboardChart existing = new DashboardChart();
        existing.setId(10L);
        existing.setDashboardId(1L);
        when(chartMapper.selectById(10L)).thenReturn(existing);
        when(chartMapper.deleteById(10L)).thenReturn(1);
        when(chartDataSourceMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(1);
        assertTrue(dashboardService.removeChart(10L));
        verify(chartCacheService).invalidate(10L);
        verify(chartDataSourceMapper).delete(any(LambdaQueryWrapper.class));
    }

    @Test
    void removeChart_notFound_throws() {
        when(chartMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.removeChart(999L));
        assertEquals("图表不存在", ex.getMessage());
    }

    // ==================== updateChart ====================

    @Test
    void updateChart_success() {
        DashboardChart existing = new DashboardChart();
        existing.setId(10L);
        when(chartMapper.selectById(10L)).thenReturn(existing);
        when(chartMapper.updateById(any(DashboardChart.class))).thenReturn(1);
        assertTrue(dashboardService.updateChart(chart));
        verify(chartCacheService).invalidate(10L);
    }

    @Test
    void updateChart_notFound_throws() {
        when(chartMapper.selectById(999L)).thenReturn(null);
        chart.setId(999L);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.updateChart(chart));
        assertEquals("图表不存在", ex.getMessage());
    }

    // ==================== executeChartQuery ====================

    @Test
    void executeChartQuery_datasourceNotFound_throws() {
        DashboardChart c = new DashboardChart();
        c.setId(10L);
        c.setDatasourceId(999L);
        c.setQuerySql("SELECT 1");
        when(chartMapper.selectById(10L)).thenReturn(c);
        when(dataSourceConfigMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.executeChartQuery(10L, new HashMap<>()));
        assertEquals("数据源不存在", ex.getMessage());
    }

    @Test
    void executeChartQuery_noDatasourceConfigured_throws() {
        DashboardChart c = new DashboardChart();
        c.setId(10L);
        c.setDatasourceId(null);
        c.setQuerySql(null);
        c.setTableResourceId(null);
        when(chartMapper.selectById(10L)).thenReturn(c);
        when(chartDataSourceMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.executeChartQuery(10L, new HashMap<>()));
        assertEquals("图表数据源未配置", ex.getMessage());
    }

    // ==================== previewDashboard ====================

    @Test
    void previewDashboard_notFound_throws() {
        when(dashboardMapper.selectById(999L)).thenReturn(null);
        BusinessException ex = assertThrows(BusinessException.class,
            () -> dashboardService.previewDashboard(999L));
        assertEquals("大屏不存在", ex.getMessage());
    }

    @Test
    void previewDashboard_success() {
        when(dashboardMapper.selectById(1L)).thenReturn(dashboard);
        when(chartMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.singletonList(chart));
        when(chartMapper.selectById(10L)).thenReturn(chart);
        when(chartDataSourceMapper.selectList(any(LambdaQueryWrapper.class)))
            .thenReturn(Collections.emptyList());
        Map<String, Object> result = dashboardService.previewDashboard(1L);
        assertNotNull(result);
        assertEquals(dashboard, result.get("dashboard"));
        assertEquals(1, result.get("chartCount"));
    }
}
