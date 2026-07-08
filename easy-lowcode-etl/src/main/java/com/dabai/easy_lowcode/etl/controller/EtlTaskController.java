package com.dabai.easy_lowcode.etl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.result.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.service.EtlTaskService;
import com.dabai.easy_lowcode.etl.service.ScheduleService;
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
 * ETL任务控制器
 */
@Tag(name = "ETL任务管理", description = "ETL任务CRUD、调度管理、执行监控、连接测试")
@Slf4j
@RestController
@RequestMapping("/api/etl/task")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class EtlTaskController {

    private final EtlTaskService etlTaskService;
    private final ScheduleService scheduleService;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    @Operation(summary = "分页查询任务列表", description = "分页查询ETL任务列表，支持关键词搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<EtlTask>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（任务名称或编码）") @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<EtlTask> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(EtlTask::getTaskName, keyword)
                   .or()
                   .like(EtlTask::getTaskCode, keyword);
        }
        wrapper.orderByDesc(EtlTask::getCreateTime);

        Page<EtlTask> page = etlTaskService.page(new Page<>(current, size), wrapper);

        fillDatasourceNames(page.getRecords());

        PageResult<EtlTask> result = new PageResult<>(
            page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize()
        );
        return Result.success(result);
    }

    @Operation(summary = "获取任务详情", description = "根据ID获取ETL任务详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<EtlTask> getById(@Parameter(description = "任务ID") @PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        fillDatasourceNames(List.of(task));
        return Result.success(task);
    }

    @Operation(summary = "创建任务", description = "创建新的ETL任务")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> create(@RequestBody EtlTask task) {
        try {
            boolean success = etlTaskService.createTask(task);
            if (success) {
                return Result.success("创建成功");
            }
            return Result.error("创建失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "更新任务", description = "更新ETL任务配置")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> update(@RequestBody EtlTask task) {
        try {
            boolean success = etlTaskService.updateTask(task);
            if (success) {
                return Result.success("更新成功");
            }
            return Result.error("更新失败");
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "删除任务", description = "删除ETL任务并取消调度")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "任务ID") @PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        scheduleService.cancelTask(id);
        etlTaskService.removeById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "启停调度", description = "开启或关闭任务的定时调度")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @PutMapping("/{id}/schedule")
    public Result<Void> toggleSchedule(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "是否启用调度") @RequestParam boolean enabled) {
        if (enabled) {
            scheduleService.scheduleTask(id);
        } else {
            scheduleService.cancelTask(id);
        }
        return Result.success(enabled ? "调度已开启" : "调度已关闭");
    }

    @Operation(summary = "执行任务", description = "立即执行一次ETL任务")
    @ApiResponse(responseCode = "200", description = "任务已提交")
    @PostMapping("/{id}/execute")
    public Result<Long> execute(@Parameter(description = "任务ID") @PathVariable Long id) {
        try {
            Long logId = etlTaskService.executeTask(id);
            return Result.success("任务已提交执行", logId);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }

    @Operation(summary = "停止任务", description = "停止正在执行的ETL任务")
    @ApiResponse(responseCode = "200", description = "停止信号已发送")
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@Parameter(description = "任务ID") @PathVariable Long id) {
        etlTaskService.stopTask(id);
        return Result.success("停止信号已发送");
    }

    @Operation(summary = "测试源数据源连接", description = "测试任务配置的源数据源连接")
    @ApiResponse(responseCode = "200", description = "测试完成")
    @PostMapping("/test-source/{datasourceId}")
    public Result<Boolean> testSourceConnection(@Parameter(description = "数据源ID") @PathVariable Long datasourceId) {
        boolean ok = etlTaskService.testSourceConnection(datasourceId);
        return ok ? Result.success("连接成功", true) : Result.error("连接失败");
    }

    @Operation(summary = "测试目标数据源连接", description = "测试任务配置的目标数据源连接")
    @ApiResponse(responseCode = "200", description = "测试完成")
    @PostMapping("/test-target/{datasourceId}")
    public Result<Boolean> testTargetConnection(@Parameter(description = "数据源ID") @PathVariable Long datasourceId) {
        boolean ok = etlTaskService.testTargetConnection(datasourceId);
        return ok ? Result.success("连接成功", true) : Result.error("连接失败");
    }

    @Operation(summary = "扫描源表字段", description = "扫描任务源表的字段列表")
    @ApiResponse(responseCode = "200", description = "扫描成功")
    @GetMapping("/{id}/source-columns")
    public Result<List<Map<String, Object>>> scanSourceColumns(@Parameter(description = "任务ID") @PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) return Result.error("任务不存在");
        List<Map<String, Object>> columns = etlTaskService.scanSourceColumns(
            task.getSourceDatasourceId(), task.getSourceTable());
        return Result.success(columns);
    }

    @Operation(summary = "扫描目标表字段", description = "扫描任务目标表的字段列表")
    @ApiResponse(responseCode = "200", description = "扫描成功")
    @GetMapping("/{id}/target-columns")
    public Result<List<Map<String, Object>>> scanTargetColumns(@Parameter(description = "任务ID") @PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) return Result.error("任务不存在");
        List<Map<String, Object>> columns = etlTaskService.scanTargetColumns(
            task.getTargetDatasourceId(), task.getTargetTable());
        return Result.success(columns);
    }

    @Operation(summary = "预览源数据", description = "预览任务源表的数据")
    @ApiResponse(responseCode = "200", description = "预览成功")
    @GetMapping("/{id}/preview")
    public Result<List<Map<String, Object>>> previewSourceData(
            @Parameter(description = "任务ID") @PathVariable Long id,
            @Parameter(description = "限制条数") @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> data = etlTaskService.previewSourceData(id, limit);
        return Result.success(data);
    }

    @Operation(summary = "获取任务执行历史", description = "获取任务的执行历史记录")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}/history")
    public Result<List<Map<String, Object>>> getHistory(@Parameter(description = "任务ID") @PathVariable Long id) {
        List<Map<String, Object>> history = etlTaskService.getTaskHistory(id);
        return Result.success(history);
    }

    @Operation(summary = "获取所有数据源列表", description = "获取所有已启用的数据源（用于选择源/目标数据源）")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/datasources")
    public Result<List<DataSourceConfig>> listDatasources() {
        List<DataSourceConfig> list = dataSourceConfigMapper.selectList(
            new LambdaQueryWrapper<DataSourceConfig>()
                .eq(DataSourceConfig::getStatus, 1)
                .orderByAsc(DataSourceConfig::getName));
        list.forEach(ds -> ds.setPassword("******"));
        return Result.success(list);
    }

    @Operation(summary = "扫描表字段", description = "根据数据源和表名扫描表的字段列表")
    @ApiResponse(responseCode = "200", description = "扫描成功")
    @GetMapping("/scan-columns")
    public Result<List<Map<String, Object>>> scanColumns(
            @Parameter(description = "数据源ID") @RequestParam Long datasourceId,
            @Parameter(description = "表名") @RequestParam String tableName) {
        List<Map<String, Object>> columns = etlTaskService.scanSourceColumns(datasourceId, tableName);
        return Result.success(columns);
    }

    private void fillDatasourceNames(List<EtlTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return;
        for (EtlTask task : tasks) {
            if (task.getSourceDatasourceId() != null) {
                DataSourceConfig ds = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
                if (ds != null) task.setSourceDatasourceName(ds.getName());
            }
            if (task.getTargetDatasourceId() != null) {
                DataSourceConfig ds = dataSourceConfigMapper.selectById(task.getTargetDatasourceId());
                if (ds != null) task.setTargetDatasourceName(ds.getName());
            }
        }
    }
}
