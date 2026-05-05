package com.dabai.easy_lowcode.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;
import com.dabai.easy_lowcode.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    // ========== 大屏管理 ==========

    @GetMapping("/page")
    public Result<PageResult<Dashboard>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(Dashboard::getName, keyword)
                   .or().like(Dashboard::getTitle, keyword);
        }
        if (status != null) {
            wrapper.eq(Dashboard::getStatus, status);
        }
        wrapper.orderByDesc(Dashboard::getSortOrder)
               .orderByDesc(Dashboard::getCreateTime);

        Page<Dashboard> page = dashboardService.page(new Page<>(current, size), wrapper);

        // 填充图表数量
        for (Dashboard d : page.getRecords()) {
            long count = dashboardService.getCharts(d.getId()).size();
            d.setChartCount((int) count);
        }

        PageResult<Dashboard> result = new PageResult<>(
            page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords());
        return Result.success(result);
    }

    @GetMapping("/list")
    public Result<List<Dashboard>> list(@RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Dashboard::getStatus, status);
        wrapper.orderByAsc(Dashboard::getSortOrder);
        return Result.success(dashboardService.list(wrapper));
    }

    @GetMapping("/{id}")
    public Result<Dashboard> getById(@PathVariable Long id) {
        Dashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) return Result.error("大屏不存在");
        return Result.success(dashboard);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Dashboard dashboard) {
        try {
            dashboardService.createDashboard(dashboard);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping
    public Result<Void> update(@RequestBody Dashboard dashboard) {
        try {
            dashboardService.updateDashboard(dashboard);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        Dashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) return Result.error("大屏不存在");
        // 删除大屏下的所有图表
        List<DashboardChart> charts = dashboardService.getCharts(id);
        for (DashboardChart chart : charts) {
            dashboardService.removeChart(chart.getId());
        }
        dashboardService.removeById(id);
        return Result.success("删除成功");
    }

    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        try {
            dashboardService.publishDashboard(id);
            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/copy")
    public Result<Long> copy(@PathVariable Long id) {
        try {
            Long newId = dashboardService.copyDashboard(id);
            return Result.success("复制成功", newId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        Dashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) return Result.error("大屏不存在");
        dashboard.setStatus(2);
        dashboardService.updateById(dashboard);
        return Result.success("已下线");
    }

    // ========== 图表管理 ==========

    @GetMapping("/{dashboardId}/charts")
    public Result<List<DashboardChart>> getCharts(@PathVariable Long dashboardId) {
        List<DashboardChart> charts = dashboardService.getCharts(dashboardId);
        return Result.success(charts);
    }

    @PostMapping("/chart")
    public Result<Void> addChart(@RequestBody DashboardChart chart) {
        try {
            dashboardService.addChart(chart);
            return Result.success("添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/chart")
    public Result<Void> updateChart(@RequestBody DashboardChart chart) {
        try {
            dashboardService.updateChart(chart);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @DeleteMapping("/chart/{chartId}")
    public Result<Void> removeChart(@PathVariable Long chartId) {
        try {
            dashboardService.removeChart(chartId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @PutMapping("/charts/positions")
    public Result<Void> updatePositions(@RequestBody List<DashboardChart> charts) {
        dashboardService.updateChartPositions(charts);
        return Result.success("位置更新成功");
    }

    @GetMapping("/chart/{chartId}/data")
    public Result<List<Map<String, Object>>> queryChartData(
            @PathVariable Long chartId,
            @RequestParam(required = false) Map<String, Object> params) {
        try {
            if (params == null) params = new java.util.HashMap<>();
            List<Map<String, Object>> data = dashboardService.executeChartQuery(chartId, params);
            return Result.success(data);
        } catch (Exception e) {
            log.error("图表数据查询失败: chartId={}", chartId, e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> previewDashboard(@PathVariable Long id) {
        try {
            Map<String, Object> preview = dashboardService.previewDashboard(id);
            return Result.success(preview);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
