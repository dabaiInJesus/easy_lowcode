package com.dabai.easy_lowcode.etl.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.service.EtlTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;


/**
 * ETL任务控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/etl/task")
@RequiredArgsConstructor
public class EtlTaskController {

    private final EtlTaskService etlTaskService;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    /**
     * 分页查询任务列表
     */
    @GetMapping("/page")
    public Result<PageResult<EtlTask>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<EtlTask> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(EtlTask::getTaskName, keyword)
                   .or()
                   .like(EtlTask::getTaskCode, keyword);
        }
        wrapper.orderByDesc(EtlTask::getCreateTime);

        Page<EtlTask> page = etlTaskService.page(new Page<>(current, size), wrapper);

        // 填充数据源名称
        fillDatasourceNames(page.getRecords());

        PageResult<EtlTask> result = new PageResult<>(
            page.getTotal(), page.getCurrent(), page.getSize(), page.getRecords()
        );
        return Result.success(result);
    }

    /**
     * 获取任务详情
     */
    @GetMapping("/{id}")
    public Result<EtlTask> getById(@PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        fillDatasourceNames(List.of(task));
        return Result.success(task);
    }

    /**
     * 创建任务
     */
    @PostMapping
    public Result<Void> create(@RequestBody EtlTask task) {
        try {
            boolean success = etlTaskService.createTask(task);
            if (success) {
                log.info("创建ETL任务成功: {}", task.getTaskName());
                return Result.success("创建成功");
            }
            return Result.error("创建失败");
        } catch (Exception e) {
            log.error("创建ETL任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 更新任务
     */
    @PutMapping
    public Result<Void> update(@RequestBody EtlTask task) {
        try {
            boolean success = etlTaskService.updateTask(task);
            if (success) {
                return Result.success("更新成功");
            }
            return Result.error("更新失败");
        } catch (Exception e) {
            log.error("更新ETL任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 删除任务
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) {
            return Result.error("任务不存在");
        }
        etlTaskService.removeById(id);
        return Result.success("删除成功");
    }

    /**
     * 执行任务
     */
    @PostMapping("/{id}/execute")
    public Result<Long> execute(@PathVariable Long id) {
        try {
            Long logId = etlTaskService.executeTask(id);
            return Result.success("任务已提交执行", logId);
        } catch (Exception e) {
            log.error("执行ETL任务失败", e);
            return Result.error(e.getMessage());
        }
    }

    /**
     * 停止任务
     */
    @PostMapping("/{id}/stop")
    public Result<Void> stop(@PathVariable Long id) {
        etlTaskService.stopTask(id);
        return Result.success("停止信号已发送");
    }

    /**
     * 测试源数据源连接
     */
    @PostMapping("/test-source/{datasourceId}")
    public Result<Boolean> testSourceConnection(@PathVariable Long datasourceId) {
        boolean ok = etlTaskService.testSourceConnection(datasourceId);
        return ok ? Result.success("连接成功", true) : Result.error("连接失败");
    }

    /**
     * 测试目标数据源连接
     */
    @PostMapping("/test-target/{datasourceId}")
    public Result<Boolean> testTargetConnection(@PathVariable Long datasourceId) {
        boolean ok = etlTaskService.testTargetConnection(datasourceId);
        return ok ? Result.success("连接成功", true) : Result.error("连接失败");
    }

    /**
     * 扫描源表字段
     */
    @GetMapping("/{id}/source-columns")
    public Result<List<Map<String, Object>>> scanSourceColumns(@PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) return Result.error("任务不存在");
        List<Map<String, Object>> columns = etlTaskService.scanSourceColumns(
            task.getSourceDatasourceId(), task.getSourceTable());
        return Result.success(columns);
    }

    /**
     * 扫描目标表字段
     */
    @GetMapping("/{id}/target-columns")
    public Result<List<Map<String, Object>>> scanTargetColumns(@PathVariable Long id) {
        EtlTask task = etlTaskService.getById(id);
        if (task == null) return Result.error("任务不存在");
        List<Map<String, Object>> columns = etlTaskService.scanTargetColumns(
            task.getTargetDatasourceId(), task.getTargetTable());
        return Result.success(columns);
    }

    /**
     * 预览源数据
     */
    @GetMapping("/{id}/preview")
    public Result<List<Map<String, Object>>> previewSourceData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> data = etlTaskService.previewSourceData(id, limit);
        return Result.success(data);
    }

    /**
     * 获取任务执行历史
     */
    @GetMapping("/{id}/history")
    public Result<List<Map<String, Object>>> getHistory(@PathVariable Long id) {
        List<Map<String, Object>> history = etlTaskService.getTaskHistory(id);
        return Result.success(history);
    }

    /**
     * 获取所有数据源列表（用于选择源/目标数据源）
     */
    @GetMapping("/datasources")
    public Result<List<DataSourceConfig>> listDatasources() {
        List<DataSourceConfig> list = dataSourceConfigMapper.selectList(
            new LambdaQueryWrapper<DataSourceConfig>()
                .eq(DataSourceConfig::getStatus, 1)
                .orderByAsc(DataSourceConfig::getName));
        list.forEach(ds -> ds.setPassword("******"));
        return Result.success(list);
    }

    /**
     * 填充数据源名称
     */
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
