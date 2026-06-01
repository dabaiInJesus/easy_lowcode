package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class UpdateAdminPassword {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/easy_lowcode", "postgres", "postgres123");
            Statement stmt = conn.createStatement();
            
            // 使用正确生成的 BCrypt hash（123456 的正确 hash）
            String newPassword = "$2a$10$l9Z.7X9AIuzj1gk783KvcORuWBlLCGraRFMTtdAmz7DTALdS0ajjO";
            int updated = stmt.executeUpdate("UPDATE sys_user SET password = '" + newPassword + "' WHERE username = 'admin'");
            System.out.println("已更新 " + updated + " 条记录");
            System.out.println("新密码 123456 已设置");
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}