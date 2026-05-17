package com.dabai.easy_lowcode.resource.template;

import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class TemplateEngine {

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}\\}");
    private static final Pattern CONDITION_PATTERN = Pattern.compile("\\{\\{#([a-zA-Z_][a-zA-Z0-9_]*)\\}}(.*?)\\{\\{/\\1\\}\\}", Pattern.DOTALL);

    private static final Pattern SQL_KEYWORD_PATTERN = Pattern.compile(
            "^(INSERT|UPDATE|DELETE|DROP|ALTER|CREATE|TRUNCATE|EXEC|EXECUTE|CALL)\\s",
            Pattern.CASE_INSENSITIVE);

    private final String templateSql;
    private String tableName;
    private Set<String> allowedColumns;
    private Map<String, Object> params;
    private String dbType;

    @Getter
    private final List<TemplateParameter> detectedParams = new ArrayList<>();

    @Getter
    private String renderedSql;

    public TemplateEngine(String templateSql) {
        if (templateSql == null || templateSql.isBlank()) {
            throw new BusinessException("SQL模板不能为空");
        }
        this.templateSql = templateSql.trim();
    }

    public TemplateEngine setTableName(String tableName) {
        this.tableName = tableName;
        return this;
    }

    public TemplateEngine setAllowedColumns(Set<String> allowedColumns) {
        this.allowedColumns = allowedColumns;
        return this;
    }

    public TemplateEngine bindParams(Map<String, Object> params) {
        this.params = params != null ? params : new HashMap<>();
        return this;
    }

    public TemplateEngine setDbType(String dbType) {
        this.dbType = dbType;
        return this;
    }

    public static class TemplateParameter {
        private final String name;
        private Object value;

        public TemplateParameter(String name) { this.name = name; }
        public String getName() { return name; }
        public Object getValue() { return value; }
        public void setValue(Object value) { this.value = value; }
    }

    public TemplateParameter getParameter(String name) {
        return detectedParams.stream()
                .filter(p -> p.getName().equals(name))
                .findFirst().orElse(null);
    }

    public TemplateEngine build() {
        validateSqlSafety(templateSql);

        String sql = templateSql;

        sql = processConditions(sql);

        sql = processPlaceholders(sql);

        this.renderedSql = sql;
        return this;
    }

    private String processConditions(String sql) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = CONDITION_PATTERN.matcher(sql);

        while (matcher.find()) {
            String paramName = matcher.group(1);
            String content = matcher.group(2);
            Object value = params != null ? params.get(paramName) : null;
            boolean conditionMet = value != null && !value.toString().isEmpty();

            if (conditionMet) {
                matcher.appendReplacement(result, Matcher.quoteReplacement(content));
            } else {
                matcher.appendReplacement(result, "");
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private String processPlaceholders(String sql) {
        StringBuffer result = new StringBuffer();
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(sql);

        while (matcher.find()) {
            String placeholder = matcher.group(1);

            if ("tableName".equals(placeholder)) {
                if (tableName == null) {
                    throw new BusinessException("模板使用了 {{tableName}} 但未设置表名");
                }
                matcher.appendReplacement(result, Matcher.quoteReplacement(tableName));
                continue;
            }

            Object value = params != null ? params.get(placeholder) : null;

            detectedParams.add(new TemplateParameter(placeholder));

            if (value == null) {
                matcher.appendReplacement(result, "?");
                continue;
            }

            if (isColumnReference(placeholder)) {
                String validated = validateColumnRef(placeholder);
                matcher.appendReplacement(result, Matcher.quoteReplacement(validated));
            } else {
                matcher.appendReplacement(result, "?");
            }
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private boolean isColumnReference(String placeholder) {
        if (params == null) return false;
        Object value = params.get(placeholder);
        return value == null;
    }

    private String validateColumnRef(String columnName) {
        if (allowedColumns != null && !allowedColumns.isEmpty()) {
            if (!allowedColumns.contains(columnName.toLowerCase())) {
                throw new BusinessException("模板引用了未授权的字段: " + columnName);
            }
        }
        return columnName;
    }

    private void validateSqlSafety(String sql) {
        String trimmed = sql.trim();
        if (!trimmed.toUpperCase().startsWith("SELECT") && !trimmed.toUpperCase().startsWith("WITH")) {
            throw new BusinessException("SQL模板仅允许 SELECT 查询");
        }
        if (SQL_KEYWORD_PATTERN.matcher(trimmed).find()) {
            throw new BusinessException("SQL模板包含非法的 SQL 关键字");
        }
    }

    public String getSql() {
        return renderedSql;
    }

    public Object[] getParamValues() {
        List<Object> values = new ArrayList<>();
        Pattern p = Pattern.compile("\\{\\{([a-zA-Z_][a-zA-Z0-9_]*)\\}}");
        String sql = templateSql;

        sql = processConditionsRaw(sql);

        Matcher m = p.matcher(sql);
        while (m.find()) {
            String name = m.group(1);
            if ("tableName".equals(name)) continue;
            Object val = params != null ? params.get(name) : null;
            if (val != null && !isColumnReference(name)) {
                values.add(val);
            }
        }
        return values.toArray();
    }

    private String processConditionsRaw(String sql) {
        Matcher matcher = CONDITION_PATTERN.matcher(sql);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String paramName = matcher.group(1);
            String content = matcher.group(2);
            Object value = params != null ? params.get(paramName) : null;
            if (value != null && !value.toString().isEmpty()) {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(content));
            } else {
                matcher.appendReplacement(sb, "");
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
