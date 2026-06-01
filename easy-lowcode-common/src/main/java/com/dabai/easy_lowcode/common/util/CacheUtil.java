package com.dabai.easy_lowcode.common.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis缓存工具类
 * 提供简单的缓存操作封装
 */
@Slf4j
@Component
public class CacheUtil {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final String CACHE_PREFIX = "easy-lowcode:";

    /**
     * 设置缓存（永不过期）
     */
    public void set(String key, String value) {
        try {
            redisTemplate.opsForValue().set(getRealKey(key), value);
        } catch (Exception e) {
            log.warn("Redis set failed: {}", e.getMessage());
        }
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, String value, long timeout, TimeUnit unit) {
        try {
            redisTemplate.opsForValue().set(getRealKey(key), value, timeout, unit);
        } catch (Exception e) {
            log.warn("Redis set with expiry failed: {}", e.getMessage());
        }
    }

    /**
     * 设置缓存（带过期时间）
     */
    public void set(String key, String value, Duration duration) {
        try {
            redisTemplate.opsForValue().set(getRealKey(key), value, duration);
        } catch (Exception e) {
            log.warn("Redis set with duration failed: {}", e.getMessage());
        }
    }

    /**
     * 获取缓存
     */
    public String get(String key) {
        try {
            return redisTemplate.opsForValue().get(getRealKey(key));
        } catch (Exception e) {
            log.warn("Redis get failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 删除缓存
     */
    public void delete(String key) {
        try {
            redisTemplate.delete(getRealKey(key));
        } catch (Exception e) {
            log.warn("Redis delete failed: {}", e.getMessage());
        }
    }

    /**
     * 判断key是否存在
     */
    public boolean exists(String key) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(getRealKey(key)));
        } catch (Exception e) {
            log.warn("Redis exists check failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 设置过期时间
     */
    public boolean expire(String key, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(redisTemplate.expire(getRealKey(key), timeout, unit));
        } catch (Exception e) {
            log.warn("Redis expire failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 获取剩余过期时间
     */
    public Long getExpire(String key) {
        try {
            return redisTemplate.getExpire(getRealKey(key), TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Redis getExpire failed: {}", e.getMessage());
            return -1L;
        }
    }

    /**
     * 递增
     */
    public Long increment(String key) {
        try {
            return redisTemplate.opsForValue().increment(getRealKey(key));
        } catch (Exception e) {
            log.warn("Redis increment failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 递增（带步长）
     */
    public Long increment(String key, long delta) {
        try {
            return redisTemplate.opsForValue().increment(getRealKey(key), delta);
        } catch (Exception e) {
            log.warn("Redis increment with delta failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 递减
     */
    public Long decrement(String key) {
        try {
            return redisTemplate.opsForValue().decrement(getRealKey(key));
        } catch (Exception e) {
            log.warn("Redis decrement failed: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 删除匹配的所有key
     */
    public Long deleteByPattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(getRealKey(pattern));
            if (keys != null && !keys.isEmpty()) {
                return redisTemplate.delete(keys);
            }
            return 0L;
        } catch (Exception e) {
            log.warn("Redis deleteByPattern failed: {}", e.getMessage());
            return 0L;
        }
    }

    /**
     * 分布式锁
     */
    public boolean tryLock(String key, String value, long timeout, TimeUnit unit) {
        try {
            return Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(getRealKey(key), value, timeout, unit)
            );
        } catch (Exception e) {
            log.warn("Redis tryLock failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 释放锁（仅当值匹配时）
     */
    public boolean releaseLock(String key, String value) {
        try {
            String script = 
                "if redis.call('get', KEYS[1]) == ARGV[1] then " +
                "    return redis.call('del', KEYS[1]) " +
                "else " +
                "    return 0 " +
                "end";
            Long result = redisTemplate.execute(
                new org.springframework.data.redis.core.script.DefaultRedisScript<>(script, Long.class),
                java.util.Collections.singletonList(getRealKey(key)),
                value
            );
            return result != null && result == 1L;
        } catch (Exception e) {
            log.warn("Redis releaseLock failed: {}", e.getMessage());
            return false;
        }
    }

    private String getRealKey(String key) {
        return CACHE_PREFIX + key;
    }
}