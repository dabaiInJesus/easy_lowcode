package com.dabai.easy_lowcode.collector.service.impl;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.collector.service.DataPreviewService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
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

/**
 * 数据预览服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataPreviewServiceImpl implements DataPreviewService {
    
    private final TableResourceMapper tableResourceMapper;
    private final DataSourceConfigMapper dataSourceConfigMapper;
    
    @Override
    public List<Map<String, Object>> previewTableData(Long resourceId, int limit) {
        // 获取表资源配置
        TableResource tableResource = tableResourceMapper.selectById(resourceId);
        if (tableResource == null) {
            throw new BusinessException("表资源不存在，ID: " + resourceId);
        }
        
        // 获取数据源配置
        DataSourceConfig dataSource = dataSourceConfigMapper.selectById(tableResource.getDatasourceId());
        if (dataSource == null) {
            throw new BusinessException("数据源不存在，ID: " + tableResource.getDatasourceId());
        }
        
        // 限制预览条数
        if (limit <= 0 || limit > 100) {
            limit = 10;
        }
        
        Connection conn = null;
        Statement stmt = null;
        ResultSet rs = null;
        
        try {
            // 解密密码
            String password;
            try {
                password = EncryptUtil.decrypt(dataSource.getPassword());
            } catch (Exception e) {
                password = dataSource.getPassword();
            }
            
            // 建立数据库连接
            Class.forName(dataSource.getDriverClassName());
            conn = DriverManager.getConnection(dataSource.getUrl(), dataSource.getUsername(), password);
            
            // 构建查询SQL
            String sql = buildQuerySql(tableResource.getTableName(), limit, dataSource.getDbType());
            
            log.debug("执行预览SQL: {}", sql);
            
            stmt = conn.createStatement();
            rs = stmt.executeQuery(sql);
            
            // 解析结果集
            List<Map<String, Object>> result = new ArrayList<>();
            int columnCount = rs.getMetaData().getColumnCount();
            
            while (rs.next()) {
                Map<String, Object> row = new HashMap<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnName(i);
                    Object value = rs.getObject(i);
                    row.put(columnName, value);
                }
                result.add(row);
            }
            
            log.info("预览数据完成，返回 {} 条记录", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("预览数据失败", e);
            throw new BusinessException("预览数据失败: " + e.getMessage());
        } finally {
            // 关闭资源
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
                if (conn != null) conn.close();
            } catch (Exception e) {
                log.warn("关闭数据库资源失败", e);
            }
        }
    }
    
    /**
     * 构建查询SQL
     */
    private String buildQuerySql(String tableName, int limit, String dbType) {
        StringBuilder sql = new StringBuilder();
        
        switch (dbType.toLowerCase()) {
            case "mysql":
            case "postgresql":
            case "tidb": // TiDB兼容MySQL
            case "gbase": // GBase兼容MySQL
            case "oceanbase": // OceanBase兼容MySQL
            case "opengauss": // openGauss兼容PostgreSQL
            case "kingbase": // 人大金仓兼容PostgreSQL
            case "highgo": // 瀚高数据库兼容PostgreSQL
                sql.append("SELECT * FROM ").append(tableName).append(" LIMIT ").append(limit);
                break;
            case "oracle":
            case "dm": // 达梦数据库兼容Oracle
                sql.append("SELECT * FROM ").append(tableName).append(" WHERE ROWNUM <= ").append(limit);
                break;
            case "sqlserver":
                sql.append("SELECT TOP ").append(limit).append(" * FROM ").append(tableName);
                break;
            case "gaussdb": // GaussDB
                // GaussDB支持多种模式，使用标准LIMIT语法
                sql.append("SELECT * FROM ").append(tableName).append(" LIMIT ").append(limit);
                break;
            default:
                sql.append("SELECT * FROM ").append(tableName).append(" LIMIT ").append(limit);
        }
        
        return sql.toString();
    }
}
