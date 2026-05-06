package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
import com.dabai.easy_lowcode.resource.service.ResourceSearchService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 资源检索服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceSearchServiceImpl implements ResourceSearchService {

    private final TableResourceMapper tableResourceMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final DynamicDataService dynamicDataService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 字段白名单缓存 */
    private final Map<Long, Set<String>> columnCache = new ConcurrentHashMap<>();
    private final Map<Long, List<FieldConfig>> fieldConfigCache = new ConcurrentHashMap<>();
    
    /** SQL注入检测正则 */
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            ".*(union|select|insert|update|delete|drop|create|alter|exec|execute|script|<|>).*",
            Pattern.CASE_INSENSITIVE);

    // ==================== 单资源检索实现 ====================

    @Override
    public SearchResult singleSearch(String resourceCode, SearchParams params) {
        TableResource tableResource = tableResourceMapper.selectByResourceCode(resourceCode);
        if (tableResource == null) {
            throw new BusinessException("资源不存在: " + resourceCode);
        }

        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        try (Connection conn = getConnection(dataSource)) {
            Set<String> allowedColumns = getAllowedColumns(tableResource, conn);
            List<FieldConfig> fields = getTableFields(tableResource, conn);
            fieldConfigCache.put(tableResource.getId(), fields);
            
            // 构建WHERE条件
            List<String> whereClauses = new ArrayList<>();
            Map<String, String> whereValues = new LinkedHashMap<>();
            
            if (params.getFilters() != null) {
                for (Map.Entry<String, Object> entry : params.getFilters().entrySet()) {
                    String field = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null && !value.toString().isEmpty() && allowedColumns.contains(field.toLowerCase())) {
                        whereClauses.add(field + " = ?");
                        whereValues.put(field, value.toString());
                    }
                }
            }
            
            // 全文检索
            if (params.getKeyword() != null && !params.getKeyword().isEmpty()) {
                List<String> textFields = fields.stream()
                        .filter(f -> "string".equals(f.getFieldType()) && f.isSearchable())
                        .map(f -> f.getColumnName())
                        .collect(Collectors.toList());
                
                if (!textFields.isEmpty()) {
                    List<String> keywordClauses = new ArrayList<>();
                    for (String tf : textFields) {
                        keywordClauses.add("UPPER(" + tf + ") LIKE UPPER(?)");
                        whereValues.put("_kw_" + tf, "%" + params.getKeyword() + "%");
                    }
                    whereClauses.add("(" + String.join(" OR ", keywordClauses) + ")");
                }
            }

            // 构建COUNT查询
            String countSql = "SELECT COUNT(*) FROM " + tableResource.getTableName();
            if (!whereClauses.isEmpty()) {
                countSql += " WHERE " + String.join(" AND ", whereClauses);
            }
            
            long total = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                int idx = 1;
                for (Map.Entry<String, String> entry : whereValues.entrySet()) {
                    if (!entry.getKey().startsWith("_kw_")) {
                        countStmt.setString(idx++, entry.getValue());
                    } else {
                        countStmt.setString(idx++, entry.getValue());
                    }
                }
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            // 构建分页查询
            int page = params.getPage() != null ? params.getPage() : 1;
            int pageSize = params.getPageSize() != null ? Math.min(params.getPageSize(), 100) : 20;
            int offset = (page - 1) * pageSize;

            List<String> selectCols = allowedColumns.stream().limit(50).collect(Collectors.toList());
            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(String.join(", ", selectCols));
            sql.append(" FROM ").append(tableResource.getTableName());
            
            if (!whereClauses.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            }
            
            // 排序
            if (params.getOrderField() != null && !params.getOrderField().isEmpty() 
                    && allowedColumns.contains(params.getOrderField().toLowerCase())) {
                sql.append(" ORDER BY ").append(params.getOrderField());
                sql.append(" ").append("DESC".equalsIgnoreCase(params.getOrderDirection()) ? "DESC" : "ASC");
            } else {
                sql.append(" ORDER BY 1 DESC"); // 默认按主键倒序
            }
            
            // 分页
            sql.append(buildLimitOffset(dataSource.getDbType(), pageSize, offset));

            List<Map<String, Object>> records = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int paramIdx = 1;
                for (Map.Entry<String, String> entry : whereValues.entrySet()) {
                    pstmt.setString(paramIdx++, entry.getValue());
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    ResultSetMetaData metaData = rs.getMetaData();
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        row.put("_resourceCode", resourceCode); // 标记数据来源
                        records.add(row);
                    }
                }
            }

            SearchResult result = new SearchResult(total, page, pageSize, records);
            result.setSourceResource(resourceCode);
            return result;
            
        } catch (Exception e) {
            log.error("单资源检索失败: {}", resourceCode, e);
            throw new BusinessException("检索失败: " + e.getMessage());
        }
    }

    @Override
    public Map<String, Object> singleGetById(String resourceCode, Long id) {
        TableResource tableResource = tableResourceMapper.selectByResourceCode(resourceCode);
        if (tableResource == null) {
            throw new BusinessException("资源不存在: " + resourceCode);
        }

        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        try (Connection conn = getConnection(dataSource)) {
            // 获取主键列名
            String primaryKey = getPrimaryKeyColumn(tableResource.getTableName(), conn);
            
            String sql = "SELECT * FROM " + tableResource.getTableName() + " WHERE " + primaryKey + " = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setObject(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        return row;
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.error("单条记录查询失败: {} - {}", resourceCode, id, e);
            throw new BusinessException("查询失败: " + e.getMessage());
        }
    }

    // ==================== 多资源统一检索实现 ====================

    @Override
    public SearchResult multiSearch(List<String> resourceCodes, SearchParams params) {
        if (resourceCodes == null || resourceCodes.isEmpty()) {
            throw new BusinessException("请至少选择一个资源");
        }

        List<Map<String, Object>> allRecords = new ArrayList<>();
        long total = 0;
        int page = params.getPage() != null ? params.getPage() : 1;
        int pageSize = params.getPageSize() != null ? Math.min(params.getPageSize(), 100) : 20;

        for (String resourceCode : resourceCodes) {
            try {
                SearchResult result = singleSearch(resourceCode, params);
                // 标记来源
                for (Map<String, Object> record : result.getRecords()) {
                    record.put("_resourceCode", resourceCode);
                }
                allRecords.addAll(result.getRecords());
                total += result.getTotal();
            } catch (Exception e) {
                log.warn("资源检索失败 {}: {}", resourceCode, e.getMessage());
            }
        }

        // 多资源合并后重新分页
        int offset = (page - 1) * pageSize;
        int endIdx = Math.min(offset + pageSize, allRecords.size());
        List<Map<String, Object>> pageRecords = offset < allRecords.size() 
                ? allRecords.subList(offset, endIdx) 
                : Collections.emptyList();

        SearchResult result = new SearchResult(total, page, pageSize, new ArrayList<>(pageRecords));
        result.setSourceResources(resourceCodes);
        return result;
    }

    @Override
    public SearchResult joinSearch(JoinConfig joinConfig, SearchParams params) {
        TableResource leftResource = tableResourceMapper.selectByResourceCode(joinConfig.getLeftResource());
        TableResource rightResource = tableResourceMapper.selectByResourceCode(joinConfig.getRightResource());
        
        if (leftResource == null || rightResource == null) {
            throw new BusinessException("关联资源不存在");
        }

        DataSourceConfig leftDs = dataSourceConfigMapper.selectById(leftResource.getDatasourceId());
        DataSourceConfig rightDs = dataSourceConfigMapper.selectById(rightResource.getDatasourceId());
        
        // 简单实现：同数据源关联
        if (!leftDs.getId().equals(rightDs.getId())) {
            throw new BusinessException("目前仅支持同数据源的关联查询");
        }

        try (Connection conn = getConnection(leftDs)) {
            Set<String> leftColumns = getAllowedColumns(leftResource, conn);
            Set<String> rightColumns = getAllowedColumns(rightResource, conn);

            // 构建关联查询
            String leftAlias = "a";
            String rightAlias = "b";
            
            List<String> selectCols = new ArrayList<>();
            for (String col : leftColumns) {
                selectCols.add(leftAlias + "." + col + " AS " + leftAlias + "_" + col);
            }
            for (String col : rightColumns) {
                selectCols.add(rightAlias + "." + col + " AS " + rightAlias + "_" + col);
            }

            String joinType = "LEFT".equalsIgnoreCase(joinConfig.getJoinType()) ? "LEFT JOIN" : 
                              "RIGHT".equalsIgnoreCase(joinConfig.getJoinType()) ? "RIGHT JOIN" : "INNER JOIN";
            
            StringBuilder sql = new StringBuilder("SELECT ");
            sql.append(String.join(", ", selectCols));
            sql.append(" FROM ").append(leftResource.getTableName()).append(" ").append(leftAlias);
            sql.append(" ").append(joinType).append(" ").append(rightResource.getTableName()).append(" ").append(rightAlias);
            sql.append(" ON ").append(leftAlias).append(".").append(joinConfig.getLeftField())
               .append(" = ").append(rightAlias).append(".").append(joinConfig.getRightField());

            // WHERE条件
            List<String> whereClauses = new ArrayList<>();
            if (params.getFilters() != null) {
                for (Map.Entry<String, Object> entry : params.getFilters().entrySet()) {
                    String field = entry.getKey();
                    Object value = entry.getValue();
                    if (value != null && !value.toString().isEmpty()) {
                        // 判断字段属于哪个表
                        if (leftColumns.contains(field.toLowerCase())) {
                            whereClauses.add(leftAlias + "." + field + " = ?");
                        } else if (rightColumns.contains(field.toLowerCase())) {
                            whereClauses.add(rightAlias + "." + field + " = ?");
                        }
                    }
                }
            }

            if (!whereClauses.isEmpty()) {
                sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            }

            // 计数
            String countSql = "SELECT COUNT(*) FROM (" + sql + ") t";
            long total = 0;
            try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
                int idx = 1;
                if (params.getFilters() != null) {
                    for (Map.Entry<String, Object> entry : params.getFilters().entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                            countStmt.setString(idx++, entry.getValue().toString());
                        }
                    }
                }
                try (ResultSet rs = countStmt.executeQuery()) {
                    if (rs.next()) {
                        total = rs.getLong(1);
                    }
                }
            }

            // 分页
            int page = params.getPage() != null ? params.getPage() : 1;
            int pageSize = params.getPageSize() != null ? Math.min(params.getPageSize(), 100) : 20;
            sql.append(buildLimitOffset(leftDs.getDbType(), pageSize, (page - 1) * pageSize));

            List<Map<String, Object>> records = new ArrayList<>();
            try (PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
                int idx = 1;
                if (params.getFilters() != null) {
                    for (Map.Entry<String, Object> entry : params.getFilters().entrySet()) {
                        if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                            pstmt.setString(idx++, entry.getValue().toString());
                        }
                    }
                }
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> row = new LinkedHashMap<>();
                        ResultSetMetaData metaData = rs.getMetaData();
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            row.put(metaData.getColumnName(i), rs.getObject(i));
                        }
                        records.add(row);
                    }
                }
            }

            SearchResult result = new SearchResult(total, page, pageSize, records);
            result.setSourceResources(Arrays.asList(joinConfig.getLeftResource(), joinConfig.getRightResource()));
            return result;

        } catch (Exception e) {
            log.error("关联检索失败", e);
            throw new BusinessException("关联检索失败: " + e.getMessage());
        }
    }

    // ==================== 全文检索实现 ====================

    @Override
    public SearchResult fullTextSearch(String resourceCode, String keyword, SearchParams params) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("请输入搜索关键词");
        }
        params.setKeyword(keyword);
        return singleSearch(resourceCode, params);
    }

    @Override
    public SearchResult multiFullTextSearch(List<String> resourceCodes, String keyword, SearchParams params) {
        if (keyword == null || keyword.trim().isEmpty()) {
            throw new BusinessException("请输入搜索关键词");
        }
        if (resourceCodes == null || resourceCodes.isEmpty()) {
            throw new BusinessException("请至少选择一个资源");
        }
        
        params.setKeyword(keyword);
        List<Map<String, Object>> allRecords = new ArrayList<>();
        long total = 0;

        for (String resourceCode : resourceCodes) {
            try {
                SearchResult result = singleSearch(resourceCode, params);
                for (Map<String, Object> record : result.getRecords()) {
                    record.put("_resourceCode", resourceCode);
                }
                allRecords.addAll(result.getRecords());
                total += result.getTotal();
            } catch (Exception e) {
                log.warn("全文检索失败 {}: {}", resourceCode, e.getMessage());
            }
        }

        // 按相关度排序（简单实现：关键词匹配次数越多越靠前）
        final String kw = keyword.toLowerCase();
        allRecords.sort((a, b) -> {
            int scoreA = countKeywordMatches(a, kw);
            int scoreB = countKeywordMatches(b, kw);
            return Integer.compare(scoreB, scoreA);
        });

        int page = params.getPage() != null ? params.getPage() : 1;
        int pageSize = params.getPageSize() != null ? Math.min(params.getPageSize(), 100) : 20;
        int offset = (page - 1) * pageSize;
        int endIdx = Math.min(offset + pageSize, allRecords.size());
        
        List<Map<String, Object>> pageRecords = offset < allRecords.size() 
                ? allRecords.subList(offset, endIdx) 
                : Collections.emptyList();

        SearchResult result = new SearchResult(total, page, pageSize, new ArrayList<>(pageRecords));
        result.setSourceResources(resourceCodes);
        return result;
    }

    @Override
    public List<FieldConfig> getResourceFields(String resourceCode) {
        TableResource tableResource = tableResourceMapper.selectByResourceCode(resourceCode);
        if (tableResource == null) {
            throw new BusinessException("资源不存在: " + resourceCode);
        }

        // 先检查缓存
        if (fieldConfigCache.containsKey(tableResource.getId())) {
            return fieldConfigCache.get(tableResource.getId());
        }

        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        try (Connection conn = getConnection(dataSource)) {
            List<FieldConfig> fields = getTableFields(tableResource, conn);
            fieldConfigCache.put(tableResource.getId(), fields);
            return fields;
        } catch (Exception e) {
            log.error("获取资源字段失败: {}", resourceCode, e);
            throw new BusinessException("获取字段信息失败: " + e.getMessage());
        }
    }

    // ==================== 私有辅助方法 ====================

    private Connection getConnection(DataSourceConfig dataSource) throws Exception {
        String password;
        try { password = EncryptUtil.decrypt(dataSource.getPassword()); }
        catch (Exception e) { password = dataSource.getPassword(); }
        Class.forName(dataSource.getDriverClassName());
        return DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(), password);
    }

    private Set<String> getAllowedColumns(TableResource tableResource, Connection conn) {
        if (columnCache.containsKey(tableResource.getId())) {
            return columnCache.get(tableResource.getId());
        }

        Set<String> columns = new HashSet<>();
        
        // 从configJson解析
        if (tableResource.getConfigJson() != null && !tableResource.getConfigJson().isEmpty()) {
            try {
                Map<String, Object> config = objectMapper.readValue(tableResource.getConfigJson(), Map.class);
                Object fields = config.get("fields");
                if (fields instanceof List) {
                    for (Object f : (List) fields) {
                        if (f instanceof Map) {
                            String col = (String) ((Map) f).get("columnName");
                            if (col != null) columns.add(col.toLowerCase());
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("解析configJson失败: {}", e.getMessage());
            }
        }

        if (!columns.isEmpty()) {
            columnCache.put(tableResource.getId(), columns);
            return columns;
        }

        // 从表结构扫描
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableResource.getTableName() + " WHERE 1=0")) {
            ResultSetMetaData metaData = rs.getMetaData();
            for (int i = 1; i <= metaData.getColumnCount(); i++) {
                columns.add(metaData.getColumnName(i).toLowerCase());
            }
        } catch (Exception e) {
            log.warn("扫描表结构失败: {}", e.getMessage());
        }

        columnCache.put(tableResource.getId(), columns);
        return columns;
    }

    private List<FieldConfig> getTableFields(TableResource tableResource, Connection conn) {
        List<FieldConfig> fields = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COLUMN_NAME, DATA_TYPE, COLUMN_COMMENT FROM INFORMATION_SCHEMA.COLUMNS " +
                     "WHERE TABLE_NAME = '" + tableResource.getTableName() + "'")) {
            while (rs.next()) {
                FieldConfig field = new FieldConfig();
                field.setColumnName(rs.getString("COLUMN_NAME"));
                field.setComment(rs.getString("COLUMN_COMMENT"));
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
        } catch (Exception e) {
            log.warn("获取表字段信息失败: {}", e.getMessage());
        }
        return fields;
    }

    private String getPrimaryKeyColumn(String tableName, Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE " +
                     "WHERE TABLE_NAME = '" + tableName + "' AND CONSTRAINT_NAME LIKE '%PRIMARY'")) {
            if (rs.next()) {
                return rs.getString("COLUMN_NAME");
            }
        } catch (Exception e) {
            log.warn("获取主键失败: {}", e.getMessage());
        }
        return "id";
    }

    private String buildLimitOffset(String dbType, int limit, int offset) {
        String db = dbType.toLowerCase();
        if (db.contains("mysql") || db.contains("postgresql") || db.contains("tidb") || db.contains("gbase")) {
            return " LIMIT " + limit + " OFFSET " + offset;
        } else if (db.contains("oracle") || db.contains("dm")) {
            return " AND ROWNUM <= " + limit;
        } else if (db.contains("sqlserver")) {
            return " OFFSET " + offset + " ROWS FETCH NEXT " + limit + " ROWS ONLY";
        }
        return " LIMIT " + limit + " OFFSET " + offset;
    }

    private int countKeywordMatches(Map<String, Object> record, String keyword) {
        int count = 0;
        for (Object value : record.values()) {
            if (value != null && value.toString().toLowerCase().contains(keyword)) {
                count++;
            }
        }
        return count;
    }
}
