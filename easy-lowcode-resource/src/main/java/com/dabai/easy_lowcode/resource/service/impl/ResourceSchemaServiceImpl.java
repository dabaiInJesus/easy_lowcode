package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.resource.model.FieldConfig;
import com.dabai.easy_lowcode.resource.service.ResourceCacheManager;
import com.dabai.easy_lowcode.resource.service.ResourceSchemaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * 资源元数据服务实现
 * 负责从 configJson 或 INFORMATION_SCHEMA 解析表字段信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceSchemaServiceImpl implements ResourceSchemaService {

    private final ResourceCacheManager cacheManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Set<String> getAllowedColumns(TableResource tableResource, Connection conn) {
        Set<String> cached = cacheManager.getCachedAllowedColumns(tableResource.getId());
        if (cached != null) {
            return cached;
        }

        Set<String> columns = parseColumnsFromConfig(tableResource);
        if (!columns.isEmpty()) {
            cacheManager.cacheAllowedColumns(tableResource.getId(), columns);
            return columns;
        }

        columns = scanTableColumns(tableResource.getTableName(), conn);
        cacheManager.cacheAllowedColumns(tableResource.getId(), columns);
        return columns;
    }

    @Override
    public List<FieldConfig> getTableFields(TableResource tableResource, Connection conn) {
        return queryInformationSchemaColumns(tableResource.getTableName(), conn);
    }

    @Override
    public String getPrimaryKeyColumn(String tableName, Connection conn) {
        String sql = "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE WHERE TABLE_NAME = ? AND CONSTRAINT_NAME LIKE '%PRIMARY'";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("COLUMN_NAME");
                }
            }
        } catch (Exception e) {
            log.warn("获取主键失败: {}", e.getMessage());
        }
        return "id";
    }

    private Set<String> parseColumnsFromConfig(TableResource tableResource) {
        Set<String> columns = new HashSet<>();
        if (tableResource.getConfigJson() == null || tableResource.getConfigJson().isEmpty()) {
            return columns;
        }

        try {
            Map<String, Object> config = objectMapper.readValue(tableResource.getConfigJson(), Map.class);
            Object fields = config.get("fields");
            if (fields instanceof List) {
                for (Object f : (List<?>) fields) {
                    if (f instanceof Map) {
                        String col = (String) ((Map<?, ?>) f).get("columnName");
                        if (col != null) {
                            columns.add(col.toLowerCase());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析configJson失败: {}", e.getMessage());
        }
        return columns;
    }

    private Set<String> scanTableColumns(String tableName, Connection conn) {
        Set<String> columns = new HashSet<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableName + " WHERE 1=0")) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columns.add(metaData.getColumnName(i).toLowerCase());
            }
        } catch (Exception e) {
            log.warn("扫描表结构失败: {}", e.getMessage());
        }
        return columns;
    }

    private List<FieldConfig> queryInformationSchemaColumns(String tableName, Connection conn) {
        List<FieldConfig> fields = new ArrayList<>();
        String sql = "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    FieldConfig field = new FieldConfig();
                    field.setColumnName(rs.getString("COLUMN_NAME"));
                    field.setColumnComment(rs.getString("COLUMN_COMMENT"));
                    field.setFieldLabel(rs.getString("COLUMN_COMMENT"));

                    String dataType = rs.getString("DATA_TYPE").toLowerCase();
                    if (dataType.contains("int") || dataType.contains("decimal") || dataType.contains("float") || dataType.contains("double") || dataType.contains("numeric")) {
                        field.setFieldType("number");
                    } else if (dataType.contains("date") || dataType.contains("time")) {
                        field.setFieldType("date");
                    } else if (dataType.contains("bit") || dataType.contains("bool")) {
                        field.setFieldType("boolean");
                    } else {
                        field.setFieldType("string");
                    }

                    field.setSearchable("string".equals(field.getFieldType()));
                    field.setSortable(true);
                    fields.add(field);
                }
            }
        } catch (Exception e) {
            log.warn("获取表字段信息失败: {}", e.getMessage());
        }
        return fields;
    }
}
