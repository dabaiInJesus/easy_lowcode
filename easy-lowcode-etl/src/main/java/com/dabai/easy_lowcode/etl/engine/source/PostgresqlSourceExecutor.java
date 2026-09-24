package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.DataSourceCredentialResolver;
import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL 数据源节点执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresqlSourceExecutor implements NodeExecutor {

    private final DataSourceCredentialResolver credentialResolver;

    @Override
    public String getNodeType() {
        return "postgresql";
    }

    @Override
    public String getCategory() {
        return "SOURCE";
    }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        config = credentialResolver.resolve(config);
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        String query = (String) config.get("query");

        if (query == null || query.isBlank()) {
            String table = (String) config.get("table");
            query = "SELECT * FROM " + table;
        }

        log.info("PostgreSQL Source: url={}, query={}", url, query);

        return new JdbcReaderWrapper(url, username, password, query);
    }

    private static class JdbcReaderWrapper implements ItemReader<Map<String, Object>> {
        private final String url, username, password, query;
        private Connection conn;
        private ResultSet rs;
        private ResultSetMetaData metaData;
        private boolean initialized = false;

        JdbcReaderWrapper(String url, String username, String password, String query) {
            this.url = url;
            this.username = username;
            this.password = password;
            this.query = query;
        }

        @Override
        public Map<String, Object> read() {
            try {
                if (!initialized) {
                    conn = DriverManager.getConnection(url, username, password);
                    // PG JDBC：fetchSize>0 启用服务端游标流式读取，并防止单语句黑洞挂起
                    java.sql.PreparedStatement ps = conn.prepareStatement(query,
                            java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
                    ps.setFetchSize(1000);
                    ps.setQueryTimeout(3600);
                    rs = ps.executeQuery();
                    metaData = rs.getMetaData();
                    initialized = true;
                }
                if (rs != null && rs.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= metaData.getColumnCount(); i++) {
                        row.put(metaData.getColumnLabel(i), rs.getObject(i));
                    }
                    return row;
                }
                return null;
            } catch (Exception e) {
                throw new RuntimeException("PostgreSQL 读取失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "url": { "type": "string", "label": "JDBC URL", "placeholder": "jdbc:postgresql://localhost:5432/db" },
                  "username": { "type": "string", "label": "用户名" },
                  "password": { "type": "string", "label": "密码", "inputType": "password" },
                  "table": { "type": "string", "label": "表名" },
                  "query": { "type": "string", "label": "自定义SQL（可选）", "inputType": "textarea" }
                }""";
    }
}
