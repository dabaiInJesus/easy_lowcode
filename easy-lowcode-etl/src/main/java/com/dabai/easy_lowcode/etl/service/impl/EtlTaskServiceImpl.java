package com.dabai.easy_lowcode.etl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.entity.EtlTaskLog;
import com.dabai.easy_lowcode.etl.mapper.EtlTaskMapper;
import com.dabai.easy_lowcode.etl.service.EtlTaskLogService;
import com.dabai.easy_lowcode.etl.service.EtlTaskService;
import com.dabai.easy_lowcode.etl.service.ScheduleService;
import com.dabai.easy_lowcode.etl.service.TaskExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ETL 任务服务实现（重构后）
 * 仅保留 CRUD 和调度协调，执行引擎、转换规则、任务状态均已拆分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlTaskServiceImpl extends ServiceImpl<EtlTaskMapper, EtlTask> implements EtlTaskService {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final EtlTaskLogService etlTaskLogService;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final ScheduleService scheduleService;
    private final TaskExecutor taskExecutor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTask(EtlTask task) {
        validateTaskInput(task, null);

        DataSourceConfig sourceDs = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
        if (sourceDs == null) throw new BusinessException("源数据源不存在");
        DataSourceConfig targetDs = dataSourceConfigMapper.selectById(task.getTargetDatasourceId());
        if (targetDs == null) throw new BusinessException("目标数据源不存在");

        if ("TABLE".equals(task.getReadMode()) && (task.getSourceTable() == null || task.getSourceTable().trim().isEmpty())) {
            throw new BusinessException("TABLE模式下源表名不能为空");
        }

        Long count = baseMapper.selectCount(new LambdaQueryWrapper<EtlTask>()
                .eq(EtlTask::getTaskCode, task.getTaskCode()));
        if (count > 0) throw new BusinessException("任务编码已存在: " + task.getTaskCode());

        applyDefaults(task);

        boolean saved = save(task);
        if (saved && task.getStatus() == 1 && !"MANUAL".equals(task.getScheduleType())) {
            scheduleService.scheduleTask(task.getId());
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(EtlTask task) {
        if (task.getTaskCode() != null) {
            Long count = baseMapper.selectCount(new LambdaQueryWrapper<EtlTask>()
                    .eq(EtlTask::getTaskCode, task.getTaskCode())
                    .ne(EtlTask::getId, task.getId()));
            if (count > 0) throw new BusinessException("任务编码已存在: " + task.getTaskCode());
        }

        LambdaUpdateWrapper<EtlTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EtlTask::getId, task.getId());

        if (task.getTaskName() != null) wrapper.set(EtlTask::getTaskName, task.getTaskName());
        if (task.getTaskCode() != null) wrapper.set(EtlTask::getTaskCode, task.getTaskCode());
        if (task.getSourceDatasourceId() != null) wrapper.set(EtlTask::getSourceDatasourceId, task.getSourceDatasourceId());
        if (task.getSourceTable() != null) wrapper.set(EtlTask::getSourceTable, task.getSourceTable());
        if (task.getSourceSql() != null) wrapper.set(EtlTask::getSourceSql, task.getSourceSql());
        if (task.getReadMode() != null) wrapper.set(EtlTask::getReadMode, task.getReadMode());
        if (task.getTargetDatasourceId() != null) wrapper.set(EtlTask::getTargetDatasourceId, task.getTargetDatasourceId());
        if (task.getTargetTable() != null) wrapper.set(EtlTask::getTargetTable, task.getTargetTable());
        if (task.getWriteMode() != null) wrapper.set(EtlTask::getWriteMode, task.getWriteMode());
        if (task.getFieldMapping() != null) wrapper.set(EtlTask::getFieldMapping, task.getFieldMapping());
        if (task.getTransformRules() != null) wrapper.set(EtlTask::getTransformRules, task.getTransformRules());
        if (task.getScheduleType() != null) wrapper.set(EtlTask::getScheduleType, task.getScheduleType());
        if (task.getCronExpression() != null) wrapper.set(EtlTask::getCronExpression, task.getCronExpression());
        if (task.getIntervalSeconds() != null) wrapper.set(EtlTask::getIntervalSeconds, task.getIntervalSeconds());
        if (task.getBatchSize() != null) wrapper.set(EtlTask::getBatchSize, task.getBatchSize());
        if (task.getSkipError() != null) wrapper.set(EtlTask::getSkipError, task.getSkipError());
        if (task.getStatus() != null) wrapper.set(EtlTask::getStatus, task.getStatus());
        if (task.getRemark() != null) wrapper.set(EtlTask::getRemark, task.getRemark());

        boolean updated = update(wrapper);
        if (updated) {
            if (task.getStatus() != null && task.getStatus() != 1) {
                scheduleService.cancelTask(task.getId());
            } else if (task.getScheduleType() != null || task.getStatus() != null) {
                scheduleService.refresh();
            }
        }
        return updated;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executeTask(Long taskId) {
        EtlTask task = baseMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在，ID: " + taskId);
        if (task.getStatus() != 1) throw new BusinessException("任务未启用，无法执行");

        DataSourceConfig sourceDs = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
        DataSourceConfig targetDs = dataSourceConfigMapper.selectById(task.getTargetDatasourceId());
        if (sourceDs == null || targetDs == null) throw new BusinessException("数据源不存在");

        EtlTaskLog logRecord = new EtlTaskLog();
        logRecord.setTaskId(taskId);
        logRecord.setExecStatus("RUNNING");
        logRecord.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        logRecord.setReadCount(0L);
        logRecord.setWriteCount(0L);
        logRecord.setSkipCount(0L);
        etlTaskLogService.recordLog(logRecord);

        taskExecutor.executeAsync(task, sourceDs, targetDs, logRecord.getId());
        return logRecord.getId();
    }

    @Override
    public boolean stopTask(Long taskId) {
        return taskExecutor.stop(taskId);
    }

    @Override
    public boolean testSourceConnection(Long datasourceId) {
        return testConnection(datasourceId);
    }

    @Override
    public boolean testTargetConnection(Long datasourceId) {
        return testConnection(datasourceId);
    }

    @Override
    public List<Map<String, Object>> scanSourceColumns(Long datasourceId, String tableName) {
        DataSourceConfig ds = dataSourceConfigMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException("数据源不存在");
        return scanColumns(ds, tableName);
    }

    @Override
    public List<Map<String, Object>> scanTargetColumns(Long datasourceId, String tableName) {
        DataSourceConfig ds = dataSourceConfigMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException("数据源不存在");
        return scanColumns(ds, tableName);
    }

    @Override
    public List<Map<String, Object>> previewSourceData(Long taskId, int limit) {
        EtlTask task = baseMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (limit <= 0 || limit > 100) limit = 10;
        DataSourceConfig ds = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
        validateTableName(task.getSourceTable());
        try (Connection conn = getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + task.getSourceTable() + " LIMIT " + limit)) {
            List<Map<String, Object>> data = new ArrayList<>();
            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                data.add(row);
            }
            return data;
        } catch (Exception e) {
            throw new BusinessException("预览数据失败: " + e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> getTaskHistory(Long taskId) {
        List<EtlTaskLog> logs = etlTaskLogService.getLogsByTaskId(taskId, 20);
        return logs.stream().map(log -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", log.getId());
            map.put("execStatus", log.getExecStatus());
            map.put("startTime", log.getStartTime());
            map.put("endTime", log.getEndTime());
            map.put("readCount", log.getReadCount());
            map.put("writeCount", log.getWriteCount());
            map.put("skipCount", log.getSkipCount());
            map.put("errorMessage", log.getErrorMessage());
            return map;
        }).collect(Collectors.toList());
    }

    private boolean testConnection(Long datasourceId) {
        DataSourceConfig ds = dataSourceConfigMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException("数据源不存在");
        try (Connection conn = getConnection(ds)) {
            return conn.isValid(5);
        } catch (Exception e) {
            throw new BusinessException("连接测试失败: " + e.getMessage());
        }
    }

    private List<Map<String, Object>> scanColumns(DataSourceConfig ds, String tableName) {
        List<Map<String, Object>> columns = new ArrayList<>();
        validateTableName(tableName);
        try (Connection conn = getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " WHERE 1=0")) {
            ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("columnName", meta.getColumnName(i));
                col.put("dataType", meta.getColumnTypeName(i));
                col.put("nullable", meta.isNullable(i));
                columns.add(col);
            }
        } catch (Exception e) {
            log.error("扫描表字段失败: {}", e.getMessage());
            throw new BusinessException("扫描表字段失败: " + e.getMessage());
        }
        return columns;
    }

    private Connection getConnection(DataSourceConfig ds) throws Exception {
        String pwd = decryptPwd(ds.getPassword());
        Class.forName(ds.getDriverClassName());
        return DriverManager.getConnection(ds.getUrl(), ds.getUsername(), pwd);
    }

    private String decryptPwd(String encrypted) {
        try {
            return EncryptUtil.decrypt(encrypted);
        } catch (Exception e) {
            return encrypted;
        }
    }

    private static void validateTableName(String tableName) {
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new BusinessException("非法表名: " + tableName);
        }
    }

    private void validateTaskInput(EtlTask task, Long existingId) {
        if (task.getTaskName() == null || task.getTaskName().trim().isEmpty()) {
            throw new BusinessException("任务名称不能为空");
        }
        if (task.getTaskCode() == null || task.getTaskCode().trim().isEmpty()) {
            throw new BusinessException("任务编码不能为空");
        }
        if (task.getSourceDatasourceId() == null) {
            throw new BusinessException("源数据源不能为空");
        }
        if (task.getTargetDatasourceId() == null) {
            throw new BusinessException("目标数据源不能为空");
        }
        if (task.getTargetTable() == null || task.getTargetTable().trim().isEmpty()) {
            throw new BusinessException("目标表名不能为空");
        }
    }

    private void applyDefaults(EtlTask task) {
        if (task.getReadMode() == null) task.setReadMode("TABLE");
        if (task.getWriteMode() == null) task.setWriteMode("INSERT");
        if (task.getScheduleType() == null) task.setScheduleType("MANUAL");
        if (task.getBatchSize() == null) task.setBatchSize(1000);
        if (task.getStatus() == null) task.setStatus(1);
        if (task.getSkipError() == null) task.setSkipError(0);
    }
}
