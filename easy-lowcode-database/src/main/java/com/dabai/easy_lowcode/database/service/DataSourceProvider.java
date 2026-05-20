package com.dabai.easy_lowcode.database.service;

import com.dabai.easy_lowcode.database.model.DataSourceInfo;

import java.sql.Connection;
import java.util.List;

/**
 * 数据源提供者接口
 * 用于解耦业务模块（resource/dashboard/etl）与 collector 模块
 */
public interface DataSourceProvider {

    /**
     * 根据ID获取数据源配置
     */
    DataSourceInfo getById(Long id);

    /**
     * 根据编码获取数据源配置
     */
    DataSourceInfo getByCode(String code);

    /**
     * 获取所有启用的数据源
     */
    List<DataSourceInfo> listActive();

    /**
     * 获取数据源的 JDBC 连接（自动处理密码解密）
     */
    Connection getConnection(Long datasourceId) throws Exception;

    /**
     * 获取数据源的 JDBC 连接
     */
    Connection getConnection(DataSourceInfo info) throws Exception;
}
