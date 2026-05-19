package com.dabai.easy_lowcode.dashboard.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChartCacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOps;

    private ChartCacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cacheService = new ChartCacheService(redisTemplate);
    }

    @Test
    void get_cacheHit_returnsData() throws Exception {
        String json = "[{\"col1\": \"v1\", \"col2\": 123}]";
        when(valueOps.get(anyString())).thenReturn(json);

        Optional<List<Map<String, Object>>> result = cacheService.get(1L, "SELECT * FROM t", 100, 0);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().size());
        assertEquals("v1", result.get().get(0).get("col1"));
    }

    @Test
    void get_cacheMiss_returnsEmpty() {
        when(valueOps.get(anyString())).thenReturn(null);

        Optional<List<Map<String, Object>>> result = cacheService.get(1L, "SELECT * FROM t", 100, 0);

        assertFalse(result.isPresent());
    }

    @Test
    void get_redisError_returnsEmpty() {
        when(valueOps.get(anyString())).thenThrow(new RuntimeException("Redis down"));

        Optional<List<Map<String, Object>>> result = cacheService.get(1L, "SELECT * FROM t", 100, 0);

        assertFalse(result.isPresent());
    }

    @Test
    void put_nullData_doesNotCache() {
        cacheService.put(1L, "SELECT * FROM t", null, 0);

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void put_emptyData_doesNotCache() {
        List<Map<String, Object>> emptyData = new ArrayList<>();
        cacheService.put(1L, "SELECT * FROM t", emptyData, 0);

        verify(valueOps, never()).set(anyString(), anyString(), any(Duration.class));
    }

    @Test
    void put_validData_cachesWithDefaultTTL() {
        List<Map<String, Object>> data = List.of(Map.of("col", "val"));
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        cacheService.put(1L, "SELECT * FROM t", data, 0);

        verify(valueOps).set(anyString(), anyString(), eq(Duration.ofMinutes(5)));
    }

    @Test
    void put_customTTL_usesProvidedTTL() {
        List<Map<String, Object>> data = List.of(Map.of("col", "val"));
        doNothing().when(valueOps).set(anyString(), anyString(), any(Duration.class));

        cacheService.put(1L, "SELECT * FROM t", data, 120);

        verify(valueOps).set(anyString(), anyString(), eq(Duration.ofSeconds(120)));
    }

    @Test
    void invalidate_deletesAllKeysForChart() {
        doReturn(2L).when(redisTemplate).delete((Collection<String>) any());

        cacheService.invalidate(5L);

        verify(redisTemplate).delete((Collection<String>) any());
    }

    @Test
    void invalidate_singleKey_deletesSpecificKey() {
        doReturn(true).when(redisTemplate).delete((String) anyString());

        cacheService.invalidate(5L, "SELECT * FROM t");

        verify(redisTemplate).delete((String) anyString());
    }
}
