package com.dabai.easy_lowcode.dashboard.engine;

import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.*;

/**
 * HiveServer2 SQL 执行引擎
 * <p>
 * 通过 JDBC 连接 HiveServer2 Thrift 服务，
 * 支持 Kerberos 认证和普通认证两种模式。
 * <p>
 * 依赖（需添加到 pom.xml）：
 * <pre>
 * &lt;dependency&gt;
 *     &lt;groupId&gt;org.apache.hive&lt;/groupId&gt;
 *     &lt;artifactId&gt;hive-jdbc&lt;/artifactId&gt;
 *     &lt;version&gt;4.0.1&lt;/version&gt;
 * &lt;/dependency&gt;
 * </pre>
 * <p>
 * application.yml 配置示例：
 * <pre>
 * hive:
 *   enabled: true
 *   host: localhost
 *   port: 10000
 *   database: default
 *   auth-type: kerberos  # 或 none / ldap
 *   principal: hive/hive@REALM
 *   keytab: /path/to/hive.keytab
 * </pre>
 */
@Slf4j
public class HiveSqlEngine implements SqlEngine {

    private final String host;
    private final int port;
    private final String database;
    private final String authType;
    private final String principal;
    private final String username;
    private final String password;

    public HiveSqlEngine(String host, int port, String database,
                         String authType, String principal,
                         String username, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.authType = authType != null ? authType : "none";
        this.principal = principal;
        this.username = username;
        this.password = password;
    }

    @Override
    public List<Map<String, Object>> execute(String sql, Integer limit) {
        List<Map<String, Object>> result = new ArrayList<>();

        String jdbcUrl = buildJdbcUrl();
        log.debug("连接 HiveServer2: {}", jdbcUrl);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement()) {

            stmt.setQueryTimeout(300); // Hive 查询超时 5 分钟

            String finalSql = applyLimit(sql.trim(), limit);
            log.info("执行 Hive SQL: {}", finalSql);

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
            log.error("Hive SQL 执行失败: sql={}", sql, e);
            throw new RuntimeException("Hive SQL 执行失败: " + e.getMessage(), e);
        }
        return result;
    }

    @Override
    public boolean testConnection() {
        String jdbcUrl = buildJdbcUrl();
        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
            return conn.isValid(10);
        } catch (SQLException e) {
            log.warn("HiveServer2 连接测试失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    public SqlDialect getDialect() {
        return SqlDialect.HIVE2;
    }

    @Override
    public List<ColumnMeta> getColumns(String schema, String table) {
        List<ColumnMeta> columns = new ArrayList<>();
        String jdbcUrl = buildJdbcUrl();
        String query = String.format("DESCRIBE `%s`.`%s`",
                schema != null ? schema : database, table);

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                String colName = rs.getString(1);
                String colType = rs.getString(2);
                String colComment = rs.getString(3);
                if (colName != null && !colName.isEmpty() && !colName.startsWith("#")) {
                    columns.add(new ColumnMeta(colName, colType, colComment, true));
                }
            }
        } catch (SQLException e) {
            log.warn("获取 Hive 表字段失败: table={}, error={}", table, e.getMessage());
        }
        return columns;
    }

    /**
     * 构建 HiveServer2 JDBC URL
     */
    private String buildJdbcUrl() {
        StringBuilder url = new StringBuilder("jdbc:hive2://");
        url.append(host).append(":").append(port)
                .append("/").append(database);

        // 认证参数
        if ("kerberos".equalsIgnoreCase(authType)) {
            url.append(";principal=").append(principal)
               .append(";auth=kerberos");
        } else if ("ldap".equalsIgnoreCase(authType)) {
            // LDAP 认证参数在 URL 中拼接
        }

        return url.toString();
    }

    private String applyLimit(String sql, Integer limit) {
        if (limit == null || limit <= 0) {
            return sql;
        }
        if (sql.toUpperCase().contains(" LIMIT ")) {
            return sql;
        }
        // Hive 支持 LIMIT 子句
        return sql + " LIMIT " + limit;
    }
}
