package com.dabai.easy_lowcode.dashboard.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;
import com.dabai.easy_lowcode.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 数据大屏控制器
 */
@Tag(name = "数据大屏管理", description = "大屏CRUD、图表管理、数据查询及发布")
@Slf4j
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "分页查询大屏列表", description = "分页查询大屏列表，支持关键词和状态筛选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<Dashboard>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态（0草稿/1已发布/2已下线）") @RequestParam(required = false) Integer status) {
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

        for (Dashboard d : page.getRecords()) {
            long count = dashboardService.getCharts(d.getId()).size();
            d.setChartCount((int) count);
        }

        PageResult<Dashboard> result = new PageResult<>(
            page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize());
        return Result.success(result);
    }

    @Operation(summary = "获取大屏列表", description = "获取大屏列表（不分页）")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<Dashboard>> list(
            @Parameter(description = "状态筛选") @RequestParam(required = false) Integer status) {
        LambdaQueryWrapper<Dashboard> wrapper = new LambdaQueryWrapper<>();
        if (status != null) wrapper.eq(Dashboard::getStatus, status);
        wrapper.orderByAsc(Dashboard::getSortOrder);
        return Result.success(dashboardService.list(wrapper));
    }

    @Operation(summary = "获取大屏详情", description = "根据ID获取大屏详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<Dashboard> getById(@Parameter(description = "大屏ID") @PathVariable Long id) {
        Dashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) return Result.error("大屏不存在");
        return Result.success(dashboard);
    }

    @Operation(summary = "创建大屏", description = "创建新的大屏")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> create(@RequestBody Dashboard dashboard) {
        try {
            dashboardService.createDashboard(dashboard);
            return Result.success("创建成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新大屏", description = "更新大屏信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> update(@RequestBody Dashboard dashboard) {
        try {
            dashboardService.updateDashboard(dashboard);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除大屏", description = "删除大屏及其所有图表")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@Parameter(description = "大屏ID") @PathVariable Long id) {
        Dashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) return Result.error("大屏不存在");
        List<DashboardChart> charts = dashboardService.getCharts(id);
        for (DashboardChart chart : charts) {
            dashboardService.removeChart(chart.getId());
        }
        dashboardService.removeById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "发布大屏", description = "发布大屏使其对外可见")
    @ApiResponse(responseCode = "200", description = "发布成功")
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@Parameter(description = "大屏ID") @PathVariable Long id) {
        try {
            dashboardService.publishDashboard(id);
            return Result.success("发布成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "复制大屏", description = "复制一个大屏及其配置")
    @ApiResponse(responseCode = "200", description = "复制成功")
    @PostMapping("/{id}/copy")
    public Result<Long> copy(@Parameter(description = "大屏ID") @PathVariable Long id) {
        try {
            Long newId = dashboardService.copyDashboard(id);
            return Result.success("复制成功", newId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "下线大屏", description = "将已发布的大屏下线")
    @ApiResponse(responseCode = "200", description = "下线成功")
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@Parameter(description = "大屏ID") @PathVariable Long id) {
        Dashboard dashboard = dashboardService.getById(id);
        if (dashboard == null) return Result.error("大屏不存在");
        dashboard.setStatus(2);
        dashboardService.updateById(dashboard);
        return Result.success("已下线");
    }

    @Operation(summary = "获取大屏图表列表", description = "获取指定大屏下的所有图表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{dashboardId}/charts")
    public Result<List<DashboardChart>> getCharts(@Parameter(description = "大屏ID") @PathVariable Long dashboardId) {
        List<DashboardChart> charts = dashboardService.getCharts(dashboardId);
        return Result.success(charts);
    }

    @Operation(summary = "添加图表", description = "向大屏添加图表组件")
    @ApiResponse(responseCode = "200", description = "添加成功")
    @PostMapping("/chart")
    public Result<Void> addChart(@RequestBody DashboardChart chart) {
        try {
            dashboardService.addChart(chart);
            return Result.success("添加成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新图表", description = "更新图表配置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/chart")
    public Result<Void> updateChart(@RequestBody DashboardChart chart) {
        try {
            dashboardService.updateChart(chart);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除图表", description = "从大屏中删除图表组件")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/chart/{chartId}")
    public Result<Void> removeChart(@Parameter(description = "图表ID") @PathVariable Long chartId) {
        try {
            dashboardService.removeChart(chartId);
            return Result.success("删除成功");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新图表位置", description = "批量更新图表在大屏中的位置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/charts/positions")
    public Result<Void> updatePositions(@RequestBody List<DashboardChart> charts) {
        dashboardService.updateChartPositions(charts);
        return Result.success("位置更新成功");
    }

    @Operation(summary = "查询图表数据", description = "执行图表配置的查询获取数据")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/chart/{chartId}/data")
    public Result<List<Map<String, Object>>> queryChartData(
            @Parameter(description = "图表ID") @PathVariable Long chartId,
            @Parameter(description = "查询参数") @RequestParam(required = false) Map<String, Object> params) {
        try {
            if (params == null) params = new java.util.HashMap<>();
            List<Map<String, Object>> data = dashboardService.executeChartQuery(chartId, params);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("查询失败: " + e.getMessage());
        }
    }

    @Operation(summary = "预览大屏", description = "获取大屏预览数据（包含所有图表配置）")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}/preview")
    public Result<Map<String, Object>> previewDashboard(@Parameter(description = "大屏ID") @PathVariable Long id) {
        try {
            Map<String, Object> preview = dashboardService.previewDashboard(id);
            return Result.success(preview);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
