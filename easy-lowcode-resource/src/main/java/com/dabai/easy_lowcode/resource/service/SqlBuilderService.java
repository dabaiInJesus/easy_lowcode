package com.dabai.easy_lowcode.resource.service;

import java.util.List;
import java.util.Map;

/**
 * SQL 构建服务
 * 负责生成安全的动态 SQL 语句，包含分页、排序、条件过滤
 */
public interface SqlBuilderService {

    /**
     * 构建 WHERE 条件子句和参数映射
     */
    WhereClauseResult buildWhereClause(Map<String, Object> filters, java.util.Set<String> allowedColumns);

    /**
     * 构建全文检索条件子句
     */
    void appendKeywordClause(List<String> whereClauses, Map<String, String> whereValues, List<String> searchableColumns, String keyword);

    /**
     * 构建 COUNT SQL 语句
     */
    String buildCountSql(String tableName, List<String> whereClauses);

    /**
     * 构建 SELECT SQL 语句
     */
    String buildSelectSql(String tableName, List<String> selectColumns, List<String> whereClauses, String orderField, String orderDirection, String dbType, int pageSize, int offset);

    /**
     * 构建关联查询 SQL
     */
    String buildJoinSql(String leftTable, String rightTable, String leftAlias, String rightAlias, String joinType, String leftField, String rightField, List<String> selectColumns);

    /**
     * 构建不同数据库的分页语法
     */
    String buildLimitOffset(String dbType, int limit, int offset);

    /**
     * WHERE 条件构建结果
     */
    record WhereClauseResult(List<String> clauses, Map<String, String> values) {}
}
