package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.service.DataSourceConfigService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DataSourceConfigController.class)
@AutoConfigureMockMvc(addFilters = false)
class DataSourceConfigControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DataSourceConfigService dataSourceConfigService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private DataSourceConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = new DataSourceConfig();
        sampleConfig.setId(1L);
        sampleConfig.setName("主数据库");
        sampleConfig.setCode("main_db");
        sampleConfig.setDbType("postgresql");
        sampleConfig.setUrl("jdbc:postgresql://localhost:5432/test");
        sampleConfig.setUsername("postgres");
        sampleConfig.setPassword("encrypted_password");
        sampleConfig.setDriverClassName("org.postgresql.Driver");
        sampleConfig.setStatus(1);
    }

    @Test
    void page_returnsPagedResults() throws Exception {
        Page<DataSourceConfig> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleConfig));
        page.setTotal(1);
        when(dataSourceConfigService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/collector/datasource/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].name", is("主数据库")))
                .andExpect(jsonPath("$.data.records[0].password", is("******")))
                .andExpect(jsonPath("$.data.total", is(1)));
    }

    @Test
    void getById_returnsConfig() throws Exception {
        when(dataSourceConfigService.getById(1L)).thenReturn(sampleConfig);

        mockMvc.perform(get("/api/collector/datasource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name", is("主数据库")))
                .andExpect(jsonPath("$.data.code", is("main_db")))
                .andExpect(jsonPath("$.data.dbType", is("postgresql")));
    }

    @Test
    void create_success() throws Exception {
        when(dataSourceConfigService.count(any())).thenReturn(0L);
        when(dataSourceConfigService.save(any(DataSourceConfig.class))).thenReturn(true);

        DataSourceConfig newConfig = new DataSourceConfig();
        newConfig.setName("新数据源");
        newConfig.setCode("new_db");
        newConfig.setDbType("mysql");
        newConfig.setUrl("jdbc:mysql://localhost:3306/new_db");
        newConfig.setUsername("root");
        newConfig.setPassword("123456");

        mockMvc.perform(post("/api/collector/datasource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"));

        verify(dataSourceConfigService).save(any(DataSourceConfig.class));
    }

    @Test
    void create_duplicateCode_returnsError() throws Exception {
        when(dataSourceConfigService.count(any())).thenReturn(1L);

        DataSourceConfig duplicateConfig = new DataSourceConfig();
        duplicateConfig.setName("重复数据源");
        duplicateConfig.setCode("main_db");
        duplicateConfig.setDbType("mysql");
        duplicateConfig.setUrl("jdbc:mysql://localhost:3306/dup");
        duplicateConfig.setUsername("root");
        duplicateConfig.setPassword("123456");

        mockMvc.perform(post("/api/collector/datasource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("数据源编码已存在: main_db"));

        verify(dataSourceConfigService, never()).save(any(DataSourceConfig.class));
    }

    @Test
    void delete_success() throws Exception {
        when(dataSourceConfigService.getById(1L)).thenReturn(sampleConfig);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);
        when(dataSourceConfigService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/collector/datasource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(dataSourceConfigService).removeById(1L);
    }

    @Test
    void delete_hasTableResources_returnsError() throws Exception {
        when(dataSourceConfigService.getById(1L)).thenReturn(sampleConfig);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(3);

        mockMvc.perform(delete("/api/collector/datasource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该数据源下有 3 个表资源引用，请先删除关联的表资源后再删除"));

        verify(dataSourceConfigService, never()).removeById(anyLong());
    }

    @Test
    void testConnection_success() throws Exception {
        when(dataSourceConfigService.testConnection(any(DataSourceConfig.class))).thenReturn(true);

        DataSourceConfig testConfig = new DataSourceConfig();
        testConfig.setDbType("postgresql");
        testConfig.setUrl("jdbc:postgresql://localhost:5432/test");
        testConfig.setUsername("postgres");
        testConfig.setPassword("password");

        mockMvc.perform(post("/api/collector/datasource/test-connection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(testConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", is(true)));
    }

    @Test
    void testConnection_failure_returnsError() throws Exception {
        when(dataSourceConfigService.testConnection(any(DataSourceConfig.class))).thenReturn(false);

        DataSourceConfig failConfig = new DataSourceConfig();
        failConfig.setDbType("mysql");
        failConfig.setUrl("jdbc:mysql://wrong-host:3306/test");
        failConfig.setUsername("root");
        failConfig.setPassword("wrong");

        mockMvc.perform(post("/api/collector/datasource/test-connection")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(failConfig)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("连接失败"));
    }
}
