package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.common.util.CacheUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(HealthController.class)
@AutoConfigureMockMvc(addFilters = false)
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private HealthController healthController;

    @MockitoBean
    private CacheUtil cacheUtil;

    @MockitoBean
    private DataSource dataSource;

    @BeforeEach
    void setUp() throws Exception {
        Connection mockConn = mock(Connection.class);
        DatabaseMetaData mockMetaData = mock(DatabaseMetaData.class);
        when(dataSource.getConnection()).thenReturn(mockConn);
        when(mockConn.isValid(5)).thenReturn(true);
        when(mockConn.getMetaData()).thenReturn(mockMetaData);
        when(mockMetaData.getURL()).thenReturn("jdbc:postgresql://localhost:5432/easy_lowcode");
    }

    @Test
    void check_allHealthy_returnsOK() throws Exception {
        when(cacheUtil.get(anyString())).thenReturn("ok");

        mockMvc.perform(get("/api/auth/health/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OK")))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.components.database.status", is("UP")))
                .andExpect(jsonPath("$.components.redis.status", is("UP")));
    }

    @Test
    void check_databaseDown_returnsDegraded() throws Exception {
        Connection mockConn = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(mockConn);
        when(mockConn.isValid(5)).thenReturn(false);
        when(cacheUtil.get(anyString())).thenReturn("ok");

        mockMvc.perform(get("/api/auth/health/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DEGRADED")))
                .andExpect(jsonPath("$.components.database.status", is("DOWN")));
    }

    @Test
    void check_redisDown_returnsDegraded() throws Exception {
        when(cacheUtil.get(anyString())).thenReturn(null);

        mockMvc.perform(get("/api/auth/health/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("DEGRADED")))
                .andExpect(jsonPath("$.components.redis.status", is("DOWN")));
    }

    @Test
    void check_noDataSource_returnsUnknown() throws Exception {
        ReflectionTestUtils.setField(healthController, "dataSource", null);

        when(cacheUtil.get(anyString())).thenReturn("ok");

        mockMvc.perform(get("/api/auth/health/check"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.database.status", is("UNKNOWN")));
    }

    @Test
    void info_returnsSystemInfo() throws Exception {
        mockMvc.perform(get("/api/auth/health/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application", is("Easy Lowcode Platform")))
                .andExpect(jsonPath("$.version", is("1.0.0-SNAPSHOT")))
                .andExpect(jsonPath("$.javaVersion").exists())
                .andExpect(jsonPath("$.osName").exists())
                .andExpect(jsonPath("$.memory.total").exists())
                .andExpect(jsonPath("$.memory.used").exists())
                .andExpect(jsonPath("$.memory.free").exists())
                .andExpect(jsonPath("$.memory.max").exists())
                .andExpect(jsonPath("$.threads.count").isNumber())
                .andExpect(jsonPath("$.versions.springBoot").exists());
    }

    @Test
    void ready_allOk_returnsReady() throws Exception {
        when(cacheUtil.get(anyString())).thenReturn("ok");

        mockMvc.perform(get("/api/auth/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready", is(true)));
    }

    @Test
    void ready_cacheThrows_returnsNotReady() throws Exception {
        doThrow(new RuntimeException("Redis connection refused"))
                .when(cacheUtil).set(anyString(), anyString(), any(java.time.Duration.class));

        mockMvc.perform(get("/api/auth/health/ready"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready", is(false)))
                .andExpect(jsonPath("$.reason").exists());
    }

    @Test
    void live_returnsAlive() throws Exception {
        mockMvc.perform(get("/api/auth/health/live"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.alive", is(true)))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}
