package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class FixLiquibaseChecksum {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://127.0.0.1:5432/easy_lowcode";
        String user = "postgres";
        String password = System.getenv("DB_PASSWORD");
        if (password == null || password.isEmpty()) {
            password = "postgres123";
        }
        
        try (Connection conn = DriverManager.getConnection(url, user, password);
             Statement stmt = conn.createStatement()) {
            
            System.out.println("开始修复 Liquibase checksum 问题...");
            
            // 删除所有与 collector_tables 和 sys_resource 相关的 changeset 记录
            // 让 Liquibase 重新执行这些变更
            int deleted1 = stmt.executeUpdate(
                "DELETE FROM databasechangelog WHERE filename LIKE '%006-create-collector-tables.xml%'"
            );
            System.out.println("已删除 006-create-collector-tables.xml 的记录: " + deleted1);
            
            int deleted2 = stmt.executeUpdate(
                "DELETE FROM databasechangelog WHERE filename LIKE '%007-create-sys-resource-table.xml%'"
            );
            System.out.println("已删除 007-create-sys-resource-table.xml 的记录: " + deleted2);
            
            // 清空 collector_datasource 表（允许重新插入）
            // 由于表已经有数据且 id 不为空，我们需要处理这种情况
            try {
                int deleted3 = stmt.executeUpdate("DELETE FROM collector_datasource");
                System.out.println("已清空 collector_datasource 表: " + deleted3);
            } catch (Exception e) {
                System.out.println("清空 collector_datasource 失败（可能为空或不存在）: " + e.getMessage());
            }
            
            System.out.println("修复完成！请重新启动应用。");
            
        } catch (Exception e) {
            System.err.println("修复失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}