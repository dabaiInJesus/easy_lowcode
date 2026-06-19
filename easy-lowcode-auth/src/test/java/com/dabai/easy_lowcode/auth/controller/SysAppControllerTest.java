package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.auth.entity.SysApp;
import com.dabai.easy_lowcode.auth.service.SysAppService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysAppController.class)
@AutoConfigureMockMvc(addFilters = false)
class SysAppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysAppService sysAppService;

    @Autowired
    private ObjectMapper objectMapper;

    private SysApp sampleApp;

    @BeforeEach
    void setUp() {
        sampleApp = new SysApp();
        sampleApp.setId(1L);
        sampleApp.setAppName("测试应用");
        sampleApp.setAppCode("test_app");
        sampleApp.setAppUrl("http://localhost:3000");
        sampleApp.setClientId("client_001");
        sampleApp.setClientSecret("secret_001");
        sampleApp.setRedirectUri("http://localhost:3000/callback");
        sampleApp.setStatus(1);
        sampleApp.setSort(1);
    }

    @Test
    void getPage_returnsPagedResults() throws Exception {
        Page<SysApp> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleApp));
        page.setTotal(1);
        when(sysAppService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/auth/app/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].appName", is("测试应用")))
                .andExpect(jsonPath("$.data.total", is(1)));
    }

    @Test
    void getPage_withKeyword_filtersByName() throws Exception {
        Page<SysApp> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleApp));
        page.setTotal(1);
        when(sysAppService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/auth/app/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "测试"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)));
    }

    @Test
    void getPage_emptyResult() throws Exception {
        Page<SysApp> page = new Page<>(1, 10);
        page.setRecords(List.of());
        page.setTotal(0);
        when(sysAppService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/auth/app/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(0)))
                .andExpect(jsonPath("$.data.total", is(0)));
    }

    @Test
    void getList_returnsAllApps() throws Exception {
        List<SysApp> apps = Arrays.asList(sampleApp);
        when(sysAppService.getAppList()).thenReturn(apps);

        mockMvc.perform(get("/api/auth/app/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].appName", is("测试应用")));
    }

    @Test
    void getById_returnsApp() throws Exception {
        when(sysAppService.getById(1L)).thenReturn(sampleApp);

        mockMvc.perform(get("/api/auth/app/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.appName", is("测试应用")))
                .andExpect(jsonPath("$.data.appCode", is("test_app")))
                .andExpect(jsonPath("$.data.clientId", is("client_001")));
    }

    @Test
    void create_app_success() throws Exception {
        when(sysAppService.save(any(SysApp.class))).thenReturn(true);

        SysApp newApp = new SysApp();
        newApp.setAppName("新应用");
        newApp.setAppCode("new_app");

        mockMvc.perform(post("/api/auth/app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newApp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(sysAppService).save(any(SysApp.class));
    }

    @Test
    void update_app_success() throws Exception {
        when(sysAppService.updateById(any(SysApp.class))).thenReturn(true);

        sampleApp.setAppName("更新后的应用");

        mockMvc.perform(put("/api/auth/app")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleApp)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(sysAppService).updateById(any(SysApp.class));
    }

    @Test
    void delete_app_success() throws Exception {
        when(sysAppService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/auth/app/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(sysAppService).removeById(1L);
    }
}
