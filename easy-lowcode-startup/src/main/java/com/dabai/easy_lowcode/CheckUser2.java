package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckUser2 {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/easy_lowcode", "postgres", "postgres123");
            Statement stmt = conn.createStatement();
            
            // Check sys_user table - 正确的列名
            ResultSet rs = stmt.executeQuery("SELECT * FROM sys_user LIMIT 5");
            System.out.println("=== sys_user 表数据 ===");
            java.sql.ResultSetMetaData meta = rs.getMetaData();
            System.out.print("Columns: ");
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                System.out.print(meta.getColumnName(i) + " ");
            }
            System.out.println();
            while (rs.next()) {
                System.out.println("Row: id=" + rs.getLong("id") + ", username=" + rs.getString("username") + 
                    ", password=" + rs.getString("password"));
            }
            
            // Check ai_config table
            rs = stmt.executeQuery("SELECT * FROM ai_config LIMIT 5");
            System.out.println("\n=== ai_config 表数据 ===");
            meta = rs.getMetaData();
            System.out.print("Columns: ");
            for (int i = 1; i <= meta.getColumnCount(); i++) {
                System.out.print(meta.getColumnName(i) + " ");
            }
            System.out.println();
            while (rs.next()) {
                System.out.println("Row: id=" + rs.getLong("id") + ", provider=" + rs.getString("provider"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}