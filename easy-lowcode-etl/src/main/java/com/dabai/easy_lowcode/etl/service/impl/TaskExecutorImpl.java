package com.dabai.easy_lowcode.etl.service.impl;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.model.TransformRule;
import com.dabai.easy_lowcode.etl.service.EtlTaskLogService;
import com.dabai.easy_lowcode.etl.service.TaskExecutor;
import com.dabai.easy_lowcode.etl.service.TaskStateManager;
import com.dabai.easy_lowcode.etl.service.TransformRuleProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * ETL 任务执行器实现
 * 支持 TABLE/SQL 读取模式，INSERT/MERGE/TRUNCATE/REPLACE 写入模式
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TaskExecutorImpl implements TaskExecutor {

    private static final Pattern SAFE_TABLE_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private final EtlTaskLogService etlTaskLogService;
    private final TransformRuleProcessor transformRuleProcessor;
    private final TaskStateManager taskStateManager;
    @Qualifier("etlExecutor")
    private final Executor etlExecutor;

    @Override
    public void executeAsync(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId) {
        CompletableFuture<Void> future = CompletableFuture.runAsync(
                () -> executeEtl(task, sourceDs, targetDs, logId), etlExecutor);
        taskStateManager.registerTask(task.getId(), future);
        future.whenComplete((result, ex) -> {
            taskStateManager.unregisterTask(task.getId());
            if (ex != null) {
                if (ex instanceof java.util.concurrent.CancellationException) {
                    log.warn("ETL任务被中断: taskId={}", task.getId());
                    etlTaskLogService.updateLastLogStatus(task.getId(), "STOPPED");
                } else if (ex.getCause() instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                    log.warn("ETL任务被中断: taskId={}", task.getId());
                    etlTaskLogService.updateLastLogStatus(task.getId(), "STOPPED");
                } else {
                    log.error("ETL任务执行异常: taskId={}", task.getId(), ex);
                    etlTaskLogService.updateLastLogStatus(task.getId(), "FAILED");
                }
            }
        });
    }

    @Override
    public boolean stop(Long taskId) {
        return taskStateManager.stopTask(taskId, () ->
                etlTaskLogService.updateLastLogStatus(taskId, "STOPPED"));
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

            String sourceQuery = resolveSourceQuery(task);
            log.info("ETL执行查询: {}", sourceQuery);

            sourceStmt = sourceConn.createStatement();
            rs = sourceStmt.executeQuery(sourceQuery);

            int columnCount = rs.getMetaData().getColumnCount();
            List<String> sourceColumns = new ArrayList<>();
            for (int i = 1; i <= columnCount; i++) {
                sourceColumns.add(rs.getMetaData().getColumnName(i));
            }

            Map<String, String> fieldMap = transformRuleProcessor.parseFieldMapping(task.getFieldMapping(), sourceColumns);
            List<TransformRule> rules = transformRuleProcessor.parseTransformRules(task.getTransformRules());

            List<String> targetColumns = fieldMap.values().stream()
                    .filter(Objects::nonNull).distinct().collect(Collectors.toList());
            if (targetColumns.isEmpty()) {
                targetColumns = sourceColumns;
            }

            List<Object[]> batch = new ArrayList<>();
            long readCount = 0;
            long writeCount = 0;
            long skipCount = 0;
            int batchSize = task.getBatchSize() != null ? task.getBatchSize() : 1000;
            String dbType = targetDs.getDbType();

            while (rs.next()) {
                readCount++;
                Object[] row = buildRow(targetColumns, fieldMap, rs, rules);
                batch.add(row);

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
            rollbackQuietly(targetConn);
            throw new RuntimeException("ETL执行失败: " + e.getMessage(), e);
        } finally {
            closeQuietly(rs);
            closeQuietly(sourceStmt);
            closeQuietly(sourceConn);
            cleanupTargetConn(targetConn);
        }
    }

    private String resolveSourceQuery(EtlTask task) {
        if ("SQL".equals(task.getReadMode()) && task.getSourceSql() != null) {
            return task.getSourceSql();
        }
        validateTableName(task.getSourceTable());
        return "SELECT * FROM " + task.getSourceTable();
    }

    private Object[] buildRow(List<String> targetColumns, Map<String, String> fieldMap, ResultSet rs, List<TransformRule> rules) throws SQLException {
        Object[] row = new Object[targetColumns.size()];
        for (int i = 0; i < targetColumns.size(); i++) {
            String targetCol = targetColumns.get(i);
            String sourceCol = fieldMap.entrySet().stream()
                    .filter(e -> e.getValue().equals(targetCol))
                    .map(Map.Entry::getKey)
                    .findFirst().orElse(targetCol);
            Object value = rs.getObject(sourceCol);
            row[i] = transformRuleProcessor.applyTransforms(sourceCol, targetCol, value, rules);
        }
        return row;
    }

    private long batchWrite(Connection conn, String targetTable, List<String> targetColumns,
                            List<Object[]> batch, String writeMode, String dbType) throws Exception {
        validateTableName(targetTable);
        if ("TRUNCATE".equalsIgnoreCase(writeMode)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("TRUNCATE TABLE " + targetTable);
            }
        }

        String cols = targetColumns.stream().map(this::escapeIdentifier).collect(Collectors.joining(", "));
        String placeholders = targetColumns.stream().map(c -> "?").collect(Collectors.joining(", "));
        String baseSql = "INSERT INTO " + targetTable + " (" + cols + ") VALUES (" + placeholders + ")";

        String sql = switch (writeMode.toUpperCase()) {
            case "MERGE" -> buildMergeSql(targetTable, targetColumns, dbType);
            case "REPLACE" -> buildReplaceSql(targetTable, targetColumns, dbType);
            default -> baseSql;
        };

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
        String cols = columns.stream().map(this::escapeIdentifier).collect(Collectors.joining(", "));
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
        String cols = columns.stream().map(this::escapeIdentifier).collect(Collectors.joining(", "));
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

    private void rollbackQuietly(Connection conn) {
        if (conn != null) {
            try {
                conn.rollback();
            } catch (Exception ex) {
                log.warn("ETL回滚失败: {}", ex.getMessage());
            }
        }
    }

    private void cleanupTargetConn(Connection conn) {
        if (conn != null) {
            try {
                conn.setAutoCommit(true);
            } catch (Exception ex) {
                log.warn("重置autoCommit失败: {}", ex.getMessage());
            }
            closeQuietly(conn);
        }
    }

    private void closeQuietly(AutoCloseable resource) {
        if (resource != null) {
            try {
                resource.close();
            } catch (Exception ignored) {
            }
        }
    }

    private static void validateTableName(String tableName) {
        if (!SAFE_TABLE_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
    }
}
