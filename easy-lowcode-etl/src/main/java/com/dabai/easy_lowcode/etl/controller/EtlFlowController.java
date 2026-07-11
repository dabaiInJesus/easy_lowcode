package com.dabai.easy_lowcode.etl.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.etl.entity.EtlFlow;
import com.dabai.easy_lowcode.etl.entity.EtlFlowExecution;
import com.dabai.easy_lowcode.etl.service.EtlFlowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 可视化流程控制器
 */
@Tag(name = "流程管理", description = "可视化数据采集流程的CRUD和执行")
@Slf4j
@RestController
@RequestMapping("/api/etl/flow")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EtlFlowController {

    private final EtlFlowService flowService;

    @Operation(summary = "分页查询流程列表")
    @GetMapping("/page")
    public Result<PageResult<EtlFlow>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        Page<EtlFlow> pageResult = flowService.pageFlow(new Page<>(current, size), keyword, status);
        return Result.success(new PageResult<>(pageResult.getRecords(), pageResult.getTotal(),
                pageResult.getCurrent(), pageResult.getSize()));
    }

    @Operation(summary = "获取流程详情")
    @GetMapping("/{id}")
    public Result<EtlFlow> getById(@Parameter(description = "流程ID") @PathVariable Long id) {
        return Result.success(flowService.getFlowDetail(id));
    }

    @Operation(summary = "创建流程")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> create(@RequestBody EtlFlow flow) {
        flowService.saveFlow(flow);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新流程")
    @PutMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> update(@RequestBody EtlFlow flow) {
        flowService.updateFlow(flow);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除流程")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@Parameter(description = "流程ID") @PathVariable Long id) {
        flowService.deleteFlow(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "执行流程")
    @PostMapping("/{id}/execute")
    public Result<Long> execute(@Parameter(description = "流程ID") @PathVariable Long id) {
        Long executionId = flowService.executeFlow(id);
        return Result.success("执行已启动", executionId);
    }

    @Operation(summary = "停止流程")
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@Parameter(description = "流程ID") @PathVariable Long id) {
        flowService.stopFlow(id);
        return Result.success("已停止");
    }

    @Operation(summary = "获取执行历史")
    @GetMapping("/{id}/history")
    public Result<List<EtlFlowExecution>> history(
            @Parameter(description = "流程ID") @PathVariable Long id,
            @Parameter(description = "限制条数") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(flowService.getExecutionHistory(id, limit));
    }

    @Operation(summary = "获取执行详情")
    @GetMapping("/execution/{executionId}")
    public Result<EtlFlowExecution> executionDetail(
            @Parameter(description = "执行记录ID") @PathVariable Long executionId) {
        return Result.success(flowService.getExecutionDetail(executionId));
    }

    @Operation(summary = "配置流程调度")
    @PutMapping("/{id}/schedule")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> updateSchedule(
            @Parameter(description = "流程ID") @PathVariable Long id,
            @RequestBody java.util.Map<String, Object> scheduleConfig) {
        String scheduleType = (String) scheduleConfig.getOrDefault("scheduleType", "MANUAL");
        String cronExpression = (String) scheduleConfig.get("cronExpression");
        Integer intervalSeconds = scheduleConfig.get("intervalSeconds") != null
                ? ((Number) scheduleConfig.get("intervalSeconds")).intValue() : null;

        EtlFlow flow = new EtlFlow();
        flow.setId(id);
        flow.setScheduleType(scheduleType);
        flow.setCronExpression(cronExpression);
        flow.setIntervalSeconds(intervalSeconds);
        flowService.updateFlow(flow);
        return Result.success("调度配置已更新");
    }

    @Operation(summary = "获取调度状态")
    @GetMapping("/{id}/schedule/status")
    public Result<java.util.Map<String, Object>> getScheduleStatus(
            @Parameter(description = "流程ID") @PathVariable Long id) {
        EtlFlow flow = flowService.getById(id);
        if (flow == null) return Result.error("流程不存在");

        java.util.Map<String, Object> status = new java.util.HashMap<>();
        status.put("scheduleType", flow.getScheduleType());
        status.put("cronExpression", flow.getCronExpression());
        status.put("intervalSeconds", flow.getIntervalSeconds());
        status.put("isRunning", flowService.isFlowRunning(id));
        return Result.success(status);
    }
}
