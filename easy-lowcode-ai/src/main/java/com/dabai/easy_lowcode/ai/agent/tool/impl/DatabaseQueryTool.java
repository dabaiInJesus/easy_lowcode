package com.dabai.easy_lowcode.ai.agent.tool.impl;

import com.dabai.easy_lowcode.ai.agent.tool.AgentTool;
import com.dabai.easy_lowcode.common.sql.SqlValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.*;
import java.util.*;

/**
 * 数据库查询工具
 * <p>
 * 允许 Agent 查询数据库（仅 SELECT 语句）
 */
@Slf4j
@Component
public class DatabaseQueryTool implements AgentTool {

    @Override
    public String getName() {
        return "database_query";
    }

    @Override
    public String getDescription() {
        return "执行数据库 SELECT 查询。只能执行查询语句，不能修改数据。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "jdbc_url", Map.of("type", "string", "description", "JDBC URL"),
                        "username", Map.of("type", "string", "description", "用户名"),
                        "password", Map.of("type", "string", "description", "密码"),
                        "sql", Map.of("type", "string", "description", "SQL 查询语句（仅 SELECT）")
                ),
                "required", List.of("jdbc_url", "username", "password", "sql")
        );
    }

    @Override
    public Object execute(Map<String, Object> params) {
        String jdbcUrl = (String) params.get("jdbc_url");
        String username = (String) params.get("username");
        String password = (String) params.get("password");
        String sql = (String) params.get("sql");

        // 安全校验：仅允许 SELECT
        try {
            SqlValidator.assertSelectOnly(sql);
        } catch (IllegalArgumentException e) {
            return Map.of("error", e.getMessage());
        }

        try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            ResultSetMetaData meta = rs.getMetaData();
            int colCount = meta.getColumnCount();

            List<String> columns = new ArrayList<>();
            for (int i = 1; i <= colCount; i++) {
                columns.add(meta.getColumnLabel(i));
            }

            List<Map<String, Object>> rows = new ArrayList<>();
            int rowCount = 0;
            while (rs.next() && rowCount < 100) { // 限制最多100行
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= colCount; i++) {
                    row.put(meta.getColumnLabel(i), rs.getObject(i));
                }
                rows.add(row);
                rowCount++;
            }

            return Map.of(
                    "columns", columns,
                    "rows", rows,
                    "rowCount", rows.size()
            );

        } catch (Exception e) {
            log.warn("数据库查询失败: {}", e.getMessage());
            return Map.of("error", "查询失败: " + e.getMessage());
        }
    }

    @Override
    public String getToolType() {
        return "database";
    }
}
