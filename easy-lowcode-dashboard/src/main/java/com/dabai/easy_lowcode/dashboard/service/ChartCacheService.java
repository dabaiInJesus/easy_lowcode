package com.dabai.easy_lowcode.dashboard.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 图表数据缓存服务
 * <p>
 * 使用 Redis 缓存图表查询结果，避免频繁查库。
 * 支持按 chartId + SQL 指纹自动生成缓存 key，
 * 并提供手动清理能力。
 * <p>
 * 缓存策略：
 * <ul>
 *   <li>Key 格式：chart:data:{chartId}:{sqlFingerprint}</li>
 *   <li>默认 TTL：5 分钟（可按图表配置覆盖）</li>
 *   <li>SQL 指纹：对 SQL 字符串做 MD5，防止重复查询</li>
 * </ul>
 */
@Slf4j
@Service
public class ChartCacheService {

    private static final String KEY_PREFIX = "chart:data:";
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    @Autowired
    public ChartCacheService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 尝试从缓存获取图表数据
     *
     * @param chartId      图表 ID
     * @param sql          查询 SQL
     * @param limit        限制条数
     * @param ttlSeconds   缓存 TTL（秒），≤0 时使用默认值
     * @return 缓存的数据（空表示未命中）
     */
    public Optional<List<Map<String, Object>>> get(Long chartId, String sql, int limit, long ttlSeconds) {
        String key = buildKey(chartId, sql);
        try {
            String json = redisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            List<Map<String, Object>> data = objectMapper.readValue(json,
                    new TypeReference<List<Map<String, Object>>>() {});
            log.debug("图表缓存命中: chartId={}, key={}", chartId, key);
            return Optional.of(data);
        } catch (Exception e) {
            log.warn("读取图表缓存失败: chartId={}, key={}, error={}", chartId, key, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 写入图表数据缓存
     *
     * @param chartId    图表 ID
     * @param sql        查询 SQL
     * @param data       查询结果
     * @param ttlSeconds TTL（秒），≤0 时使用默认值
     */
    public void put(Long chartId, String sql, List<Map<String, Object>> data, long ttlSeconds) {
        if (data == null || data.isEmpty()) {
            // 空结果不缓存，防止缓存穿透
            return;
        }
        String key = buildKey(chartId, sql);
        try {
            String json = objectMapper.writeValueAsString(data);
            Duration ttl = ttlSeconds > 0 ? Duration.ofSeconds(ttlSeconds) : DEFAULT_TTL;
            redisTemplate.opsForValue().set(key, json, ttl);
            log.debug("图表缓存写入: chartId={}, key={}, ttl={}, rows={}",
                    chartId, key, ttl, data.size());
        } catch (Exception e) {
            log.warn("写入图表缓存失败: chartId={}, key={}, error={}", chartId, key, e.getMessage());
        }
    }

    /**
     * 清除指定图表的所有缓存
     *
     * @param chartId 图表 ID
     */
    public void invalidate(Long chartId) {
        try {
            String pattern = KEY_PREFIX + chartId + ":*";
            var keys = redisTemplate.keys(pattern);
            if (keys != null && !keys.isEmpty()) {
                redisTemplate.delete(keys);
                log.info("清除图表缓存: chartId={}, 删除 {} 条", chartId, keys.size());
            }
        } catch (Exception e) {
            log.warn("清除图表缓存失败: chartId={}, error={}", chartId, e.getMessage());
        }
    }

    /**
     * 清除指定图表指定 SQL 的缓存
     *
     * @param chartId 图表 ID
     * @param sql     查询 SQL
     */
    public void invalidate(Long chartId, String sql) {
        String key = buildKey(chartId, sql);
        try {
            Boolean deleted = redisTemplate.delete(key);
            if (Boolean.TRUE.equals(deleted)) {
                log.debug("清除单条缓存: key={}", key);
            }
        } catch (Exception e) {
            log.warn("清除缓存失败: key={}, error={}", key, e.getMessage());
        }
    }

    /**
     * 构建缓存 Key：chart:data:{chartId}:{md5(sql+limit)}
     */
    private String buildKey(Long chartId, String sql) {
        String fingerprint = md5(sql);
        return KEY_PREFIX + chartId + ":" + fingerprint;
    }

    /**
     * 简单的 MD5（用于缓存 key 防重，非安全用途）
     */
    private String md5(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            // MD5 永远可用
            return String.valueOf(input.hashCode());
        }
    }
}
