package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Set123456Password {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/easy_lowcode", "postgres", "postgres123");
            Statement stmt = conn.createStatement();
            
            // 使用 123456 的 BCrypt hash
            String newPassword = "$2a$10$h/DpFV5iK5S7gA3R8oPQDugv0NxRfVGWiQxNVkp7RLuoMM9OkJs4G";
            int updated = stmt.executeUpdate("UPDATE sys_user SET password = '" + newPassword + "' WHERE username = 'admin'");
            System.out.println("已更新 " + updated + " 条记录");
            System.out.println("新密码 123456 已设置");
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}