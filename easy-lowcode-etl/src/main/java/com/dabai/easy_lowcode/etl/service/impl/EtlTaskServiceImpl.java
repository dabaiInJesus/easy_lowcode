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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * ETL任务服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlTaskServiceImpl extends ServiceImpl<EtlTaskMapper, EtlTask> implements EtlTaskService {

    private final EtlTaskLogService etlTaskLogService;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTask(EtlTask task) {
        // 验证必填字段
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

        // 验证数据源存在
        if (dataSourceConfigMapper.selectById(task.getSourceDatasourceId()) == null) {
            throw new BusinessException("源数据源不存在");
        }
        if (dataSourceConfigMapper.selectById(task.getTargetDatasourceId()) == null) {
            throw new BusinessException("目标数据源不存在");
        }

        // TABLE模式需要源表名
        if ("TABLE".equals(task.getReadMode()) && (task.getSourceTable() == null || task.getSourceTable().trim().isEmpty())) {
            throw new BusinessException("全表读取模式需要指定源表名");
        }
        // SQL模式需要SQL语句
        if ("SQL".equals(task.getReadMode()) && (task.getSourceSql() == null || task.getSourceSql().trim().isEmpty())) {
            throw new BusinessException("自定义SQL模式需要输入SQL语句");
        }

        // 检查编码唯一性
        LambdaQueryWrapper<EtlTask> checkWrapper = new LambdaQueryWrapper<>();
        checkWrapper.eq(EtlTask::getTaskCode, task.getTaskCode());
        if (this.count(checkWrapper) > 0) {
            throw new BusinessException("任务编码已存在: " + task.getTaskCode());
        }

        // 设置默认值
        if (task.getBatchSize() == null) task.setBatchSize(1000);
        if (task.getStatus() == null) task.setStatus(1);
        if (task.getScheduleType() == null) task.setScheduleType("MANUAL");
        if (task.getWriteMode() == null) task.setWriteMode("INSERT");
        if (task.getReadMode() == null) task.setReadMode("TABLE");

        return this.save(task);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(EtlTask task) {
        EtlTask existing = this.getById(task.getId());
        if (existing == null) {
            throw new BusinessException("任务不存在");
        }

        // 如果修改了编码，检查唯一性
        if (task.getTaskCode() != null && !task.getTaskCode().equals(existing.getTaskCode())) {
            LambdaQueryWrapper<EtlTask> checkWrapper = new LambdaQueryWrapper<>();
            checkWrapper.eq(EtlTask::getTaskCode, task.getTaskCode());
            checkWrapper.ne(EtlTask::getId, task.getId());
            if (this.count(checkWrapper) > 0) {
                throw new BusinessException("任务编码已存在: " + task.getTaskCode());
            }
        }

        LambdaUpdateWrapper<EtlTask> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(EtlTask::getId, task.getId());

        if (task.getTaskName() != null) updateWrapper.set(EtlTask::getTaskName, task.getTaskName());
        if (task.getTaskCode() != null) updateWrapper.set(EtlTask::getTaskCode, task.getTaskCode());
        if (task.getSourceDatasourceId() != null) updateWrapper.set(EtlTask::getSourceDatasourceId, task.getSourceDatasourceId());
        if (task.getSourceTable() != null) updateWrapper.set(EtlTask::getSourceTable, task.getSourceTable());
        if (task.getSourceSql() != null) updateWrapper.set(EtlTask::getSourceSql, task.getSourceSql());
        if (task.getReadMode() != null) updateWrapper.set(EtlTask::getReadMode, task.getReadMode());
        if (task.getTargetDatasourceId() != null) updateWrapper.set(EtlTask::getTargetDatasourceId, task.getTargetDatasourceId());
        if (task.getTargetTable() != null) updateWrapper.set(EtlTask::getTargetTable, task.getTargetTable());
        if (task.getWriteMode() != null) updateWrapper.set(EtlTask::getWriteMode, task.getWriteMode());
        if (task.getFieldMapping() != null) updateWrapper.set(EtlTask::getFieldMapping, task.getFieldMapping());
        if (task.getTransformRules() != null) updateWrapper.set(EtlTask::getTransformRules, task.getTransformRules());
        if (task.getScheduleType() != null) updateWrapper.set(EtlTask::getScheduleType, task.getScheduleType());
        if (task.getCronExpression() != null) updateWrapper.set(EtlTask::getCronExpression, task.getCronExpression());
        if (task.getIntervalSeconds() != null) updateWrapper.set(EtlTask::getIntervalSeconds, task.getIntervalSeconds());
        if (task.getBatchSize() != null) updateWrapper.set(EtlTask::getBatchSize, task.getBatchSize());
        if (task.getSkipError() != null) updateWrapper.set(EtlTask::getSkipError, task.getSkipError());
        if (task.getRemark() != null) updateWrapper.set(EtlTask::getRemark, task.getRemark());
        // status 允许设置为0
        updateWrapper.set(EtlTask::getStatus, task.getStatus());

        return this.update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long executeTask(Long taskId) {
        EtlTask task = this.getById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }
        if (task.getStatus() == null || task.getStatus() != 1) {
            throw new BusinessException("任务已禁用，无法执行");
        }

        // 检查源、目标数据源
        DataSourceConfig sourceDs = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
        DataSourceConfig targetDs = dataSourceConfigMapper.selectById(task.getTargetDatasourceId());
        if (sourceDs == null) throw new BusinessException("源数据源不存在");
        if (targetDs == null) throw new BusinessException("目标数据源不存在");

        // 创建执行日志
        EtlTaskLog taskLog = new EtlTaskLog();
        taskLog.setTaskId(taskId);
        taskLog.setExecStatus("RUNNING");
        taskLog.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
        etlTaskLogService.recordLog(taskLog);
        Long logId = taskLog.getId();

        // 异步执行ETL
        asyncExecuteEtl(task, sourceDs, targetDs, logId);

        return logId;
    }

    /**
     * 异步执行ETL
     */
    @Async("etlExecutor")
    public void asyncExecuteEtl(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId) {
        try {
            executeEtl(task, sourceDs, targetDs, logId);
        } catch (Exception e) {
            log.error("ETL任务执行异常: taskId={}", task.getId(), e);
            etlTaskLogService.updateStatus(logId, "FAILED", e.getMessage());
        }
    }

    /**
     * 执行ETL核心逻辑
     */
    private void executeEtl(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId) {
        Connection sourceConn = null;
        Connection targetConn = null;
        Statement sourceStmt = null;
        Statement targetStmt = null;
        ResultSet rs = null;

        try {
            // 解密密码
            String sourcePwd = decryptPwd(sourceDs.getPassword());
            String targetPwd = decryptPwd(targetDs.getPassword());

            // 连接源数据源
            Class.forName(sourceDs.getDriverClassName());
            sourceConn = DriverManager.getConnection(sourceDs.getUrl(), sourceDs.getUsername(), sourcePwd);
            sourceStmt = sourceConn.createStatement();

            // 连接目标数据源
            Class.forName(targetDs.getDriverClassName());
            targetConn = DriverManager.getConnection(targetDs.getUrl(), targetDs.getUsername(), targetPwd);
            targetStmt = targetConn.createStatement();

            // 构建源查询SQL
            String sourceQuery;
            if ("SQL".equals(task.getReadMode()) && task.getSourceSql() != null) {
                sourceQuery = task.getSourceSql();
            } else {
                sourceQuery = "SELECT * FROM " + task.getSourceTable();
            }

            log.info("ETL执行查询: {}", sourceQuery);
            rs = sourceStmt.executeQuery(sourceQuery);

            // 获取列信息
            int columnCount = rs.getMetaData().getColumnCount();
            List<String> columnNames = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                columnNames.add(rs.getMetaData().getColumnName(i));
            }

            // 解析字段映射
            Map<String, String> fieldMap = parseFieldMapping(task.getFieldMapping(), columnNames);

            // 分批处理
            List<List<Object>> batch = new ArrayList<>();
            long readCount = 0;
            long writeCount = 0;
            long skipCount = 0;
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;

            while (rs.next()) {
                readCount++;
                List<Object> row = new ArrayList<>();
                boolean skipRow = false;

                for (String colName : columnNames) {
                    Object value = rs.getObject(colName);
                    // 应用转换规则（简化版，实际应解析transformRules）
                    row.add(value);
                }

                if (!skipRow) {
                    batch.add(row);
                } else {
                    skipCount++;
                }

                // 达到批次大小，写入目标
                if (batch.size() >= batchSize) {
                    writeCount += batchWrite(targetStmt, task.getTargetTable(), columnNames, batch, task.getWriteMode());
                    batch.clear();
                }
            }

            // 写入剩余数据
            if (!batch.isEmpty()) {
                writeCount += batchWrite(targetStmt, task.getTargetTable(), columnNames, batch, task.getWriteMode());
            }

            log.info("ETL执行完成: 读取={}, 写入={}, 跳过={}", readCount, writeCount, skipCount);

            // 更新日志
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            LambdaUpdateWrapper<EtlTaskLog> logUpdate = new LambdaUpdateWrapper<>();
            logUpdate.eq(EtlTaskLog::getId, logId)
                     .set(EtlTaskLog::getExecStatus, "SUCCESS")
                     .set(EtlTaskLog::getEndTime, sdf.format(new Date()))
                     .set(EtlTaskLog::getReadCount, readCount)
                     .set(EtlTaskLog::getWriteCount, writeCount)
                     .set(EtlTaskLog::getSkipCount, skipCount);
            etlTaskLogService.update(logUpdate);

        } catch (Exception e) {
            log.error("ETL执行出错", e);
            throw new RuntimeException("ETL执行失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(sourceStmt);
            closeQuietly(targetStmt);
            closeQuietly(sourceConn);
            closeQuietly(targetConn);
        }
    }

    private String decryptPwd(String encrypted) {
        try {
            return EncryptUtil.decrypt(encrypted);
        } catch (Exception e) {
            return encrypted;
        }
    }

    private Map<String, String> parseFieldMapping(String fieldMappingJson, List<String> sourceColumns) {
        Map<String, String> map = new LinkedHashMap<>();
        if (fieldMappingJson != null && !fieldMappingJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                List<Map<String, String>> mappings = mapper.readValue(fieldMappingJson,
                    mapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                for (Map<String, String> m : mappings) {
                    map.put(m.get("source"), m.get("target"));
                }
            } catch (Exception e) {
                log.warn("解析字段映射失败，使用全量字段映射: {}", e.getMessage());
                for (String col : sourceColumns) {
                    map.put(col, col);
                }
            }
        } else {
            for (String col : sourceColumns) {
                map.put(col, col);
            }
        }
        return map;
    }

    private long batchWrite(Statement stmt, String targetTable, List<String> columnNames,
                            List<List<Object>> batch, String writeMode) throws Exception {
        StringBuilder sql = new StringBuilder();
        if ("TRUNCATE".equalsIgnoreCase(writeMode)) {
            stmt.execute("TRUNCATE TABLE " + targetTable);
        }

        // 构建批量INSERT
        StringBuilder columns = new StringBuilder();
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < columnNames.size(); i++) {
            if (i > 0) {
                columns.append(", ");
                values.append(", ");
            }
            columns.append(columnNames.get(i));
            values.append("?");
        }

        // 使用逐行INSERT + 批量提交
        long count = 0;
        for (List<Object> row : batch) {
            StringBuilder rowSql = new StringBuilder();
            rowSql.append("INSERT INTO ").append(targetTable)
                  .append(" (").append(columns).append(") VALUES (");
            for (int i = 0; i < row.size(); i++) {
                if (i > 0) rowSql.append(", ");
                Object val = row.get(i);
                if (val == null) {
                    rowSql.append("NULL");
                } else if (val instanceof Number) {
                    rowSql.append(val);
                } else {
                    rowSql.append("'").append(val.toString().replace("'", "''")).append("'");
                }
            }
            rowSql.append(")");
            stmt.execute(rowSql.toString());
            count++;
        }
        return count;
    }

    @Override
    public boolean stopTask(Long taskId) {
        // 实际项目中应该通过JobExecution ID来停止
        log.warn("停止任务功能需要结合Spring Batch的JobOperator实现: taskId={}", taskId);
        return true;
    }

    @Override
    public boolean testSourceConnection(Long datasourceId) {
        return testConnection(datasourceId);
    }

    @Override
    public boolean testTargetConnection(Long datasourceId) {
        return testConnection(datasourceId);
    }

    private boolean testConnection(Long datasourceId) {
        DataSourceConfig ds = dataSourceConfigMapper.selectById(datasourceId);
        if (ds == null) return false;
        try {
            String pwd = decryptPwd(ds.getPassword());
            Class.forName(ds.getDriverClassName());
            Connection conn = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), pwd);
            conn.close();
            return true;
        } catch (Exception e) {
            log.error("数据源连接测试失败: id={}", datasourceId, e);
            return false;
        }
    }

    @Override
    public List<Map<String, Object>> scanSourceColumns(Long datasourceId, String tableName) {
        return scanColumns(datasourceId, tableName);
    }

    @Override
    public List<Map<String, Object>> scanTargetColumns(Long datasourceId, String tableName) {
        return scanColumns(datasourceId, tableName);
    }

    private List<Map<String, Object>> scanColumns(Long datasourceId, String tableName) {
        DataSourceConfig ds = dataSourceConfigMapper.selectById(datasourceId);
        if (ds == null) throw new BusinessException("数据源不存在");
        List<Map<String, Object>> columns = new ArrayList<>();
        try {
            String pwd = decryptPwd(ds.getPassword());
            Connection conn = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), pwd);
            // 通用列查询（简化版）
            String sql = "SELECT * FROM " + tableName + " WHERE 1=0";
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                Map<String, Object> col = new HashMap<>();
                col.put("columnName", meta.getColumnName(i));
                col.put("dataType", meta.getColumnTypeName(i));
                col.put("nullable", meta.isNullable(i));
                columns.add(col);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("扫描表结构失败", e);
            throw new BusinessException("扫描表结构失败: " + e.getMessage());
        }
        return columns;
    }

    @Override
    public List<Map<String, Object>> previewSourceData(Long taskId, int limit) {
        EtlTask task = this.getById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        DataSourceConfig ds = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
        if (ds == null) throw new BusinessException("源数据源不存在");

        List<Map<String, Object>> result = new ArrayList<>();
        try {
            String pwd = decryptPwd(ds.getPassword());
            Connection conn = DriverManager.getConnection(ds.getUrl(), ds.getUsername(), pwd);
            String sql;
            if ("SQL".equals(task.getReadMode()) && task.getSourceSql() != null) {
                sql = "SELECT * FROM (" + task.getSourceSql() + ") t LIMIT " + limit;
            } else {
                sql = "SELECT * FROM " + task.getSourceTable() + " LIMIT " + limit;
            }
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= meta.getColumnCount(); i++) {
                    row.put(meta.getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("预览数据失败", e);
            throw new BusinessException("预览数据失败: " + e.getMessage());
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getTaskHistory(Long taskId) {
        List<EtlTaskLog> logs = etlTaskLogService.lambdaQuery()
                .eq(EtlTaskLog::getTaskId, taskId)
                .orderByDesc(EtlTaskLog::getCreateTime)
                .last("LIMIT 20")
                .list();
        return logs.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", log.getId());
            m.put("execStatus", log.getExecStatus());
            m.put("startTime", log.getStartTime());
            m.put("endTime", log.getEndTime());
            m.put("readCount", log.getReadCount());
            m.put("writeCount", log.getWriteCount());
            m.put("skipCount", log.getSkipCount());
            m.put("errorMessage", log.getErrorMessage());
            return m;
        }).collect(Collectors.toList());
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) {}
        }
    }
}
