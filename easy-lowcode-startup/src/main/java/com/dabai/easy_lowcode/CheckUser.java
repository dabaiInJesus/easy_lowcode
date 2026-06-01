package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckUser {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/easy_lowcode", "postgres", "postgres123");
            Statement stmt = conn.createStatement();
            
            // Check sys_user table
            ResultSet rs = stmt.executeQuery("SELECT id, username, password, nickname FROM sys_user LIMIT 5");
            System.out.println("=== sys_user 表数据 ===");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id") + ", Username: " + rs.getString("username") + 
                    ", Password: " + rs.getString("password") + ", Nickname: " + rs.getString("nickname"));
            }
            
            // Check ai_config table
            rs = stmt.executeQuery("SELECT id, provider_name, status, is_default FROM ai_config LIMIT 5");
            System.out.println("\n=== ai_config 表数据 ===");
            while (rs.next()) {
                System.out.println("ID: " + rs.getLong("id") + ", Provider: " + rs.getString("provider_name") + 
                    ", Status: " + rs.getInt("status") + ", Default: " + rs.getInt("is_default"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}