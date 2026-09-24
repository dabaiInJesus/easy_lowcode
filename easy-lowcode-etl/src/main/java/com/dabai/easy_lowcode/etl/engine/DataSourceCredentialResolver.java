package com.dabai.easy_lowcode.etl.engine;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.common.util.EncryptUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * ETL 节点连接凭据解析器
 * <p>
 * 画板中选择已有数据源时，出于安全考虑密码不回填到节点配置（留空或 ******），
 * url/username 也可能缺失。执行时通过节点配置中的 datasourceId 回查数据源配置，
 * 补全缺失的连接信息并解密真实密码——与"测试连接"服务（DataSourceConfigServiceImpl）语义一致。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataSourceCredentialResolver {

    private final DataSourceConfigMapper dataSourceConfigMapper;

    /**
     * 解析节点连接配置：优先使用显式填写的值，缺失/占位的字段用数据源配置兜底
     *
     * @param config 节点 configJson 解析出的配置（可能包含 datasourceId）
     * @return 补全后的配置（原 map 不被修改）
     */
    public Map<String, Object> resolve(Map<String, Object> config) {
        Map<String, Object> resolved = new HashMap<>(config);
        Object dsIdObj = resolved.get("datasourceId");
        String url = str(resolved.get("url"));
        String username = str(resolved.get("username"));
        String password = str(resolved.get("password"));

        boolean needFallback = url.isBlank() || username.isBlank()
                || password.isBlank() || "******".equals(password);
        if (dsIdObj == null || !needFallback) {
            return resolved;
        }

        try {
            Long dsId = Long.parseLong(String.valueOf(dsIdObj));
            DataSourceConfig ds = dataSourceConfigMapper.selectById(dsId);
            if (ds == null) {
                log.warn("数据源配置不存在，使用节点显式配置: datasourceId={}", dsId);
                return resolved;
            }
            if (url.isBlank()) resolved.put("url", ds.getUrl());
            if (username.isBlank()) resolved.put("username", ds.getUsername());
            if (password.isBlank() || "******".equals(password)) {
                resolved.put("password", decrypt(ds.getPassword()));
            }
            log.debug("连接凭据已从数据源配置补全: datasourceId={}, name={}", dsId, ds.getName());
        } catch (NumberFormatException e) {
            log.warn("datasourceId 非法，使用节点显式配置: {}", dsIdObj);
        }
        return resolved;
    }

    /** 解密数据源密码；解密失败视为明文直接使用（与测试连接逻辑一致） */
    private String decrypt(String stored) {
        if (stored == null || stored.isBlank()) return "";
        try {
            return EncryptUtil.decrypt(stored);
        } catch (Exception e) {
            log.debug("密码非加密格式，按明文使用");
            return stored;
        }
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }
}
