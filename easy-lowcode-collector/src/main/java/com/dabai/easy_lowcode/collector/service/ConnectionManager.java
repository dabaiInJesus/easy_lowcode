package com.dabai.easy_lowcode.collector.service;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * JDBC 连接管理器
 * 统一处理驱动加载、密码解密、连接获取，消除各模块重复逻辑
 * 使用 HikariCP 连接池缓存外部数据源连接
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionManager {

    private final DataSourceConfigMapper dataSourceConfigMapper;
    private final Map<String, HikariDataSource> dataSourcePool = new ConcurrentHashMap<>();

    /**
     * 根据数据源ID获取连接
     */
    public Connection getConnection(Long datasourceId) throws Exception {
        DataSourceConfig config = dataSourceConfigMapper.selectById(datasourceId);
        if (config == null) {
            throw new RuntimeException("数据源不存在，ID: " + datasourceId);
        }
        return getConnection(config);
    }

    /**
     * 根据数据源配置获取连接（使用 HikariCP 连接池）
     */
    public Connection getConnection(DataSourceConfig config) throws Exception {
        String poolKey = String.valueOf(config.getId());
        HikariDataSource ds = dataSourcePool.computeIfAbsent(poolKey, key -> {
            log.info("创建 HikariCP 连接池: id={}, url={}", config.getId(), config.getUrl());
            String password = decryptPassword(config.getPassword());
            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(config.getUrl());
            hikariConfig.setUsername(config.getUsername());
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName(config.getDriverClassName());
            hikariConfig.setPoolName("pool-datasource-" + config.getId());
            hikariConfig.setMaximumPoolSize(5);
            hikariConfig.setMinimumIdle(1);
            hikariConfig.setConnectionTimeout(10000);
            hikariConfig.setIdleTimeout(300000);
            hikariConfig.setMaxLifetime(600000);
            hikariConfig.setLeakDetectionThreshold(30000);
            return new HikariDataSource(hikariConfig);
        });
        return ds.getConnection();
    }

    /**
     * 关闭指定数据源的连接池
     */
    public void closePool(Long datasourceId) {
        HikariDataSource ds = dataSourcePool.remove(String.valueOf(datasourceId));
        if (ds != null) {
            log.info("关闭 HikariCP 连接池: id={}", datasourceId);
            ds.close();
        }
    }

    /**
     * 关闭所有连接池
     */
    @PreDestroy
    public void closeAllPools() {
        log.info("关闭所有 HikariCP 连接池，共 {} 个", dataSourcePool.size());
        dataSourcePool.values().forEach(HikariDataSource::close);
        dataSourcePool.clear();
    }

    /**
     * 解密数据源密码
     */
    public String decryptPassword(String encrypted) {
        try {
            return EncryptUtil.decrypt(encrypted);
        } catch (Exception e) {
            log.debug("密码解密失败，可能为明文: {}", e.getMessage());
            return encrypted;
        }
    }
}
