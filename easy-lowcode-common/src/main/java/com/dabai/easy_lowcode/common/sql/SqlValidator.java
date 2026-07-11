package com.dabai.easy_lowcode.common.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;

/**
 * SQL 安全校验工具
 * <p>
 * 用于防止用户提交非 SELECT 语句（DROP、DELETE、UPDATE 等），
 * 保护 dashboard 和 ETL 模块的数据源安全。
 */
public final class SqlValidator {

    private SqlValidator() {
    }

    /**
     * 校验 SQL 仅允许 SELECT 语句
     *
     * @param sql 待校验的 SQL
     * @throws IllegalArgumentException 如果 SQL 不是纯 SELECT 语句
     */
    public static void assertSelectOnly(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            throw new IllegalArgumentException("SQL 不能为空");
        }
        String trimmed = sql.trim();

        // 拒绝多语句（分号）
        if (trimmed.contains(";")) {
            throw new IllegalArgumentException("不允许执行多条 SQL 语句");
        }

        // 拒绝 SQL 注释
        if (trimmed.contains("--") || trimmed.contains("/*")) {
            throw new IllegalArgumentException("不允许包含 SQL 注释");
        }

        try {
            Statement stmt = CCJSqlParserUtil.parse(trimmed);
            if (!(stmt instanceof Select)) {
                throw new IllegalArgumentException("仅允许 SELECT 语句，不允许: " + stmt.getClass().getSimpleName());
            }
        } catch (JSQLParserException e) {
            throw new IllegalArgumentException("SQL 解析失败: " + e.getMessage());
        }
    }
}
