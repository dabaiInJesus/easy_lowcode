package com.dabai.easy_lowcode.dashboard.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;

import java.util.List;
import java.util.Map;

/**
 * 大屏服务接口
 */
public interface DashboardService extends IService<Dashboard> {

    /**
     * 创建大屏
     */
    boolean createDashboard(Dashboard dashboard);

    /**
     * 更新大屏
     */
    boolean updateDashboard(Dashboard dashboard);

    /**
     * 发布大屏
     */
    boolean publishDashboard(Long id);

    /**
     * 复制大屏（快速创建基于模板的新大屏）
     */
    Long copyDashboard(Long id);

    /**
     * 获取大屏的图表列表
     */
    List<DashboardChart> getCharts(Long dashboardId);

    /**
     * 添加图表
     */
    boolean addChart(DashboardChart chart);

    /**
     * 更新图表
     */
    boolean updateChart(DashboardChart chart);

    /**
     * 删除图表
     */
    boolean removeChart(Long chartId);

    /**
     * 更新图表位置（批量保存布局）
     */
    boolean updateChartPositions(List<DashboardChart> charts);

    /**
     * 执行图表查询，返回数据
     */
    List<Map<String, Object>> executeChartQuery(Long chartId, Map<String, Object> params);

    /**
     * 预览大屏（获取大屏+图表+数据）
     */
    Map<String, Object> previewDashboard(Long id);
}
