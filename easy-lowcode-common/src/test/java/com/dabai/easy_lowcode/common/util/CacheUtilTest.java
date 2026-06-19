package com.dabai.easy_lowcode.common.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.time.Duration;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CacheUtilTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheUtil cacheUtil;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private String prefixed(String key) {
        return "easy-lowcode:" + key;
    }

    @Test
    void set_delegatesToRedis() {
        cacheUtil.set("myKey", "myValue");
        verify(valueOperations).set(prefixed("myKey"), "myValue");
    }

    @Test
    void set_withTimeout_delegatesToRedis() {
        cacheUtil.set("myKey", "myValue", 30, TimeUnit.SECONDS);
        verify(valueOperations).set(prefixed("myKey"), "myValue", 30, TimeUnit.SECONDS);
    }

    @Test
    void set_withDuration_delegatesToRedis() {
        cacheUtil.set("myKey", "myValue", Duration.ofMinutes(5));
        verify(valueOperations).set(prefixed("myKey"), "myValue", Duration.ofMinutes(5));
    }

    @Test
    void get_returnsValue() {
        when(valueOperations.get(prefixed("myKey"))).thenReturn("cached");

        String result = cacheUtil.get("myKey");
        assertThat(result).isEqualTo("cached");
        verify(valueOperations).get(prefixed("myKey"));
    }

    @Test
    void get_onException_returnsNull() {
        when(valueOperations.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        String result = cacheUtil.get("myKey");
        assertThat(result).isNull();
    }

    @Test
    void delete_delegatesToRedis() {
        cacheUtil.delete("myKey");
        verify(redisTemplate).delete(prefixed("myKey"));
    }

    @Test
    void exists_returnsTrueWhenKeyPresent() {
        when(redisTemplate.hasKey(prefixed("myKey"))).thenReturn(true);

        assertThat(cacheUtil.exists("myKey")).isTrue();
    }

    @Test
    void exists_returnsFalseWhenKeyAbsent() {
        when(redisTemplate.hasKey(prefixed("myKey"))).thenReturn(false);

        assertThat(cacheUtil.exists("myKey")).isFalse();
    }

    @Test
    void exists_onException_returnsFalse() {
        when(redisTemplate.hasKey(anyString())).thenThrow(new RuntimeException("Redis down"));

        assertThat(cacheUtil.exists("myKey")).isFalse();
    }

    @Test
    void setWithTtl_delegatesToRedis() {
        cacheUtil.set("myKey", "myValue", 60, TimeUnit.MINUTES);
        verify(valueOperations).set(prefixed("myKey"), "myValue", 60, TimeUnit.MINUTES);
    }

    @Test
    void expire_delegatesToRedis() {
        when(redisTemplate.expire(prefixed("myKey"), 60, TimeUnit.SECONDS)).thenReturn(true);

        boolean result = cacheUtil.expire("myKey", 60, TimeUnit.SECONDS);
        assertThat(result).isTrue();
    }

    @Test
    void getExpire_returnsRemainingTtl() {
        when(redisTemplate.getExpire(prefixed("myKey"), TimeUnit.SECONDS)).thenReturn(120L);

        Long result = cacheUtil.getExpire("myKey");
        assertThat(result).isEqualTo(120L);
    }

    @Test
    void increment_delegatesToRedis() {
        when(valueOperations.increment(prefixed("myKey"))).thenReturn(2L);

        Long result = cacheUtil.increment("myKey");
        assertThat(result).isEqualTo(2L);
    }

    @Test
    void increment_withDelta_delegatesToRedis() {
        when(valueOperations.increment(prefixed("myKey"), 5L)).thenReturn(6L);

        Long result = cacheUtil.increment("myKey", 5L);
        assertThat(result).isEqualTo(6L);
    }

    @Test
    void decrement_delegatesToRedis() {
        when(valueOperations.decrement(prefixed("myKey"))).thenReturn(0L);

        Long result = cacheUtil.decrement("myKey");
        assertThat(result).isEqualTo(0L);
    }

    @Test
    void deleteByPattern_withMatchingKeys_deletesAll() {
        Set<String> keys = Set.of(prefixed("key1"), prefixed("key2"));
        when(redisTemplate.keys(prefixed("pattern*"))).thenReturn(keys);
        when(redisTemplate.delete(keys)).thenReturn(2L);

        Long result = cacheUtil.deleteByPattern("pattern*");
        assertThat(result).isEqualTo(2L);
    }

    @Test
    void deleteByPattern_noMatchingKeys_returnsZero() {
        when(redisTemplate.keys(prefixed("pattern*"))).thenReturn(Collections.emptySet());

        Long result = cacheUtil.deleteByPattern("pattern*");
        assertThat(result).isEqualTo(0L);
    }

    @Test
    void tryLock_acquiresLock() {
        when(valueOperations.setIfAbsent(prefixed("lock:order"), "token123", 30, TimeUnit.SECONDS))
                .thenReturn(true);

        boolean result = cacheUtil.tryLock("lock:order", "token123", 30, TimeUnit.SECONDS);
        assertThat(result).isTrue();
    }

    @Test
    void tryLock_alreadyLocked_returnsFalse() {
        when(valueOperations.setIfAbsent(prefixed("lock:order"), "token123", 30, TimeUnit.SECONDS))
                .thenReturn(false);

        boolean result = cacheUtil.tryLock("lock:order", "token123", 30, TimeUnit.SECONDS);
        assertThat(result).isFalse();
    }

    @Test
    void releaseLock_matchingValue_returnsTrue() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), eq("token123")))
                .thenReturn(1L);

        boolean result = cacheUtil.releaseLock("lock:order", "token123");
        assertThat(result).isTrue();
    }

    @Test
    void releaseLock_nonMatchingValue_returnsFalse() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), eq("token123")))
                .thenReturn(0L);

        boolean result = cacheUtil.releaseLock("lock:order", "token123");
        assertThat(result).isFalse();
    }

    @Test
    void releaseLock_onException_returnsFalse() {
        when(redisTemplate.execute(any(DefaultRedisScript.class), anyList(), anyString()))
                .thenThrow(new RuntimeException("Redis down"));

        boolean result = cacheUtil.releaseLock("lock:order", "token123");
        assertThat(result).isFalse();
    }
}
