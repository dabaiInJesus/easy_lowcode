package com.dabai.easy_lowcode.dashboard.engine;

import java.util.List;
import java.util.Map;

/**
 * SQL 执行引擎接口
 * <p>
 * 抽象不同数据源的 SQL 执行能力，支持：
 * <ul>
 *   <li>JDBC 关系型数据库（MySQL/PostgreSQL/Oracle/SQLServer）</li>
 *   <li>Hive / HiveServer2</li>
 *   <li>（可扩展）ClickHouse / Doris / Elasticsearch</li>
 * </ul>
 */
public interface SqlEngine {

    /**
     * 执行 SQL 查询
     *
     * @param sql   SQL 语句
     * @param limit 最大返回行数
     * @return 查询结果列表，每行是一个 Map（列名 → 值）
     */
    List<Map<String, Object>> execute(String sql, Integer limit);

    /**
     * 测试数据源连接是否正常
     *
     * @return true 表示连接成功
     */
    boolean testConnection();

    /**
     * 获取数据源类型
     *
     * @return 数据源类型标识（mysql/postgresql/oracle/sqlserver/hive 等）
     */
    SqlDialect getDialect();

    /**
     * 获取表字段元数据（用于 Text-to-SQL 上下文）
     *
     * @param schema  schema/数据库名（可为空）
     * @param table   表名
     * @return 字段列表
     */
    default List<ColumnMeta> getColumns(String schema, String table) {
        return List.of();
    }

    /**
     * SQL 方言枚举
     */
    enum SqlDialect {
        MYSQL("MySQL", "mysql"),
        POSTGRESQL("PostgreSQL", "postgresql"),
        ORACLE("Oracle", "oracle"),
        SQLSERVER("SQL Server", "sqlserver"),
        HIVE("Apache Hive", "hive"),
        HIVE2("HiveServer2", "hive2"),
        OTHER("Other", "other");

        private final String displayName;
        private final String code;

        SqlDialect(String displayName, String code) {
            this.displayName = displayName;
            this.code = code;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getCode() {
            return code;
        }

        public static SqlDialect fromCode(String code) {
            if (code == null) return OTHER;
            for (SqlDialect d : values()) {
                if (d.code.equalsIgnoreCase(code) || d.name().equalsIgnoreCase(code)) {
                    return d;
                }
            }
            return OTHER;
        }
    }

    /**
     * 表字段元数据
     */
    record ColumnMeta(String name, String type, String comment, boolean nullable) {
    }
}
