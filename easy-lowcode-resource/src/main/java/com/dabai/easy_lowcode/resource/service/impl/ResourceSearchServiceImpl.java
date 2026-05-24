package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.database.model.DataSourceInfo;
import com.dabai.easy_lowcode.database.service.DataSourceProvider;
import com.dabai.easy_lowcode.resource.model.FieldConfig;
import com.dabai.easy_lowcode.resource.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;

/**
 * 资源检索服务实现（重构后）
 * 仅保留协调逻辑，SQL构建、元数据解析、结果处理、缓存均已拆分为独立服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceSearchServiceImpl implements ResourceSearchService {

    private final TableResourceMapper tableResourceMapper;
    private final DataSourceProvider dataSourceProvider;
    private final SqlBuilderService sqlBuilderService;
    private final ResourceSchemaService schemaService;
    private final ResourceResultProcessor resultProcessor;
    private final ResourceCacheManager cacheManager;

    @Override
    public SearchResult singleSearch(String resourceCode, SearchParams params) {
        TableResource tableResource = tableResourceMapper.selectByResourceCode(resourceCode);
        if (tableResource == null) {
            throw new BusinessException("资源不存在: " + resourceCode);
        }

        DataSourceInfo dataSource = dataSourceProvider.getById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        try (Connection conn = dataSourceProvider.getConnection(dataSource)) {
            Set<String> allowedColumns = schemaService.getAllowedColumns(tableResource, conn);
            List<FieldConfig> fields = schemaService.getTableFields(tableResource, conn);
            cacheManager.cacheFieldConfigs(tableResource.getId(), fields);

            SqlBuilderService.WhereClauseResult whereResult = sqlBuilderService.buildWhereClause(params.getFilters(), allowedColumns);
            List<String> whereClauses = whereResult.clauses();
            Map<String, String> whereValues = whereResult.values();

            if (params.getKeyword() != null && !params.getKeyword().isEmpty()) {
                List<String> searchableColumns = fields.stream()
                        .filter(f -> "string".equals(f.getFieldType()) && f.isSearchable())
                        .map(FieldConfig::getColumnName)
                        .toList();
                sqlBuilderService.appendKeywordClause(whereClauses, whereValues, searchableColumns, params.getKeyword());
            }

            int page = params.getPage() != null ? params.getPage() : 1;
            int pageSize = params.getPageSize() != null ? Math.min(params.getPageSize(), 100) : 20;
            int offset = (page - 1) * pageSize;

            String countSql = sqlBuilderService.buildCountSql(tableResource.getTableName(), whereClauses);
            long total = executeCount(conn, countSql, whereValues);

            List<String> selectCols = new ArrayList<>(allowedColumns);
            String selectSql = sqlBuilderService.buildSelectSql(
                    tableResource.getTableName(), selectCols, whereClauses,
                    params.getOrderField(), params.getOrderDirection(),
                    dataSource.getDbType(), pageSize, offset);

            List<Map<String, Object>> records = executeQuery(conn, selectSql, whereValues);
            records.forEach(r -> r.put("_resourceCode", resourceCode));

            resultProcessor.applyResultPipeline(resourceCode, records);

            SearchResult result = new SearchResult(total, page, pageSize, records);
            result.setSourceResource(resourceCode);
            return result;

        } catch (BusinessException e) {
            throw e;
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

        DataSourceInfo dataSource = dataSourceProvider.getById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        try (Connection conn = dataSourceProvider.getConnection(dataSource)) {
            String primaryKey = schemaService.getPrimaryKeyColumn(tableResource.getTableName(), conn);
            String sql = "SELECT * FROM " + tableResource.getTableName() + " WHERE " + primaryKey + " = ?";

            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setObject(1, id);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        Map<String, Object> row = mapResultSetRow(rs);
                        List<Map<String, Object>> list = new ArrayList<>();
                        list.add(row);
                        resultProcessor.applyResultPipeline(resourceCode, list);
                        return list.isEmpty() ? null : list.get(0);
                    }
                }
            }
            return null;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("单条记录查询失败: {} - {}", resourceCode, id, e);
            throw new BusinessException("查询失败: " + e.getMessage());
        }
    }

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
                for (Map<String, Object> record : result.getRecords()) {
                    record.put("_resourceCode", resourceCode);
                }
                allRecords.addAll(result.getRecords());
                total += result.getTotal();
            } catch (Exception e) {
                log.warn("资源检索失败 {}: {}", resourceCode, e.getMessage());
            }
        }

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

        DataSourceInfo leftDs = dataSourceProvider.getById(leftResource.getDatasourceId());
        DataSourceInfo rightDs = dataSourceProvider.getById(rightResource.getDatasourceId());

        if (leftDs == null || rightDs == null || !leftDs.getId().equals(rightDs.getId())) {
            throw new BusinessException("目前仅支持同数据源的关联查询");
        }

        try (Connection conn = dataSourceProvider.getConnection(leftDs)) {
            Set<String> leftColumns = schemaService.getAllowedColumns(leftResource, conn);
            Set<String> rightColumns = schemaService.getAllowedColumns(rightResource, conn);

            if (!leftColumns.contains(joinConfig.getLeftField().toLowerCase())) {
                throw new BusinessException("非法关联字段: " + joinConfig.getLeftField());
            }
            if (!rightColumns.contains(joinConfig.getRightField().toLowerCase())) {
                throw new BusinessException("非法关联字段: " + joinConfig.getRightField());
            }

            String leftAlias = "a";
            String rightAlias = "b";

            List<String> selectCols = new ArrayList<>();
            for (String col : leftColumns) {
                selectCols.add(leftAlias + "." + col + " AS " + leftAlias + "_" + col);
            }
            for (String col : rightColumns) {
                selectCols.add(rightAlias + "." + col + " AS " + rightAlias + "_" + col);
            }

            String joinSql = sqlBuilderService.buildJoinSql(
                    leftResource.getTableName(), rightResource.getTableName(),
                    leftAlias, rightAlias, joinConfig.getJoinType(),
                    joinConfig.getLeftField(), joinConfig.getRightField(), selectCols);

            SqlBuilderService.WhereClauseResult whereResult = buildJoinWhereClause(
                    params.getFilters(), leftColumns, rightColumns, leftAlias, rightAlias);
            List<String> whereClauses = whereResult.clauses();
            Map<String, String> whereValues = whereResult.values();

            StringBuilder fullSql = new StringBuilder(joinSql);
            if (!whereClauses.isEmpty()) {
                fullSql.append(" WHERE ").append(String.join(" AND ", whereClauses));
            }

            String countSql = "SELECT COUNT(*) FROM (" + fullSql + ") t";
            long total = executeCount(conn, countSql, whereValues);

            int page = params.getPage() != null ? params.getPage() : 1;
            int pageSize = params.getPageSize() != null ? Math.min(params.getPageSize(), 100) : 20;
            fullSql.append(sqlBuilderService.buildLimitOffset(leftDs.getDbType(), pageSize, (page - 1) * pageSize));

            List<Map<String, Object>> records = executeQuery(conn, fullSql.toString(), whereValues);

            resultProcessor.applyResultPipeline(joinConfig.getLeftResource(), records);
            resultProcessor.applyResultPipeline(joinConfig.getRightResource(), records);

            SearchResult result = new SearchResult(total, page, pageSize, records);
            result.setSourceResources(Arrays.asList(joinConfig.getLeftResource(), joinConfig.getRightResource()));
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("关联检索失败", e);
            throw new BusinessException("关联检索失败: " + e.getMessage());
        }
    }

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

        final String kw = keyword.toLowerCase();
        allRecords.sort((a, b) -> {
            int scoreA = resultProcessor.countKeywordMatches(a, kw);
            int scoreB = resultProcessor.countKeywordMatches(b, kw);
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

        List<FieldConfig> cachedFields = cacheManager.getCachedFieldConfigs(tableResource.getId());
        if (cachedFields != null) {
            return cachedFields;
        }

        DataSourceInfo dataSource = dataSourceProvider.getById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在");
        }

        try (Connection conn = dataSourceProvider.getConnection(dataSource)) {
            List<FieldConfig> fields = schemaService.getTableFields(tableResource, conn);
            cacheManager.cacheFieldConfigs(tableResource.getId(), fields);
            return fields;
        } catch (Exception e) {
            log.error("获取资源字段失败: {}", resourceCode, e);
            throw new BusinessException("获取字段信息失败: " + e.getMessage());
        }
    }

    private SqlBuilderService.WhereClauseResult buildJoinWhereClause(
            Map<String, Object> filters, Set<String> leftColumns, Set<String> rightColumns,
            String leftAlias, String rightAlias) {
        List<String> clauses = new ArrayList<>();
        Map<String, String> values = new LinkedHashMap<>();

        if (filters == null) {
            return new SqlBuilderService.WhereClauseResult(clauses, values);
        }

        for (Map.Entry<String, Object> entry : filters.entrySet()) {
            String field = entry.getKey();
            Object value = entry.getValue();
            if (value != null && !value.toString().isEmpty()) {
                if (leftColumns.contains(field.toLowerCase())) {
                    clauses.add(leftAlias + "." + field + " = ?");
                    values.put(field, value.toString());
                } else if (rightColumns.contains(field.toLowerCase())) {
                    clauses.add(rightAlias + "." + field + " = ?");
                    values.put(field, value.toString());
                }
            }
        }
        return new SqlBuilderService.WhereClauseResult(clauses, values);
    }

    private long executeCount(Connection conn, String countSql, Map<String, String> whereValues) throws SQLException {
        try (PreparedStatement countStmt = conn.prepareStatement(countSql)) {
            int idx = 1;
            for (String value : whereValues.values()) {
                countStmt.setString(idx++, value);
            }
            try (ResultSet rs = countStmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return 0;
    }

    private List<Map<String, Object>> executeQuery(Connection conn, String sql, Map<String, String> whereValues) throws SQLException {
        List<Map<String, Object>> records = new ArrayList<>();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            int paramIdx = 1;
            for (String value : whereValues.values()) {
                pstmt.setString(paramIdx++, value);
            }
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    records.add(mapResultSetRow(rs));
                }
            }
        }
        return records;
    }

    private Map<String, Object> mapResultSetRow(ResultSet rs) throws SQLException {
        Map<String, Object> row = new LinkedHashMap<>();
        ResultSetMetaData metaData = rs.getMetaData();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            row.put(metaData.getColumnName(i), rs.getObject(i));
        }
        return row;
    }
}
