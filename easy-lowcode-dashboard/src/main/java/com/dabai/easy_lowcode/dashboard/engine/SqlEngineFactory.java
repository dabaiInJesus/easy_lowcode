package com.dabai.easy_lowcode.dashboard.engine;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SQL 引擎工厂
 * <p>
 * 根据 DataSourceConfig 的 dbType 动态选择合适的 SqlEngine 实现。
 * 使用缓存避免重复创建同一数据源的引擎实例。
 */
@Slf4j
@Component
public class SqlEngineFactory {

    // Hive 配置（从 application.yml 读取）
    @Value("${hive.host:localhost}")
    private String hiveHost;

    @Value("${hive.port:10000}")
    private int hivePort;

    @Value("${hive.auth-type:none}")
    private String hiveAuthType;

    @Value("${hive.principal:}")
    private String hivePrincipal;

    /**
     * 数据源缓存（datasourceId → SqlEngine）
     * 注意：仅缓存 JDBC 引擎，Hive 引擎每次新建（因为连接参数可能变化）
     */
    private final Map<Long, SqlEngine> engineCache = new ConcurrentHashMap<>();

    /**
     * 根据数据源配置获取对应的 SQL 引擎
     *
     * @param config 数据源配置（非空）
     * @return SqlEngine 实例
     */
    public SqlEngine getEngine(DataSourceConfig config) {
        if (config == null) {
            throw new BusinessException("数据源配置不能为空");
        }

        // Hive 类型走单独逻辑（不缓存，每次新建）
        if (isHiveType(config)) {
            return createHiveEngine(config);
        }

        // JDBC 类型走缓存
        return engineCache.computeIfAbsent(config.getId(), id -> {
            log.info("创建 JDBC SqlEngine: datasourceId={}, dbType={}",
                    config.getId(), config.getDbType());
            return new JdbcSqlEngine(config);
        });
    }

    /**
     * 根据数据源 ID 获取引擎
     *
     * @param datasourceId 数据源 ID
     * @param configSupplier 数据源配置查询函数（从 DB 查）
     * @return SqlEngine 实例
     */
    public SqlEngine getEngine(Long datasourceId, java.util.function.Supplier<DataSourceConfig> configSupplier) {
        if (datasourceId == null) {
            throw new BusinessException("数据源ID不能为空");
        }
        SqlEngine cached = engineCache.get(datasourceId);
        if (cached != null) {
            return cached;
        }
        DataSourceConfig config = configSupplier.get();
        if (config == null) {
            throw new BusinessException("数据源不存在: id=" + datasourceId);
        }
        return getEngine(config);
    }

    /**
     * 判断是否为 Hive 类型数据源
     */
    public boolean isHiveType(DataSourceConfig config) {
        if (config == null || config.getDbType() == null) {
            return false;
        }
        String type = config.getDbType().toLowerCase();
        return type.contains("hive") || "hive2".equals(type);
    }

    /**
     * 创建 Hive 引擎
     * <p>
     * 支持两种配置方式：
     * <ol>
     *   <li>通过 DataSourceConfig 的 extra_config JSON 字段配置</li>
     *   <li>通过 application.yml 的 hive.* 全局配置</li>
     * </ol>
     */
    private SqlEngine createHiveEngine(DataSourceConfig config) {
        String host;
        int port;
        String authType;
        String principal;
        String username;
        String password;

        // 优先从 extra_config 解析（JSON 格式）
        String extraConfig = config.getExtraConfig();
        if (extraConfig != null && !extraConfig.isBlank()) {
            try {
                var json = com.alibaba.fastjson.JSON.parseObject(extraConfig);
                host = json.getString("host");
                port = json.getInteger("port") != null ? json.getInteger("port") : 10000;
                authType = json.getString("authType");
                principal = json.getString("principal");
                username = config.getUsername();
                password = config.getPassword();
            } catch (Exception e) {
                log.warn("解析 Hive extra_config 失败，使用全局配置: {}", e.getMessage());
                host = hiveHost;
                port = hivePort;
                authType = hiveAuthType;
                principal = hivePrincipal;
                username = config.getUsername();
                password = config.getPassword();
            }
        } else {
            host = hiveHost;
            port = hivePort;
            authType = hiveAuthType;
            principal = hivePrincipal;
            username = config.getUsername();
            password = config.getPassword();
        }

        log.info("创建 HiveSqlEngine: host={}, port={}, database={}, authType={}",
                host, port, config.getUrl(), authType);

        return new HiveSqlEngine(host, port, config.getUrl(), authType, principal, username, password);
    }

    /**
     * 清除指定数据源的缓存引擎
     */
    public void evict(Long datasourceId) {
        engineCache.remove(datasourceId);
        log.info("清除 SqlEngine 缓存: datasourceId={}", datasourceId);
    }

    /**
     * 清除所有缓存
     */
    public void evictAll() {
        engineCache.clear();
        log.info("清除所有 SqlEngine 缓存");
    }
}
