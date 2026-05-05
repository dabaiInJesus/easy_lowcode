package com.dabai.easy_lowcode.dashboard.service;

import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;
import com.dabai.easy_lowcode.dashboard.mapper.ChartDataSourceMapper;
import com.dabai.easy_lowcode.dashboard.mapper.DashboardChartMapper;
import com.dabai.easy_lowcode.dashboard.mapper.DashboardMapper;
import com.dabai.easy_lowcode.dashboard.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 大屏服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceImplTest {

    @Mock
    private DashboardMapper dashboardMapper;

    @Mock
    private DashboardChartMapper chartMapper;

    @Mock
    private ChartDataSourceMapper chartDataSourceMapper;

    @InjectMocks
    private DashboardServiceImpl dashboardService;

    private Dashboard dashboard;

    @BeforeEach
    void setUp() {
        dashboard = new Dashboard();
        dashboard.setName("运营大屏");
        dashboard.setCode("ops_dashboard");
        dashboard.setTitle("运营数据概览");
        dashboard.setWidth(1920);
        dashboard.setHeight(1080);
        dashboard.setBackgroundColor("#0a1628");
        dashboard.setStatus(0);
    }

    @Test
    void testCreateDashboardSuccess() {
        when(dashboardMapper.insert(any(Dashboard.class))).thenReturn(1);
        assertDoesNotThrow(() -> dashboardService.createDashboard(dashboard));
        verify(dashboardMapper, times(1)).insert(any(Dashboard.class));
    }

    @Test
    void testCreateDashboardWithoutName() {
        dashboard.setName(null);
        Exception ex = assertThrows(Exception.class, () -> dashboardService.createDashboard(dashboard));
        assertTrue(ex.getMessage().contains("大屏名称不能为空"));
    }

    @Test
    void testCreateDashboardWithoutCode() {
        dashboard.setCode(null);
        Exception ex = assertThrows(Exception.class, () -> dashboardService.createDashboard(dashboard));
        assertTrue(ex.getMessage().contains("大屏编码不能为空"));
    }

    @Test
    void testPublishDashboardWithoutCharts() {
        dashboard.setId(1L);
        when(dashboardMapper.selectById(1L)).thenReturn(dashboard);
        when(chartMapper.selectCount(any())).thenReturn(0L);

        Exception ex = assertThrows(Exception.class, () -> dashboardService.publishDashboard(1L));
        assertTrue(ex.getMessage().contains("没有图表"));
    }

    @Test
    void testPublishNonExistentDashboard() {
        when(dashboardMapper.selectById(999L)).thenReturn(null);
        Exception ex = assertThrows(Exception.class, () -> dashboardService.publishDashboard(999L));
        assertTrue(ex.getMessage().contains("大屏不存在"));
    }

    @Test
    void testAddChartWithoutTitle() {
        DashboardChart chart = new DashboardChart();
        chart.setDashboardId(1L);
        chart.setChartType("bar");
        Exception ex = assertThrows(Exception.class, () -> dashboardService.addChart(chart));
        assertTrue(ex.getMessage().contains("图表标题不能为空"));
    }

    @Test
    void testAddChartWithoutType() {
        DashboardChart chart = new DashboardChart();
        chart.setDashboardId(1L);
        chart.setTitle("测试图表");
        Exception ex = assertThrows(Exception.class, () -> dashboardService.addChart(chart));
        assertTrue(ex.getMessage().contains("图表类型不能为空"));
    }

    @Test
    void testAddChartSuccess() {
        DashboardChart chart = new DashboardChart();
        chart.setDashboardId(1L);
        chart.setTitle("测试图表");
        chart.setChartType("bar");
        when(chartMapper.insert(any(DashboardChart.class))).thenReturn(1);
        assertDoesNotThrow(() -> dashboardService.addChart(chart));
        verify(chartMapper, times(1)).insert(any(DashboardChart.class));
    }

    @Test
    void testCopyDashboard() {
        when(dashboardMapper.selectById(1L)).thenReturn(dashboard);
        when(dashboardMapper.insert(any(Dashboard.class))).thenReturn(1);

        Long newId = dashboardService.copyDashboard(1L);
        assertNotNull(newId);
        verify(dashboardMapper, times(1)).insert(any(Dashboard.class));
    }

    @Test
    void testDefaultValues() {
        Dashboard d = new Dashboard();
        assertEquals(Integer.valueOf(0), d.getStatus());
        assertEquals(Integer.valueOf(1920), d.getWidth());
        assertEquals(Integer.valueOf(1080), d.getHeight());
        assertEquals("#0a1628", d.getBackgroundColor());
    }
}
