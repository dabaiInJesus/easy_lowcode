package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.resource.service.SqlBuilderService;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * SQL 构建服务实现
 * 负责生成安全的动态 SQL 语句，支持多种数据库方言
 */
@Service
public class SqlBuilderServiceImpl implements SqlBuilderService {

    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    @Override
    public WhereClauseResult buildWhereClause(Map<String, Object> filters, Set<String> allowedColumns) {
        List<String> clauses = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();

        if (filters == null) {
            return new WhereClauseResult(clauses, values);
        }

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !value.toString().isEmpty() && allowedColumns.contains(field.toLowerCase())) {
                clauses.add(field + " = ?");
                values.put(field, value.toString());
            }
        }
        return new WhereClauseResult(clauses, values);
    }

    @Override
    public void appendKeywordClause(List<String> whereClauses, Map<String, String> whereValues, List<String> searchableColumns, String keyword) {
        if (searchableColumns.isEmpty()) {
            return;
        }
        List<String> keywordClauses = new ArrayList<>();
        for (String col : searchableColumns) {
            keywordClauses.add("UPPER(" + col + ") LIKE UPPER(?)");
            whereValues.put("_kw_" + col, "%" + keyword + "%");
        }
        whereClauses.add("(" + String.join(" OR ", keywordClauses) + ")");
    }

    @Override
    public String buildCountSql(String tableName, List<String> whereClauses) {
        validateTableName(tableName);
        String sql = "SELECT COUNT(*) FROM " + tableName;
        if (!whereClauses.isEmpty()) {
            sql += " WHERE " + String.join(" AND ", whereClauses);
        }
        return sql;
    }

    @Override
    public String buildSelectSql(String tableName, List<String> selectColumns, List<String> whereClauses, String orderField, String orderDirection, String dbType, int pageSize, int offset) {
        validateTableName(tableName);

        List<String> safeColumns = selectColumns.stream()
                .filter(this::isSafeIdentifier)
                .limit(50)
                .collect(Collectors.toList());

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", safeColumns));
        sql.append(" FROM ").append(tableName);

        if (!whereClauses.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", whereClauses));
        }

        if (orderField != null && !orderField.isEmpty() && isSafeIdentifier(orderField)) {
            sql.append(" ORDER BY ").append(orderField);
            sql.append(" ").append("DESC".equalsIgnoreCase(orderDirection) ? "DESC" : "ASC");
        } else {
            sql.append(" ORDER BY 1 DESC");
        }

        sql.append(buildLimitOffset(dbType, pageSize, offset));
        return sql.toString();
    }

    @Override
    public String buildJoinSql(String leftTable, String rightTable, String leftAlias, String rightAlias, String joinType, String leftField, String rightField, List<String> selectColumns) {
        validateTableName(leftTable);
        validateTableName(rightTable);

        String normalizedJoinType = "LEFT".equalsIgnoreCase(joinType) ? "LEFT JOIN" :
                "RIGHT".equalsIgnoreCase(joinType) ? "RIGHT JOIN" : "INNER JOIN";

        StringBuilder sql = new StringBuilder("SELECT ");
        sql.append(String.join(", ", selectColumns));
        sql.append(" FROM ").append(leftTable).append(" ").append(leftAlias);
        sql.append(" ").append(normalizedJoinType).append(" ").append(rightTable).append(" ").append(rightAlias);
        sql.append(" ON ").append(leftAlias).append(".").append(leftField)
                .append(" = ").append(rightAlias).append(".").append(rightField);
        return sql.toString();
    }

    @Override
    public String buildLimitOffset(String dbType, int limit, int offset) {
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

    private void validateTableName(String tableName) {
        if (!SAFE_IDENTIFIER.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
    }

    private boolean isSafeIdentifier(String identifier) {
        return SAFE_IDENTIFIER.matcher(identifier).matches();
    }
}
