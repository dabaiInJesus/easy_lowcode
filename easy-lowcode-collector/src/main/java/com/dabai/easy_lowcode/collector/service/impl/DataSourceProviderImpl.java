package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import com.dabai.easy_lowcode.database.model.DataSourceInfo;
import com.dabai.easy_lowcode.database.service.DataSourceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 数据源提供者实现
 * 封装 JDBC 连接管理、密码解密，供 resource/dashboard/etl 模块使用
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataSourceProviderImpl implements DataSourceProvider {

    private final DataSourceConfigMapper dataSourceConfigMapper;

    @Override
    public DataSourceInfo getById(Long id) {
        DataSourceConfig config = dataSourceConfigMapper.selectById(id);
        return config != null ? toInfo(config) : null;
    }

    @Override
    public DataSourceInfo getByCode(String code) {
        DataSourceConfig config = dataSourceConfigMapper.selectOne(
                new LambdaQueryWrapper<DataSourceConfig>().eq(DataSourceConfig::getCode, code));
        return config != null ? toInfo(config) : null;
    }

    @Override
    public List<DataSourceInfo> listActive() {
        return dataSourceConfigMapper.selectList(
                        new LambdaQueryWrapper<DataSourceConfig>().eq(DataSourceConfig::getStatus, 1))
                .stream()
                .map(this::toInfo)
                .collect(Collectors.toList());
    }

    @Override
    public Connection getConnection(Long datasourceId) throws Exception {
        DataSourceInfo info = getById(datasourceId);
        if (info == null) {
            throw new RuntimeException("数据源不存在，ID: " + datasourceId);
        }
        return getConnection(info);
    }

    @Override
    public Connection getConnection(DataSourceInfo info) throws Exception {
        String password = decryptPassword(info.getPassword());
        Class.forName(info.getDriverClassName());
        return DriverManager.getConnection(info.getUrl(), info.getUsername(), password);
    }

    private String decryptPassword(String encrypted) {
        try {
            return EncryptUtil.decrypt(encrypted);
        } catch (Exception e) {
            log.debug("密码解密失败，可能为明文: {}", e.getMessage());
            return encrypted;
        }
    }

    private DataSourceInfo toInfo(DataSourceConfig entity) {
        DataSourceInfo info = new DataSourceInfo();
        info.setId(entity.getId());
        info.setName(entity.getName());
        info.setCode(entity.getCode());
        info.setDbType(entity.getDbType());
        info.setUrl(entity.getUrl());
        info.setUsername(entity.getUsername());
        info.setPassword(entity.getPassword());
        info.setDriverClassName(entity.getDriverClassName());
        info.setStatus(entity.getStatus());
        info.setRemark(entity.getRemark());
        info.setExtraConfig(entity.getExtraConfig());
        return info;
    }
}
