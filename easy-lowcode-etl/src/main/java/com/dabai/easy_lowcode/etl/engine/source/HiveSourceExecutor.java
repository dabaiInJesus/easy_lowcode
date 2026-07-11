package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * Hive 数据源节点执行器
 */
@Slf4j
@Component
public class HiveSourceExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "hive"; }

    @Override
    public String getCategory() { return "SOURCE"; }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        String url = (String) config.get("url");
        String username = (String) config.getOrDefault("username", "");
        String password = (String) config.getOrDefault("password", "");
        String query = (String) config.get("query");

        if (query == null || query.isBlank()) {
            query = "SELECT * FROM " + config.get("table");
        }

        log.info("Hive Source: url={}, query={}", url, query);

        return new JdbcReaderWrapper(url, username, password, query);
    }

    private static class JdbcReaderWrapper implements ItemReader<Map<String, Object>> {
        private final String url, username, password, query;
        private Connection conn; private ResultSet rs; private ResultSetMetaData meta; private boolean init = false;
        JdbcReaderWrapper(String u, String un, String p, String q) { url=u; username=un; password=p; query=q; }
        @Override public Map<String, Object> read() {
            try {
                if (!init) { conn = DriverManager.getConnection(url, username, password); rs = conn.createStatement().executeQuery(query); meta = rs.getMetaData(); init = true; }
                if (rs != null && rs.next()) { Map<String,Object> row = new LinkedHashMap<>(); for (int i=1;i<=meta.getColumnCount();i++) row.put(meta.getColumnLabel(i), rs.getObject(i)); return row; }
                return null;
            } catch (Exception e) { throw new RuntimeException("Hive 读取失败: " + e.getMessage(), e); }
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "url": { "type": "string", "label": "JDBC URL", "placeholder": "jdbc:hive2://host:10000/db" },
                  "username": { "type": "string", "label": "用户名" },
                  "password": { "type": "string", "label": "密码", "inputType": "password" },
                  "table": { "type": "string", "label": "表名" },
                  "query": { "type": "string", "label": "自定义SQL（可选）", "inputType": "textarea" }
                }""";
    }
}
