package com.dabai.easy_lowcode.dashboard.engine;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * JDBC SQL 执行引擎
 * <p>
 * 支持 MySQL / PostgreSQL / Oracle / SQLServer 等关系型数据库。
 * 由 SqlEngineFactory 根据 DataSourceConfig 动态创建实例。
 */
@Slf4j
public class JdbcSqlEngine implements SqlEngine {

    private final DataSourceConfig config;

    public JdbcSqlEngine(DataSourceConfig config) {
        this.config = config;
    }

    @Override
    public List<Map<String, Object>> execute(String sql, Integer limit) {
        List<Map<String, Object>> result = new ArrayList<>();
        String password = decryptPassword();

        try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), password);
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(60);
            String finalSql = applyLimit(sql.trim(), limit);

            log.debug("执行 SQL: {}", finalSql);
            try (ResultSet rs = stmt.executeQuery(finalSql)) {
                ResultSetMetaData metaData = rs.getMetaData();
                int colCount = metaData.getColumnCount();

                while (rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    result.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("SQL 执行失败: sql={}", sql, e);
            throw new RuntimeException("SQL 执行失败: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public boolean testConnection() {
        String password = decryptPassword();
        try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), password)) {
            return conn.isValid(5);
        } catch (SQLException e) {
            log.warn("数据源连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.fromCode(config.getDbType());
    }

    @Override
    public List<ColumnMeta> getColumns(String schema, String table) {
        List<ColumnMeta> columns = new ArrayList<>();
        String password = decryptPassword();
        try (Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), password)) {
            DatabaseMetaData md = conn.getMetaData();
            String connCatalog = conn.getCatalog();
            String connSchema = conn.getSchema();

            try (ResultSet rs = md.getColumns(connCatalog, connSchema, table, "%")) {
                while (rs.next()) {
                    columns.add(new ColumnMeta(
                            rs.getString("COLUMN_NAME"),
                            rs.getString("TYPE_NAME"),
                            rs.getString("REMARKS"),
                            rs.getInt("NULLABLE") == DatabaseMetaData.columnNullable
                    ));
                }
            }
        } catch (SQLException e) {
            log.warn("获取表字段元数据失败: table={}, error={}", table, e.getMessage());
        }
        return columns;
    }

    private String decryptPassword() {
        try {
            return EncryptUtil.decrypt(config.getPassword());
        } catch (Exception e) {
            // 解密失败，尝试原文（未加密的密码）
            return config.getPassword();
        }
    }

    private String applyLimit(String sql, Integer limit) {
        if (limit == null || limit <= 0) {
            return sql;
        }
        if (sql.toUpperCase().contains(" LIMIT ")) {
            return sql;
        }
        return switch (getDialect()) {
            case HIVE, HIVE2, MYSQL -> sql + " LIMIT " + limit;
            case POSTGRESQL, ORACLE -> "SELECT * FROM (" + sql + ") t LIMIT " + limit;
            case SQLSERVER -> {
                String upper = sql.toUpperCase();
                if (upper.startsWith("SELECT ") && !upper.contains(" TOP ")) {
                    yield "SELECT TOP " + limit + " " + sql.substring(7);
                }
                yield sql + " LIMIT " + limit;
            }
            default -> sql + " LIMIT " + limit;
        };
    }
}
