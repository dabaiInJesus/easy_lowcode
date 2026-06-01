package com.dabai.easy_lowcode;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class CheckDb {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection(
                "jdbc:postgresql://127.0.0.1:5432/easy_lowcode", "postgres", "postgres123");
            Statement stmt = conn.createStatement();
            
            // Check if datasource table has data
            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM collector_datasource");
            rs.next();
            System.out.println("collector_datasource count: " + rs.getInt(1));
            
            // Check databasechangelog
            rs = stmt.executeQuery("SELECT id, author, filename FROM databasechangelog WHERE filename LIKE '%006-create-collector-tables.xml%' OR filename LIKE '%007-create-sys-resource-table.xml%'");
            System.out.println("\n--- databasechangelog records ---");
            while (rs.next()) {
                System.out.println("ID: " + rs.getString("id") + ", Author: " + rs.getString("author") + ", File: " + rs.getString("filename"));
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}