package com.dabai.easy_lowcode.etl.engine.target;

import com.dabai.easy_lowcode.etl.engine.DataSourceCredentialResolver;
import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * PostgreSQL 目标节点执行器
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PostgresqlTargetExecutor implements NodeExecutor {

    private final DataSourceCredentialResolver credentialResolver;

    @Override
    public String getNodeType() { return "postgresql"; }

    @Override
    public String getCategory() { return "TARGET"; }

    @Override
    public ItemWriter<Map<String, Object>> createWriter(Map<String, Object> config) {
        config = credentialResolver.resolve(config);
        String url = (String) config.get("url");
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        String table = (String) config.get("table");
        String writeMode = (String) config.getOrDefault("writeMode", "INSERT");

        log.info("PostgreSQL Target: url={}, table={}, writeMode={}", url, table, writeMode);

        return chunk -> {
            List<Map<String, Object>> items = new ArrayList<>(chunk.getItems());
            if (items.isEmpty()) return;
            try (Connection conn = DriverManager.getConnection(url, username, password)) {
                conn.setAutoCommit(false);
                Map<String, Object> firstRow = items.get(0);
                List<String> columns = new ArrayList<>(firstRow.keySet());
                String cols = String.join(", ", columns);
                String placeholders = columns.stream().map(c -> "?").collect(java.util.stream.Collectors.joining(", "));

                if ("TRUNCATE".equalsIgnoreCase(writeMode)) {
                    try (Statement stmt = conn.createStatement()) { stmt.execute("TRUNCATE TABLE " + table); }
                }

                String sql = "INSERT INTO " + table + " (" + cols + ") VALUES (" + placeholders + ")";
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    int count = 0;
                    for (Map<String, Object> row : items) {
                        for (int i = 0; i < columns.size(); i++) { pstmt.setObject(i + 1, row.get(columns.get(i))); }
                        pstmt.addBatch();
                        if (++count % 500 == 0) { pstmt.executeBatch(); conn.commit(); }
                    }
                    pstmt.executeBatch();
                    conn.commit();
                    log.info("PostgreSQL Target 写入 {} 条记录到 {}", count, table);
                }
            }
        };
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "url": { "type": "string", "label": "JDBC URL" },
                  "username": { "type": "string", "label": "用户名" },
                  "password": { "type": "string", "label": "密码", "inputType": "password" },
                  "table": { "type": "string", "label": "目标表名" },
                  "writeMode": { "type": "select", "label": "写入模式", "options": ["INSERT", "TRUNCATE"], "default": "INSERT" }
                }""";
    }
}
