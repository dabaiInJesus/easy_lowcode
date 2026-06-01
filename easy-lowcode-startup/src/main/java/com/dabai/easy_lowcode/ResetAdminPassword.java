package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class ResetAdminPassword {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/easy_lowcode", "postgres", "postgres123");
            Statement stmt = conn.createStatement();
            
            // 更新管理员密码为 123456 (BCrypt hash)
            String newPassword = "$2a$10$l9Z.7X9AIuzj1gk783KvcORuWBlLCGraRFMTtdAmz7DTALdS0ajjO";
            int updated = stmt.executeUpdate("UPDATE sys_user SET password = '" + newPassword + "' WHERE username = 'admin'");
            System.out.println("更新了 " + updated + " 条记录");
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}