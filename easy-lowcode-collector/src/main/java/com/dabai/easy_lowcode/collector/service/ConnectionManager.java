package com.dabai.easy_lowcode.collector.service;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

/**
 * JDBC 连接管理器
 * 统一处理驱动加载、密码解密、连接获取，消除各模块重复逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConnectionManager {

    private final DataSourceConfigMapper dataSourceConfigMapper;

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
     * 根据数据源配置获取连接
     */
    public Connection getConnection(DataSourceConfig config) throws Exception {
        String password = decryptPassword(config.getPassword());
        Class.forName(config.getDriverClassName());
        return DriverManager.getConnection(config.getUrl(), config.getUsername(), password);
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
