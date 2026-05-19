package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.service.DataSourceConfigService;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 数据源配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceConfigServiceImpl extends ServiceImpl<DataSourceConfigMapper, DataSourceConfig> implements DataSourceConfigService {
    
    @Override
    public boolean testConnection(DataSourceConfig config) {
        try {
            // 处理密码：如果是星号，说明密码没有修改，需要从数据库获取原密码
            String password = config.getPassword();
            if ("******".equals(password) || password == null || password.trim().isEmpty()) {
                // 如果密码是星号或空，说明是从列表页测试的，需要从数据库查询原密码
                if (config.getId() != null) {
                    DataSourceConfig existingConfig = this.getById(config.getId());
                    if (existingConfig != null) {
                        password = existingConfig.getPassword();
                        log.debug("从数据库获取加密密码");
                    }
                }
            }
            
            // 解密密码
            if (password != null && !password.isEmpty()) {
                try {
                    password = EncryptUtil.decrypt(password);
                    log.debug("密码已解密");
                } catch (Exception e) {
                    // 如果解密失败，说明密码可能是明文，直接使用
                    log.debug("密码为明文，直接使用");
                }
            }
            
            // 加载驱动
            Class.forName(config.getDriverClassName());
            log.debug("驱动加载成功: {}", config.getDriverClassName());
            
            // 尝试连接
            log.debug("尝试连接数据库: URL={}, Username={}", config.getUrl(), config.getUsername());
            Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), password);
            log.debug("数据库连接成功");
            
            // 根据数据库类型执行不同的测试查询
            String testQuery = getTestQuery(config.getDbType());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(testQuery);
            if (rs.next()) {
                log.info("数据源连接测试成功: {}", config.getName());
                rs.close();
                stmt.close();
                conn.close();
                return true;
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
            log.warn("数据源连接测试失败: {} 未返回结果", testQuery);
            return false;
        } catch (ClassNotFoundException e) {
            log.error("数据源连接测试失败 - 驱动类未找到: {}, 驱动: {}", config.getName(), config.getDriverClassName(), e);
            return false;
        } catch (Exception e) {
            log.error("数据源连接测试失败: {}, URL: {}, 用户名: {}, 错误: {}", 
                config.getName(), config.getUrl(), config.getUsername(), e.getMessage(), e);
            return false;
        }
    }
    
    @Override
    public List<Map<String, Object>> scanTables(Long datasourceId) {
        DataSourceConfig config = this.getById(datasourceId);
        if (config == null) {
            throw new RuntimeException("数据源不存在");
        }
        
        List<Map<String, Object>> tables = new ArrayList<>();
        
        try {
            String password = EncryptUtil.decrypt(config.getPassword());
            Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), password);
            
            // 根据数据库类型查询表列表
            String sql = getTableListSql(config.getDbType());
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Map<String, Object> table = new HashMap<>();
                table.put("tableName", rs.getString("table_name"));
                table.put("tableComment", rs.getString("table_comment"));
                tables.add(table);
            }
            
            rs.close();
            stmt.close();
            conn.close();
            
            log.info("扫描到 {} 张表", tables.size());
        } catch (Exception e) {
            log.error("扫描表失败", e);
            throw new RuntimeException("扫描表失败: " + e.getMessage());
        }
        
        return tables;
    }
    
    @Override
    public List<Map<String, Object>> getTableColumns(Long datasourceId, String tableName) {
        DataSourceConfig config = this.getById(datasourceId);
        if (config == null) {
            throw new RuntimeException("数据源不存在");
        }
        
        List<Map<String, Object>> columns = new ArrayList<>();
        
        try {
            String password = EncryptUtil.decrypt(config.getPassword());
            Connection conn = DriverManager.getConnection(config.getUrl(), config.getUsername(), password);
            
            // 根据数据库类型查询列信息
            String sql = getColumnListSql(config.getDbType(), tableName);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(sql);
            
            while (rs.next()) {
                Map<String, Object> column = new HashMap<>();
                column.put("columnName", rs.getString("column_name"));
                column.put("dataType", rs.getString("data_type"));
                column.put("columnComment", rs.getString("column_comment"));
                column.put("isNullable", rs.getString("is_nullable"));
                column.put("columnKey", rs.getString("column_key"));
                columns.add(column);
            }
            
            rs.close();
            stmt.close();
            conn.close();
        } catch (Exception e) {
            log.error("获取表结构失败", e);
            throw new RuntimeException("获取表结构失败: " + e.getMessage());
        }
        
        return columns;
    }
    
    /**
     * 根据数据库类型获取测试查询SQL
     */
    private String getTestQuery(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "postgresql":
            case "sqlserver":
            case "tidb": // TiDB兼容MySQL
            case "oceanbase": // OceanBase兼容MySQL
            case "opengauss": // openGauss兼容PostgreSQL
            case "highgo": // 瀚高数据库兼容PostgreSQL
                return "SELECT 1";
            case "oracle":
            case "dm": // 达梦数据库
            case "kingbase": // 人大金仓
            case "gbase": // GBase
            case "gaussdb": // GaussDB
                return "SELECT 1 FROM DUAL";
            default:
                return "SELECT 1";
        }
    }
    
    /**
     * 根据数据库类型获取表列表SQL
     */
    private String getTableListSql(String dbType) {
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "tidb": // TiDB兼容MySQL
                return "SELECT TABLE_NAME as table_name, TABLE_COMMENT as table_comment FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'";
            case "postgresql":
            case "opengauss": // openGauss兼容PostgreSQL
            case "highgo": // 瀚高数据库兼容PostgreSQL
                return "SELECT tablename as table_name, obj_description((schemaname || '.' || tablename)::regclass) as table_comment FROM pg_tables WHERE schemaname = 'public'";
            case "oracle":
                return "SELECT TABLE_NAME as table_name, COMMENTS as table_comment FROM USER_TAB_COMMENTS WHERE TABLE_TYPE = 'TABLE'";
            case "sqlserver":
                return "SELECT t.name as table_name, ep.value as table_comment FROM sys.tables t LEFT JOIN sys.extended_properties ep ON t.object_id = ep.major_id AND ep.minor_id = 0 AND ep.name = 'MS_Description'";
            // 国产数据库
            case "dm": // 达梦数据库（兼容Oracle）
                return "SELECT TABLE_NAME as table_name, COMMENTS as table_comment FROM USER_TAB_COMMENTS WHERE TABLE_TYPE = 'TABLE'";
            case "kingbase": // 人大金仓（兼容PostgreSQL）
                return "SELECT tablename as table_name, obj_description((schemaname || '.' || tablename)::regclass) as table_comment FROM pg_tables WHERE schemaname = 'public'";
            case "gbase": // GBase 8a（兼容MySQL）
                return "SELECT TABLE_NAME as table_name, TABLE_COMMENT as table_comment FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'";
            case "oceanbase": // OceanBase（兼容MySQL）
                return "SELECT TABLE_NAME as table_name, TABLE_COMMENT as table_comment FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = DATABASE() AND TABLE_TYPE = 'BASE TABLE'";
            case "gaussdb": // GaussDB（兼容PostgreSQL/Oracle模式）
                // GaussDB支持多种兼容模式，这里使用通用查询
                return "SELECT TABLE_NAME as table_name, REMARKS as table_comment FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'BASE TABLE'";
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
    }
    
    private static final Pattern SAFE_SQL_NAME = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]*$");

    private static void validateTableName(String tableName) {
        if (!SAFE_SQL_NAME.matcher(tableName).matches()) {
            throw new IllegalArgumentException("非法表名: " + tableName);
        }
    }

    /**
     * 根据数据库类型获取列列表SQL
     */
    private String getColumnListSql(String dbType, String tableName) {
        validateTableName(tableName);
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "tidb": // TiDB兼容MySQL
            case "gbase": // GBase兼容MySQL
            case "oceanbase": // OceanBase兼容MySQL
                return String.format("SELECT COLUMN_NAME as column_name, DATA_TYPE as data_type, COLUMN_COMMENT as column_comment, IS_NULLABLE as is_nullable, COLUMN_KEY as column_key FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = '%s'", tableName);
            case "postgresql":
            case "opengauss": // openGauss兼容PostgreSQL
            case "highgo": // 瀚高数据库兼容PostgreSQL
                return String.format("SELECT column_name, data_type, col_description((table_schema || '.' || table_name)::regclass, ordinal_position) as column_comment, is_nullable, CASE WHEN column_name IN (SELECT kcu.column_name FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name WHERE tc.table_name = '%s' AND tc.constraint_type = 'PRIMARY KEY') THEN 'PRI' ELSE '' END as column_key FROM information_schema.columns WHERE table_name = '%s'", tableName, tableName);
            case "oracle":
                return String.format("SELECT COLUMN_NAME as column_name, DATA_TYPE as data_type, COMMENTS as column_comment, NULLABLE as is_nullable, CASE WHEN COLUMN_NAME IN (SELECT cols.column_name FROM all_constraints cons, all_cons_columns cols WHERE cols.table_name = '%s' AND cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name) THEN 'PRI' ELSE '' END as column_key FROM USER_TAB_COLUMNS cols LEFT JOIN USER_COL_COMMENTS com ON cols.TABLE_NAME = com.TABLE_NAME AND cols.COLUMN_NAME = com.COLUMN_NAME WHERE cols.TABLE_NAME = '%s'", tableName.toUpperCase(), tableName.toUpperCase());
            case "sqlserver":
                return String.format("SELECT c.name as column_name, t.name as data_type, ep.value as column_comment, CASE WHEN c.is_nullable = 1 THEN 'YES' ELSE 'NO' END as is_nullable, CASE WHEN pk.column_name IS NOT NULL THEN 'PRI' ELSE '' END as column_key FROM sys.columns c JOIN sys.types t ON c.user_type_id = t.user_type_id LEFT JOIN sys.extended_properties ep ON c.object_id = ep.major_id AND c.column_id = ep.minor_id AND ep.name = 'MS_Description' LEFT JOIN (SELECT kcu.COLUMN_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu ON tc.CONSTRAINT_NAME = kcu.CONSTRAINT_NAME WHERE tc.TABLE_NAME = '%s' AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY') pk ON c.name = pk.column_name WHERE c.object_id = OBJECT_ID('%s')", tableName, tableName);
            // 国产数据库
            case "dm": // 达梦数据库（兼容Oracle）
                return String.format("SELECT COLUMN_NAME as column_name, DATA_TYPE as data_type, COMMENTS as column_comment, NULLABLE as is_nullable, CASE WHEN COLUMN_NAME IN (SELECT cols.column_name FROM all_constraints cons, all_cons_columns cols WHERE cols.table_name = '%s' AND cons.constraint_type = 'P' AND cons.constraint_name = cols.constraint_name) THEN 'PRI' ELSE '' END as column_key FROM USER_TAB_COLUMNS cols LEFT JOIN USER_COL_COMMENTS com ON cols.TABLE_NAME = com.TABLE_NAME AND cols.COLUMN_NAME = com.COLUMN_NAME WHERE cols.TABLE_NAME = '%s'", tableName.toUpperCase(), tableName.toUpperCase());
            case "kingbase": // 人大金仓（兼容PostgreSQL）
                return String.format("SELECT column_name, data_type, col_description((table_schema || '.' || table_name)::regclass, ordinal_position) as column_comment, is_nullable, CASE WHEN column_name IN (SELECT kcu.column_name FROM information_schema.table_constraints tc JOIN information_schema.key_column_usage kcu ON tc.constraint_name = kcu.constraint_name WHERE tc.table_name = '%s' AND tc.constraint_type = 'PRIMARY KEY') THEN 'PRI' ELSE '' END as column_key FROM information_schema.columns WHERE table_name = '%s'", tableName, tableName);
            case "gaussdb": // GaussDB（支持多种模式）
                // GaussDB使用通用的INFORMATION_SCHEMA查询
                return String.format("SELECT COLUMN_NAME as column_name, DATA_TYPE as data_type, REMARKS as column_comment, IS_NULLABLE as is_nullable, COLUMN_KEY as column_key FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = '%s'", tableName);
            default:
                throw new RuntimeException("不支持的数据库类型: " + dbType);
        }
    }
}
