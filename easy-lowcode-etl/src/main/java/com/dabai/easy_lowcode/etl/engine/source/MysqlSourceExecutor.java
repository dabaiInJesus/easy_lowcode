package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.DataSourceCredentialResolver;
import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.database.JdbcCursorItemReader;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

/**
 * MySQL 数据源节点执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MysqlSourceExecutor implements NodeExecutor {

    private final DataSourceCredentialResolver credentialResolver;

    @Override
    public String getNodeType() {
        return "mysql";
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

        log.info("MySQL Source: url={}, query={}", url, query);

        // 使用自定义 Reader 包装 JDBC 读取
        return new JdbcCursorItemReaderWrapper(url, username, password, query);
    }

    /**
     * JDBC Cursor Reader 的简单包装实现
     */
    private static class JdbcCursorItemReaderWrapper implements ItemReader<Map<String, Object>> {
        private final String url;
        private final String username;
        private final String password;
        private final String query;
        private Connection conn;
        private ResultSet rs;
        private ResultSetMetaData metaData;
        private boolean initialized = false;

        JdbcCursorItemReaderWrapper(String url, String username, String password, String query) {
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
                    var stmt = conn.createStatement();
                    // 防止远端网络黑洞导致读取永久挂起：单语句最多执行 1 小时
                    stmt.setQueryTimeout(3600);
                    // 大表用流式游标，避免全表传输到内存/长时间阻塞在首查
                    stmt.setFetchSize(Integer.MIN_VALUE);
                    rs = stmt.executeQuery(query);
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
                log.error("MySQL Source 读取异常", e);
                throw new RuntimeException("MySQL 读取失败: " + e.getMessage(), e);
            }
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "url": { "type": "string", "label": "JDBC URL", "placeholder": "jdbc:mysql://localhost:3306/db" },
                  "username": { "type": "string", "label": "用户名" },
                  "password": { "type": "string", "label": "密码", "inputType": "password" },
                  "table": { "type": "string", "label": "表名" },
                  "query": { "type": "string", "label": "自定义SQL（可选，优先于表名）", "inputType": "textarea" }
                }""";
    }
}
