package com.dabai.easy_lowcode.dashboard.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.dashboard.entity.ChartDataSource;
import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;
import com.dabai.easy_lowcode.dashboard.mapper.ChartDataSourceMapper;
import com.dabai.easy_lowcode.dashboard.mapper.DashboardChartMapper;
import com.dabai.easy_lowcode.dashboard.mapper.DashboardMapper;
import com.dabai.easy_lowcode.dashboard.service.ChartCacheService;
import com.dabai.easy_lowcode.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 大屏服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl extends ServiceImpl<DashboardMapper, Dashboard> implements DashboardService {

    private final DashboardChartMapper chartMapper;
    private final ChartDataSourceMapper chartDataSourceMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final ChartCacheService chartCacheService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createDashboard(Dashboard dashboard) {
        if (dashboard.getName() == null || dashboard.getName().trim().isEmpty()) {
            throw new BusinessException("大屏名称不能为空");
        }
        if (dashboard.getCode() == null || dashboard.getCode().trim().isEmpty()) {
            throw new BusinessException("大屏编码不能为空");
        }

        // 检查编码唯一性
        LambdaQueryWrapper<Dashboard> check = new LambdaQueryWrapper<>();
        check.eq(Dashboard::getCode, dashboard.getCode());
        if (this.count(check) > 0) {
            throw new BusinessException("大屏编码已存在: " + dashboard.getCode());
        }

        if (dashboard.getStatus() == null) dashboard.setStatus(0);
        if (dashboard.getWidth() == null) dashboard.setWidth(1920);
        if (dashboard.getHeight() == null) dashboard.setHeight(1080);

        return this.save(dashboard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateDashboard(Dashboard dashboard) {
        Dashboard existing = this.getById(dashboard.getId());
        if (existing == null) throw new BusinessException("大屏不存在");

        if (dashboard.getCode() != null && !dashboard.getCode().equals(existing.getCode())) {
            LambdaQueryWrapper<Dashboard> check = new LambdaQueryWrapper<>();
            check.eq(Dashboard::getCode, dashboard.getCode());
            check.ne(Dashboard::getId, dashboard.getId());
            if (this.count(check) > 0) {
                throw new BusinessException("大屏编码已存在: " + dashboard.getCode());
            }
        }
        return this.updateById(dashboard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishDashboard(Long id) {
        Dashboard dashboard = this.getById(id);
        if (dashboard == null) throw new BusinessException("大屏不存在");

        // 检查是否有图表
        Long chartCount = chartMapper.selectCount(
            new LambdaQueryWrapper<DashboardChart>().eq(DashboardChart::getDashboardId, id));
        if (chartCount == 0) {
            throw new BusinessException("大屏没有图表，无法发布");
        }

        dashboard.setStatus(1);
        return this.updateById(dashboard);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copyDashboard(Long id) {
        Dashboard source = this.getById(id);
        if (source == null) throw new BusinessException("大屏不存在");

        // 复制大屏
        Dashboard target = new Dashboard();
        target.setName(source.getName() + " (副本)");
        target.setCode(source.getCode() + "_copy_" + System.currentTimeMillis());
        target.setTitle(source.getTitle());
        target.setDescription(source.getDescription());
        target.setWidth(source.getWidth());
        target.setHeight(source.getHeight());
        target.setBackgroundColor(source.getBackgroundColor());
        target.setBackgroundImage(source.getBackgroundImage());
        target.setLayoutConfig(source.getLayoutConfig());
        target.setStyleConfig(source.getStyleConfig());
        target.setRefreshInterval(source.getRefreshInterval());
        target.setStatus(0); // 副本都是草稿
        target.setCategory(source.getCategory());
        target.setTags(source.getTags());
        this.save(target);

        // 复制图表
        List<DashboardChart> charts = chartMapper.selectList(
            new LambdaQueryWrapper<DashboardChart>().eq(DashboardChart::getDashboardId, id));
        for (DashboardChart chart : charts) {
            chart.setId(null);
            chart.setDashboardId(target.getId());
            chartMapper.insert(chart);
        }

        return target.getId();
    }

    @Override
    public List<DashboardChart> getCharts(Long dashboardId) {
        return chartMapper.selectList(
            new LambdaQueryWrapper<DashboardChart>()
                .eq(DashboardChart::getDashboardId, dashboardId)
                .orderByAsc(DashboardChart::getSortOrder));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean addChart(DashboardChart chart) {
        if (chart.getDashboardId() == null) throw new BusinessException("所属大屏不能为空");
        if (chart.getTitle() == null || chart.getTitle().trim().isEmpty()) {
            throw new BusinessException("图表标题不能为空");
        }
        if (chart.getChartType() == null) throw new BusinessException("图表类型不能为空");
        if (chart.getSortOrder() == null) chart.setSortOrder(0);
        return chartMapper.insert(chart) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateChart(DashboardChart chart) {
        DashboardChart existing = chartMapper.selectById(chart.getId());
        if (existing == null) throw new BusinessException("图表不存在");
        // 更新前清除缓存（SQL 或配置变化都可能导致数据不同）
        chartCacheService.invalidate(chart.getId());
        return chartMapper.updateById(chart) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeChart(Long chartId) {
        DashboardChart chart = chartMapper.selectById(chartId);
        if (chart == null) throw new BusinessException("图表不存在");
        // 删除前清除缓存
        chartCacheService.invalidate(chartId);
        // 删除关联的数据源配置
        chartDataSourceMapper.delete(
            new LambdaQueryWrapper<ChartDataSource>().eq(ChartDataSource::getChartId, chartId));
        return chartMapper.deleteById(chartId) > 0;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateChartPositions(List<DashboardChart> charts) {
        for (DashboardChart chart : charts) {
            if (chart.getId() != null) {
                chartMapper.updateById(chart);
            }
        }
        return true;
    }

    @Override
    public List<Map<String, Object>> executeChartQuery(Long chartId, Map<String, Object> params) {
        DashboardChart chart = chartMapper.selectById(chartId);
        if (chart == null) throw new BusinessException("图表不存在");

        // 1. 如果有表资源ID，通过表资源查询
        if (chart.getTableResourceId() != null) {
            return queryByTableResource(chart, params);
        }

        // 2. 如果有数据源ID和SQL，直接执行SQL（带缓存）
        if (chart.getDatasourceId() != null && chart.getQuerySql() != null && !chart.getQuerySql().isEmpty()) {
            // 尝试从缓存获取（TTL = 图表配置的刷新间隔，0则用默认值）
            int ttlSeconds = chart.getRefreshInterval() != null && chart.getRefreshInterval() > 0
                    ? chart.getRefreshInterval() : 0;
            var cached = chartCacheService.get(chartId, chart.getQuerySql(), chart.getLimitRecords(), ttlSeconds);
            if (cached.isPresent()) {
                log.debug("图表 {} 缓存命中，返回 {} 条数据", chartId, cached.get().size());
                return cached.get();
            }
            List<Map<String, Object>> data = executeSql(chart.getDatasourceId(), chart.getQuerySql(), chart.getLimitRecords());
            chartCacheService.put(chartId, chart.getQuerySql(), data, ttlSeconds);
            return data;
        }

        // 3. 如果有自定义数据源配置
        List<ChartDataSource> dataSources = chartDataSourceMapper.selectList(
            new LambdaQueryWrapper<ChartDataSource>().eq(ChartDataSource::getChartId, chartId));
        if (!dataSources.isEmpty()) {
            ChartDataSource ds = dataSources.get(0);
            if ("SQL".equals(ds.getSourceType()) && ds.getDatasourceId() != null && ds.getQuerySql() != null) {
                return executeSql(ds.getDatasourceId(), ds.getQuerySql(), chart.getLimitRecords());
            }
        }

        throw new BusinessException("图表数据源未配置");
    }

    private List<Map<String, Object>> queryByTableResource(DashboardChart chart, Map<String, Object> params) {
        // 通过DynamicDataService查询表资源数据（简化实现）
        List<Map<String, Object>> data = new ArrayList<>();
        // 实际应调用 DynamicDataService，这里直接基于SQL执行
        if (chart.getQuerySql() != null && !chart.getQuerySql().isEmpty()) {
            return executeSql(chart.getDatasourceId(), chart.getQuerySql(), chart.getLimitRecords());
        }
        return data;
    }

    private List<Map<String, Object>> executeSql(Long datasourceId, String sql, Integer limit) {
        DataSourceConfig ds = dataSourceConfigMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException("数据源不存在");

        List<Map<String, Object>> result = new ArrayList<>();
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            String pwd;
            try {
                pwd = EncryptUtil.decrypt(ds.getPassword());
            } catch (Exception e) {
                pwd = ds.getPassword();
            }

            Class.forName(ds.getDriverClassName());
            conn = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), pwd);
            stmt = conn.createStatement();

            // 限制条数
            String finalSql = sql;
            if (limit != null && limit > 0) {
                finalSql = "SELECT * FROM (" + sql + ") t LIMIT " + limit;
            }

            rs = stmt.executeQuery(finalSql);
            int colCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
        } catch (Exception e) {
            log.error("图表查询失败: datasourceId={}, sql={}", datasourceId, sql, e);
            throw new BusinessException("查询失败: " + e.getMessage());
        } finally {
            if (rs != null) try { rs.close(); } catch (Exception ignore) {}
            if (stmt != null) try { stmt.close(); } catch (Exception ignore) {}
            if (conn != null) try { conn.close(); } catch (Exception ignore) {}
        }
        return result;
    }

    @Override
    public Map<String, Object> previewDashboard(Long id) {
        Dashboard dashboard = this.getById(id);
        if (dashboard == null) throw new BusinessException("大屏不存在");

        List<DashboardChart> charts = getCharts(id);
        List<Map<String, Object>> chartDataList = new ArrayList<>();

        for (DashboardChart chart : charts) {
            Map<String, Object> chartData = new LinkedHashMap<>();
            chartData.put("chart", chart);
            try {
                List<Map<String, Object>> data = executeChartQuery(chart.getId(), new HashMap<>());
                chartData.put("data", data);
            } catch (Exception e) {
                log.warn("图表[{}]查询失败: {}", chart.getId(), e.getMessage());
                chartData.put("data", Collections.emptyList());
                chartData.put("error", e.getMessage());
            }
            chartDataList.add(chartData);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dashboard", dashboard);
        result.put("charts", chartDataList);
        result.put("chartCount", charts.size());
        return result;
    }
}
