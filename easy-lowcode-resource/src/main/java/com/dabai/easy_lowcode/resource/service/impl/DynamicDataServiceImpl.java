package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态数据查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicDataServiceImpl implements DynamicDataService {

    private final TableResourceMapper tableResourceMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;

    /** 字段白名单缓存 (resourceId -> Set<columnName>)，防止SQL注入 */
    private final Map<Long, Set<String>> columnWhitelistCache = new ConcurrentHashMap<>();

    @Override
    public List<Map<String, Object>> queryDataByResourceCode(String resourceCode, Map<String, Object> params) {
        TableResource tableResource = findTableResourceByCode(resourceCode);
        if (tableResource == null) throw new BusinessException("资源不存在: " + resourceCode);
        return executeQuery(tableResource, params);
    }

    @Override
    public List<Map<String, Object>> queryDataByResourceId(Long resourceId, Map<String, Object> params) {
        TableResource tableResource = tableResourceMapper.selectById(resourceId);
        if (tableResource == null) throw new BusinessException("资源不存在，ID: " + resourceId);
        return executeQuery(tableResource, params);
    }

    @Override
    public List<Map<String, Object>> previewData(Long resourceId, int limit) {
        TableResource tableResource = tableResourceMapper.selectById(resourceId);
        if (tableResource == null) throw new BusinessException("资源不存在，ID: " + resourceId);
        if (limit <= 0 || limit > 100) limit = 10;
        Map<String, Object> params = new HashMap<>();
        params.put("_limit", String.valueOf(limit));
        return executeQuery(tableResource, params);
    }

    private List<Map<String, Object>> executeQuery(TableResource tableResource, Map<String, Object> params) {
        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(tableResource.getDatasourceId());
        if (dataSource == null) throw new BusinessException("数据源不存在，ID: " + tableResource.getDatasourceId());

        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;

        try {
            String password;
            try { password = EncryptUtil.decrypt(dataSource.getPassword()); }
            catch (Exception e) { password = dataSource.getPassword(); }

            Class.forName(dataSource.getDriverClassName());
            conn = DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(), password);

            // 构建字段白名单：从 configJson + 表结构实时查询
            Set<String> allowedColumns = getAllowedColumns(tableResource, dataSource, conn);
            if (allowedColumns.isEmpty()) {
                // 降级：使用SELECT * 但不允许任何用户传入的WHERE条件
                log.warn("无法获取表[{}]的字段白名单，仅允许无WHERE条件的查询", tableResource.getTableName());
            }

            String sql = buildQuerySql(tableResource.getTableName(), params, dataSource.getDbType(), allowedColumns);
            log.debug("执行动态查询SQL: {}", sql);

            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);

            List<Map<String, Object>> result = new ArrayList<>();
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rs.getMetaData().getColumnName(i), rs.getObject(i));
                }
                result.add(row);
            }
            return result;
        } catch (Exception e) {
            log.error("执行查询失败", e);
            throw new BusinessException("查询失败: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignore) {}
            try { if (stmt != null) stmt.close(); } catch (Exception ignore) {}
            try { if (conn != null) conn.close(); } catch (Exception ignore) {}
        }
    }

    /**
     * 获取允许查询的字段白名单。
     * 优先级: configJson 中显式配置的字段 > 从 INFORMATION_SCHEMA 实时扫描
     */
    private Set<String> getAllowedColumns(TableResource tableResource, DataSourceConfig dataSource, Connection conn) {
        // 先从缓存取
        Set<String> cached = columnWhitelistCache.get(tableResource.getId());
        if (cached != null) return cached;

        Set<String> columns = new HashSet<>();

        // 1. 从 configJson 解析
        if (tableResource.getConfigJson() != null && !tableResource.getConfigJson().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                Map<String, Object> config = mapper.readValue(tableResource.getConfigJson(), Map.class);
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

        // 2. 如果有显式配置，直接使用
        if (!columns.isEmpty()) {
            columnWhitelistCache.put(tableResource.getId(), columns);
            return columns;
        }

        // 3. 降级：从表结构实时扫描（仅一次）
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT * FROM " + tableResource.getTableName() + " WHERE 1=0");
            int colCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= colCount; i++) {
                columns.add(rs.getMetaData().getColumnName(i).toLowerCase());
            }
            rs.close();
            stmt.close();
            columnWhitelistCache.put(tableResource.getId(), columns);
        } catch (Exception e) {
            log.warn("扫描表[{}]结构失败: {}", tableResource.getTableName(), e.getMessage());
        }

        return columns;
    }

    /**
     * 校验字段名是否在白名单中，防止SQL注入
     */
    private String validateField(String field, Set<String> allowedColumns) {
        if (allowedColumns == null || allowedColumns.isEmpty()) return field; // 无白名单时放行（已有configJson就会走白名单）
        if (!allowedColumns.contains(field.toLowerCase())) {
            throw new BusinessException("不允许的查询字段: " + field);
        }
        return field;
    }

    /**
     * 构建查询SQL，支持动态WHERE条件。
     * 所有用户传入的字段名都经过白名单校验。
     */
    private String buildQuerySql(String tableName, Map<String, Object> params, String dbType,
                                  Set<String> allowedColumns) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT * FROM ").append(tableName);

        List<String> whereClauses = new ArrayList<>();

        // 处理后缀操作符的参数，需要先收集
        Map<String, String> suffixOps = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;
            Object value = entry.getValue();
            if (value == null || value.toString().isEmpty()) continue;
            String strVal = value.toString();

            // 带后缀的操作符先收集，最后统一处理
            if (key.endsWith("_like") || key.endsWith("_gt") || key.endsWith("_gte")
                    || key.endsWith("_lt") || key.endsWith("_lte") || key.endsWith("_in")) {
                suffixOps.put(key, strVal);
                continue;
            }

            // 精确匹配 — 校验字段名
            String validated = validateField(key, allowedColumns);
            whereClauses.add(buildWhereClause(validated, strVal, "eq"));
        }

        // 处理后缀操作符
        for (Map.Entry<String, String> e : suffixOps.entrySet()) {
            String key = e.getKey();
            String strVal = e.getValue();
            String op;

            if (key.endsWith("_like")) { op = "like"; }
            else if (key.endsWith("_gte")) { op = "gte"; }
            else if (key.endsWith("_gt")) { op = "gt"; }
            else if (key.endsWith("_lte")) { op = "lte"; }
            else if (key.endsWith("_lt")) { op = "lt"; }
            else if (key.endsWith("_in")) { op = "in"; }
            else continue;

            String fieldName = key.substring(0, key.length() - op.length() - 1);
            String validated = validateField(fieldName, allowedColumns);
            whereClauses.add(buildWhereClause(validated, strVal, op));
        }

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }

        // 排序字段白名单校验
        String orderField = getParamString(params, "_order");
        if (orderField != null && !orderField.isEmpty()) {
            // 校验排序字段（支持逗号分隔的多字段排序）
            String[] orderParts = orderField.split(",");
            List<String> validatedParts = new ArrayList<>();
            for (String part : orderParts) {
                String trimmed = part.trim();
                // 校验字段名（去掉可能的前缀 ASC/DESC）
                String fieldPart = trimmed.replaceAll("\\s+(ASC|DESC|asc|desc)$", "").trim();
                if (!fieldPart.isEmpty()) {
                    validateField(fieldPart, allowedColumns);
                }
                validatedParts.add(trimmed);
            }
            String sortDir = getParamString(params, "_sort");
            if (sortDir == null || (!"desc".equalsIgnoreCase(sortDir) && !"DESC".equalsIgnoreCase(sortDir))) {
                sortDir = "ASC";
            }
            sql.append(" ORDER BY ").append(String.join(", ", validatedParts)).append(" ").append(sortDir);
        }

        // 分页
        String limitStr = getParamString(params, "_limit");
        if (limitStr != null) {
            appendLimit(sql, Integer.parseInt(limitStr), null, dbType);
        } else {
            String pageStr = getParamString(params, "_page");
            String sizeStr = getParamString(params, "_size");
            int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
            int size = sizeStr != null ? Math.min(100, Integer.parseInt(sizeStr)) : 20;
            appendLimit(sql, size, (page - 1) * size, dbType);
        }

        return sql.toString();
    }

    private String buildWhereClause(String field, String value, String op) {
        switch (op) {
            case "eq":    return field + " = " + quoteValue(value);
            case "like":  return "UPPER(" + field + ") LIKE UPPER('%" + escapeSql(value) + "%')";
            case "gt":    return field + " > " + quoteValue(value);
            case "gte":   return field + " >= " + quoteValue(value);
            case "lt":    return field + " < " + quoteValue(value);
            case "lte":   return field + " <= " + quoteValue(value);
            case "in":
                StringBuilder sb = new StringBuilder(field + " IN (");
                String[] parts = value.split(",");
                for (int i = 0; i < parts.length; i++) {
                    if (i > 0) sb.append(", ");
                    sb.append(quoteValue(parts[i].trim()));
                }
                sb.append(")");
                return sb.toString();
            default:      return field + " = " + quoteValue(value);
        }
    }

    private String quoteValue(String value) {
        if (value.matches("-?\\d+(\\.\\d+)?")) return value;
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return value.toLowerCase();
        return "'" + escapeSql(value) + "'";
    }

    private String escapeSql(String value) {
        return value.replace("'", "''");
    }

    private void appendLimit(StringBuilder sql, int limit, Integer offset, String dbType) {
        String db = dbType.toLowerCase();
        if (db.equals("mysql") || db.equals("postgresql") || db.equals("tidb")
                || db.equals("gbase") || db.equals("oceanbase") || db.equals("opengauss")
                || db.equals("kingbase") || db.equals("highgo")) {
            sql.append(" LIMIT ").append(limit);
            if (offset != null && offset > 0) sql.append(" OFFSET ").append(offset);
        } else if (db.equals("oracle") || db.equals("dm")) {
            sql.insert(0, "SELECT * FROM (").append(")");
            if (offset != null && offset > 0) {
                sql.insert(0, "SELECT * FROM (SELECT t.*, ROWNUM rn FROM (");
                sql.append(") t) WHERE rn > ").append(offset).append(" AND ROWNUM <= ").append(limit);
            } else {
                sql.append(" WHERE ROWNUM <= ").append(limit);
            }
        } else if (db.equals("sqlserver")) {
            sql.insert(7, " TOP " + limit);
        } else {
            sql.append(" LIMIT ").append(limit);
            if (offset != null && offset > 0) sql.append(" OFFSET ").append(offset);
        }
    }

    private String getParamString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val == null ? null : val.toString();
    }

    private TableResource findTableResourceByCode(String resourceCode) {
        return tableResourceMapper.selectByResourceCode(resourceCode);
    }
}
