package com.dabai.easy_lowcode.resource.service.impl;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.resource.model.*;
import com.dabai.easy_lowcode.resource.processor.ProcessorChain;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.ProcessorRegistry;
import com.dabai.easy_lowcode.resource.service.ResourceExecutionService;
import com.dabai.easy_lowcode.resource.template.TemplateEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResourceExecutionServiceImpl implements ResourceExecutionService {

    private final TableResourceMapper tableResourceMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final ProcessorRegistry processorRegistry;

    private final Map<Long, Set<String>> columnWhitelistCache = new ConcurrentHashMap<>();

    @Override
    public ConfigJson getConfig(String resourceCode) {
        TableResource resource = findResource(resourceCode);
        return ConfigParser.parse(resource.getConfigJson());
    }

    @Override
    public ConfigJson getConfigById(Long resourceId) {
        TableResource resource = tableResourceMapper.selectById(resourceId);
        if (resource == null) throw new BusinessException("资源不存在，ID: " + resourceId);
        return ConfigParser.parse(resource.getConfigJson());
    }

    @Override
    public List<QueryTemplate> getTemplates(String resourceCode) {
        ConfigJson config = getConfig(resourceCode);
        return config.getQueryTemplates();
    }

    @Override
    public List<Map<String, Object>> executeQuery(String resourceCode, Map<String, Object> params, String templateName) {
        TableResource resource = findResource(resourceCode);
        return execute(resource, params, templateName);
    }

    @Override
    public List<Map<String, Object>> executeQueryById(Long resourceId, Map<String, Object> params, String templateName) {
        TableResource resource = tableResourceMapper.selectById(resourceId);
        if (resource == null) throw new BusinessException("资源不存在，ID: " + resourceId);
        return execute(resource, params, templateName);
    }

    private List<Map<String, Object>> execute(TableResource resource, Map<String, Object> params, String templateName) {
        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(resource.getDatasourceId());
        if (dataSource == null) throw new BusinessException("数据源不存在");

        ConfigJson config = ConfigParser.parse(resource.getConfigJson());
        Set<String> allowedColumns = config.getAllowedColumnNames();

        ProcessorContext ctx = ProcessorContext.builder()
                .tableResource(resource)
                .dataSourceConfig(dataSource)
                .allowedColumns(allowedColumns)
                .dbType(dataSource.getDbType())
                .build();

        Map<String, Object> processedParams = new HashMap<>();
        if (params != null) processedParams.putAll(params);

        ProcessorChain<Map<String, Object>> paramChain = processorRegistry.buildParamChain(config.getParameterProcessors());
        processedParams = paramChain.execute(processedParams, ctx);

        QueryTemplate template = resolveTemplate(config, templateName, processedParams);

        String sql;
        List<Object> paramValues = new ArrayList<>();

        if (template != null) {
            TemplateEngine engine = new TemplateEngine(template.getSql())
                    .setTableName(resource.getTableName())
                    .setAllowedColumns(allowedColumns)
                    .bindParams(processedParams)
                    .setDbType(dataSource.getDbType())
                    .build();
            sql = engine.getSql();
            paramValues.addAll(Arrays.asList(engine.getParamValues()));
        } else {
            sql = buildAutoQuerySql(resource.getTableName(), processedParams, dataSource.getDbType(), allowedColumns);
        }

        log.debug("执行查询: {}", sql);

        List<Map<String, Object>> rawData = executeJdbcQuery(dataSource, sql, paramValues, config);

        ProcessorChain<List<Map<String, Object>>> resultChain = processorRegistry.buildResultChain(config.getResultProcessors());
        List<Map<String, Object>> processedData = resultChain.execute(rawData, ctx);

        processedData = DisplayFormatter.format(processedData, config.getDisplaySettings());

        return processedData;
    }

    private QueryTemplate resolveTemplate(ConfigJson config, String templateName, Map<String, Object> params) {
        if (templateName != null) {
            QueryTemplate tmpl = config.getTemplate(templateName);
            if (tmpl != null) return tmpl;
        }
        String paramTemplate = params != null ? (String) params.remove("_template") : null;
        if (paramTemplate != null) {
            QueryTemplate tmpl = config.getTemplate(paramTemplate);
            if (tmpl != null) return tmpl;
        }
        if (config.hasTemplate("default")) {
            return config.getTemplate("default");
        }
        return null;
    }

    private String buildAutoQuerySql(String tableName, Map<String, Object> params, String dbType, Set<String> allowedColumns) {
        StringBuilder sql = new StringBuilder("SELECT * FROM ").append(tableName);
        List<String> wheres = new ArrayList<>();

        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            if (key.startsWith("_")) continue;
            Object value = entry.getValue();
            if (value == null || value.toString().isEmpty()) continue;
            String strVal = value.toString();

            if (key.endsWith("_like") || key.endsWith("_gt") || key.endsWith("_gte")
                    || key.endsWith("_lt") || key.endsWith("_lte") || key.endsWith("_in")) {
                String op;
                if (key.endsWith("_like")) op = "like";
                else if (key.endsWith("_gte")) op = ">=";
                else if (key.endsWith("_gt")) op = ">";
                else if (key.endsWith("_lte")) op = "<=";
                else if (key.endsWith("_lt")) op = "<";
                else op = "in";
                String fieldName = key.substring(0, key.length() - op.length() - 1);
                validateField(fieldName, allowedColumns);
                if ("in".equals(op)) {
                    wheres.add(fieldName + " IN (" + String.join(",", strVal.split(",")) + ")");
                } else if ("like".equals(op)) {
                    wheres.add("UPPER(" + fieldName + ") LIKE UPPER('%" + escapeSql(strVal) + "%')");
                } else {
                    wheres.add(fieldName + " " + op + " " + quoteValue(strVal));
                }
                continue;
            }

            validateField(key, allowedColumns);
            wheres.add(key + " = " + quoteValue(strVal));
        }

        if (!wheres.isEmpty()) {
            sql.append(" WHERE ").append(String.join(" AND ", wheres));
        }

        String orderField = getParamString(params, "_order");
        if (orderField != null) {
            String sortDir = getParamString(params, "_sort");
            if (sortDir == null || (!"desc".equalsIgnoreCase(sortDir) && !"DESC".equalsIgnoreCase(sortDir))) {
                sortDir = "ASC";
            }
            sql.append(" ORDER BY ").append(orderField).append(" ").append(sortDir);
        }

        String limitStr = getParamString(params, "_limit");
        if (limitStr != null) {
            sql.append(" LIMIT ").append(limitStr);
        } else {
            String pageStr = getParamString(params, "_page");
            String sizeStr = getParamString(params, "_size");
            int page = pageStr != null ? Math.max(1, Integer.parseInt(pageStr)) : 1;
            int size = sizeStr != null ? Math.min(100, Integer.parseInt(sizeStr)) : 20;
            sql.append(" LIMIT ").append(size);
            if (page > 1) sql.append(" OFFSET ").append((page - 1) * size);
        }

        return sql.toString();
    }

    private List<Map<String, Object>> executeJdbcQuery(
            DataSourceConfig dataSource, String sql,
            List<Object> paramValues, ConfigJson config) {

        Connection conn = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;

        try {
            String password;
            try { password = EncryptUtil.decrypt(dataSource.getPassword()); }
            catch (Exception e) { password = dataSource.getPassword(); }

            Class.forName(dataSource.getDriverClassName());
            conn = DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(), password);

            if (paramValues.isEmpty()) {
                Statement stmt = conn.createStatement();
                rs = stmt.executeQuery(sql);
            } else {
                pstmt = conn.prepareStatement(sql);
                for (int i = 0; i < paramValues.size(); i++) {
                    pstmt.setObject(i + 1, paramValues.get(i));
                }
                rs = pstmt.executeQuery();
            }

            List<Map<String, Object>> result = new ArrayList<>();
            int columnCount = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String colName = rs.getMetaData().getColumnLabel(i);
                    Object colVal = rs.getObject(i);
                    row.put(colName, colVal);
                }
                result.add(row);
            }
            return result;

        } catch (Exception e) {
            log.error("执行查询失败: {}", sql, e);
            throw new BusinessException("查询失败: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (Exception ignore) {}
            try { if (pstmt != null) pstmt.close(); } catch (Exception ignore) {}
            try { if (conn != null) conn.close(); } catch (Exception ignore) {}
        }
    }

    private void validateField(String field, Set<String> allowedColumns) {
        if (allowedColumns != null && !allowedColumns.isEmpty()
                && !allowedColumns.contains(field.toLowerCase())) {
            throw new BusinessException("不允许的查询字段: " + field);
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

    private String getParamString(Map<String, Object> params, String key) {
        Object val = params.get(key);
        return val == null ? null : val.toString();
    }

    private TableResource findResource(String resourceCode) {
        TableResource resource = tableResourceMapper.selectByResourceCode(resourceCode);
        if (resource == null) throw new BusinessException("资源不存在: " + resourceCode);
        return resource;
    }
}
