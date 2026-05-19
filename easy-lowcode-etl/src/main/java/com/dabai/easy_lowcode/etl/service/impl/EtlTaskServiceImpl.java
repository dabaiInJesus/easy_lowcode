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
import com.dabai.easy_lowcode.etl.model.TransformRule;
import com.dabai.easy_lowcode.etl.service.EtlTaskLogService;
import com.dabai.easy_lowcode.etl.service.EtlTaskService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class EtlTaskServiceImpl extends ServiceImpl<EtlTaskMapper, EtlTask> implements EtlTaskService {

    private final EtlTaskLogService etlTaskLogService;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final com.dabai.easy_lowcode.etl.service.ScheduleService scheduleService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static void validateTableName(String tableName) {
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new BusinessException("非法表名: " + tableName);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean createTask(EtlTask task) {
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

        if (task.getReadMode() == null) task.setReadMode("TABLE");
        if (task.getWriteMode() == null) task.setWriteMode("INSERT");
        if (task.getScheduleType() == null) task.setScheduleType("MANUAL");
        if (task.getBatchSize() == null) task.setBatchSize(1000);
        if (task.getStatus() == null) task.setStatus(1);
        if (task.getSkipError() == null) task.setSkipError(0);

        boolean saved = save(task);
        if (saved && task.getStatus() == 1 && !"MANUAL".equals(task.getScheduleType())) {
            scheduleService.scheduleTask(task.getId());
        }
        return saved;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateTask(EtlTask task) {
        LambdaUpdateWrapper<EtlTask> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EtlTask::getId, task.getId());

        if (task.getTaskCode() != null) {
            Long count = baseMapper.selectCount(new LambdaQueryWrapper<EtlTask>()
                    .eq(EtlTask::getTaskCode, task.getTaskCode())
                    .ne(EtlTask::getId, task.getId()));
            if (count > 0) throw new BusinessException("任务编码已存在: " + task.getTaskCode());
        }

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

        asyncExecuteEtl(task, sourceDs, targetDs, logRecord.getId());
        return logRecord.getId();
    }

    @Async("etlExecutor")
    public void asyncExecuteEtl(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId) {
        Future<?> future = null;
        try {
            future = runTask(task, sourceDs, targetDs, logId);
            runningTasks.put(task.getId(), future);
            future.get();
        } catch (java.util.concurrent.CancellationException e) {
            log.warn("ETL任务被中断: taskId={}", task.getId());
            etlTaskLogService.updateLastLogStatus(task.getId(), "STOPPED");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("ETL任务被中断: taskId={}", task.getId());
            etlTaskLogService.updateLastLogStatus(task.getId(), "STOPPED");
        } catch (Exception e) {
            log.error("ETL任务执行异常: taskId={}", task.getId(), e);
            etlTaskLogService.updateLastLogStatus(task.getId(), "FAILED");
        } finally {
            runningTasks.remove(task.getId());
        }
    }

    private Future<?> runTask(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId) {
        return java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            executeEtl(task, sourceDs, targetDs, logId);
        });
    }

    private void executeEtl(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId) {
        Connection sourceConn = null;
        Connection targetConn = null;
        Statement sourceStmt = null;
        ResultSet rs = null;

        try {
            String sourcePwd = decryptPwd(sourceDs.getPassword());
            String targetPwd = decryptPwd(targetDs.getPassword());

            Class.forName(sourceDs.getDriverClassName());
            sourceConn = DriverManager.getConnection(sourceDs.getUrl(), sourceDs.getUsername(), sourcePwd);

            Class.forName(targetDs.getDriverClassName());
            targetConn = DriverManager.getConnection(targetDs.getUrl(), targetDs.getUsername(), targetPwd);
            targetConn.setAutoCommit(false);

            String sourceQuery;
            if ("SQL".equals(task.getReadMode()) && task.getSourceSql() != null) {
                sourceQuery = task.getSourceSql();
            } else {
                validateTableName(task.getSourceTable());
                sourceQuery = "SELECT * FROM " + task.getSourceTable();
            }

            log.info("ETL执行查询: {}", sourceQuery);
            sourceStmt = sourceConn.createStatement();
            rs = sourceStmt.executeQuery(sourceQuery);

            int columnCount = rs.getMetaData().getColumnCount();
            List<String> sourceColumns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                sourceColumns.add(rs.getMetaData().getColumnName(i));
            }

            Map<String, String> fieldMap = parseFieldMapping(task.getFieldMapping(), sourceColumns);
            List<TransformRule> rules = parseTransformRules(task.getTransformRules());

            List<String> targetColumns = fieldMap.values().stream()
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (targetColumns.isEmpty()) targetColumns = sourceColumns;

            List<Object[]> batch = new ArrayList<>();
            long readCount = 0;
            long writeCount = 0;
            long skipCount = 0;
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
            String dbType = targetDs.getDbType();

            while (rs.next()) {
                readCount++;
                Object[] row = new Object[targetColumns.size()];
                boolean skipRow = false;

                for (int i = 0; i < targetColumns.size(); i++) {
                    String targetCol = targetColumns.get(i);
                    String sourceCol = fieldMap.entrySet().stream()
                            .filter(e -> e.getValue().equals(targetCol))
                            .map(Map.Entry::getKey)
                            .findFirst().orElse(targetCol);
                    Object value = rs.getObject(sourceCol);
                    value = applyTransforms(sourceCol, targetCol, value, rules);
                    row[i] = value;
                }

                if (!skipRow) {
                    batch.add(row);
                } else {
                    skipCount++;
                }

                if (batch.size() >= batchSize) {
                    writeCount += batchWrite(targetConn, task.getTargetTable(), targetColumns, batch, task.getWriteMode(), dbType);
                    batch.clear();
                }
            }

            if (!batch.isEmpty()) {
                writeCount += batchWrite(targetConn, task.getTargetTable(), targetColumns, batch, task.getWriteMode(), dbType);
            }

            log.info("ETL执行完成: 读取={}, 写入={}, 跳过={}", readCount, writeCount, skipCount);

            String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
            etlTaskLogService.updateLog(logId, "SUCCESS", now, readCount, writeCount, skipCount);

        } catch (Exception e) {
            log.error("ETL执行出错", e);
            if (targetConn != null) {
                try { targetConn.rollback(); } catch (Exception ex) {
                    log.warn("ETL回滚失败: {}", ex.getMessage());
                }
            }
            throw new RuntimeException("ETL执行失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(sourceStmt);
            closeQuietly(sourceConn);
            if (targetConn != null) {
                try { targetConn.setAutoCommit(true); } catch (Exception ex) {
                    log.warn("重置autoCommit失败: {}", ex.getMessage());
                }
                try { targetConn.close(); } catch (Exception ex) {
                    log.warn("关闭目标连接失败: {}", ex.getMessage());
                }
            }
        }
    }

    private long batchWrite(Connection conn, String targetTable, List<String> targetColumns,
                            List<Object[]> batch, String writeMode, String dbType) throws Exception {
        validateTableName(targetTable);
        if ("TRUNCATE".equalsIgnoreCase(writeMode)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE TABLE " + targetTable);
            }
        }

        String cols = targetColumns.stream().map(c -> escapeIdentifier(c)).collect(Collectors.joining(", "));
        String placeholders = targetColumns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String baseSql = "INSERT INTO " + targetTable + " (" + cols + ") VALUES (" + placeholders + ")";

        String sql;
        switch (writeMode.toUpperCase()) {
            case "MERGE":
                sql = buildMergeSql(targetTable, targetColumns, dbType);
                break;
            case "REPLACE":
                sql = buildReplaceSql(targetTable, targetColumns, dbType);
                break;
            default:
                sql = baseSql;
        }

        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            long count = 0;
            for (Object[] row : batch) {
                for (int i = 0; i < row.length; i++) {
                    pstmt.setObject(i + 1, row[i]);
                }
                pstmt.addBatch();
                count++;

                if (count % 500 == 0) {
                    pstmt.executeBatch();
                    conn.commit();
                }
            }
            pstmt.executeBatch();
            conn.commit();
            return count;
        }
    }

    private String buildMergeSql(String table, List<String> columns, String dbType) {
        String cols = columns.stream().map(c -> escapeIdentifier(c)).collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String updates = columns.stream()
                .map(c -> escapeIdentifier(c) + " = EXCLUDED." + escapeIdentifier(c))
                .collect(Collectors.joining(", "));
        String pk = columns.isEmpty() ? "id" : escapeIdentifier(columns.get(0));

        String db = dbType.toLowerCase();
        if (db.contains("postgresql") || db.contains("kingbase") || db.contains("opengauss")
                || db.contains("highgo") || db.contains("gbase")) {
            return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                    + " ON CONFLICT (" + pk + ") DO UPDATE SET " + updates;
        } else if (db.contains("mysql") || db.contains("tidb") || db.contains("oceanbase")) {
            return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                    + " ON DUPLICATE KEY UPDATE " + updates.replace("EXCLUDED.", "VALUES(")
                            .replace(", ", ", VALUES(") + ")";
        } else if (db.contains("oracle") || db.contains("dm")) {
            String mergeSql = "MERGE INTO " + table + " t USING (SELECT " + placeholders
                    + " FROM DUAL) s ON (t." + pk + " = s." + pk + ") WHEN MATCHED THEN UPDATE SET ";
            mergeSql += updates.replace("EXCLUDED.", "s.");
            mergeSql += " WHEN NOT MATCHED THEN INSERT (" + cols + ") VALUES (" + placeholders + ")";
            return mergeSql;
        } else if (db.contains("sqlserver")) {
            String mergeSql = "MERGE INTO " + table + " AS t USING (SELECT " + placeholders
                    + ") AS s ON (t." + pk + " = s." + pk + ") WHEN MATCHED THEN UPDATE SET ";
            mergeSql += updates.replace("EXCLUDED.", "s.");
            mergeSql += " WHEN NOT MATCHED THEN INSERT (" + cols + ") VALUES (" + placeholders + ")";
            return mergeSql;
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                + " ON CONFLICT (" + pk + ") DO UPDATE SET " + updates;
    }

    private String buildReplaceSql(String table, List<String> columns, String dbType) {
        String cols = columns.stream().map(c -> escapeIdentifier(c)).collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(c -> "?").collect(Collectors.joining(", "));

        String db = dbType.toLowerCase();
        if (db.contains("mysql") || db.contains("tidb") || db.contains("oceanbase")) {
            return "REPLACE INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
        }
        return "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")"
                + " ON CONFLICT (id) DO UPDATE SET "
                + columns.stream().map(c -> escapeIdentifier(c) + " = EXCLUDED." + escapeIdentifier(c))
                        .collect(Collectors.joining(", "));
    }

    private String escapeIdentifier(String id) {
        return "\"" + id.replace("\"", "\"\"") + "\"";
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
                List<Map<String, String>> mappings = objectMapper.readValue(fieldMappingJson,
                        objectMapper.getTypeFactory().constructCollectionType(List.class, Map.class));
                for (Map<String, String> m : mappings) {
                    map.put(m.get("source"), m.get("target"));
                }
            } catch (Exception e) {
                log.warn("解析字段映射失败，使用全量字段映射: {}", e.getMessage());
                for (String col : sourceColumns) map.put(col, col);
            }
        } else {
            for (String col : sourceColumns) map.put(col, col);
        }
        return map;
    }

    private List<TransformRule> parseTransformRules(String transformRulesJson) {
        List<TransformRule> rules = new ArrayList<>();
        if (transformRulesJson == null || transformRulesJson.isBlank()) return rules;
        try {
            rules = objectMapper.readValue(transformRulesJson, new TypeReference<List<TransformRule>>() {});
        } catch (Exception e) {
            log.warn("解析转换规则失败: {}", e.getMessage());
        }
        return rules;
    }

    private Object applyTransforms(String sourceField, String targetField, Object value, List<TransformRule> rules) {
        for (TransformRule rule : rules) {
            boolean matchesSource = rule.getSourceField() != null && rule.getSourceField().equals(sourceField);
            boolean matchesTarget = rule.getTargetField() != null && rule.getTargetField().equals(targetField);
            if (!matchesSource && !matchesTarget) continue;
            if (rule.getTransformType() == null || "NONE".equals(rule.getTransformType())) continue;

            switch (rule.getTransformType().toUpperCase()) {
                case "UPPER":
                    if (value instanceof String) return ((String) value).toUpperCase();
                    break;
                case "LOWER":
                    if (value instanceof String) return ((String) value).toLowerCase();
                    break;
                case "TRIM":
                    if (value instanceof String) return ((String) value).trim();
                    break;
                case "DEFAULT":
                    if (value == null || (value instanceof String && ((String) value).isEmpty())) {
                        return rule.getDefaultValue();
                    }
                    break;
                case "CONCAT":
                    if (rule.getExpression() != null) {
                        String expr = rule.getExpression()
                                .replace("${value}", value != null ? value.toString() : "");
                        return expr;
                    }
                    break;
                case "SUBSTRING":
                    if (value instanceof String && rule.getExpression() != null) {
                        String[] parts = rule.getExpression().split(",");
                        int start = parts.length > 0 ? Integer.parseInt(parts[0].trim()) : 0;
                        int len = parts.length > 1 ? Integer.parseInt(parts[1].trim()) : ((String) value).length();
                        String s = (String) value;
                        return s.substring(Math.min(start, s.length()), Math.min(start + len, s.length()));
                    }
                    break;
                case "DATE_FORMAT":
                    if (value instanceof java.util.Date && rule.getExpression() != null) {
                        try {
                            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(rule.getExpression());
                            return sdf.format((java.util.Date) value);
                        } catch (Exception ignored) {}
                    }
                    break;
            }
        }
        return value;
    }

    @Override
    public boolean stopTask(Long taskId) {
        Future<?> future = runningTasks.get(taskId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                log.info("ETL任务已停止: taskId={}", taskId);
                etlTaskLogService.updateLastLogStatus(taskId, "STOPPED");
            }
            return cancelled;
        }
        log.warn("未找到运行中的ETL任务: taskId={}", taskId);
        return false;
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
        if (ds == null) throw new BusinessException("数据源不存在");
        try (Connection conn = getConnection(ds)) {
            return conn.isValid(5);
        } catch (Exception e) {
            throw new BusinessException("连接测试失败: " + e.getMessage());
        }
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

    @Override
    public List<Map<String, Object>> previewSourceData(Long taskId, int limit) {
        EtlTask task = baseMapper.selectById(taskId);
        if (task == null) throw new BusinessException("任务不存在");
        if (limit <= 0 || limit > 100) limit = 10;
        DataSourceConfig ds = dataSourceConfigMapper.selectById(task.getSourceDatasourceId());
        validateTableName(task.getSourceTable());
        try (Connection conn = getConnection(ds);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM " + task.getSourceTable() + " LIMIT " + limit)) {
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

    private Connection getConnection(DataSourceConfig ds) throws Exception {
        String pwd = decryptPwd(ds.getPassword());
        Class.forName(ds.getDriverClassName());
        return DriverManager.getConnection(ds.getUrl(), ds.getUsername(), pwd);
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try { resource.close(); } catch (Exception ignored) {}
        }
    }
}
