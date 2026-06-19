package com.dabai.easy_lowcode.dashboard.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.dashboard.entity.Dashboard;
import com.dabai.easy_lowcode.dashboard.entity.DashboardChart;
import com.dabai.easy_lowcode.dashboard.service.DashboardService;
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
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc(addFilters = false)
class DashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    @Autowired
    private ObjectMapper objectMapper;

    private Dashboard sampleDashboard;

    @BeforeEach
    void setUp() {
        sampleDashboard = new Dashboard();
        sampleDashboard.setId(1L);
        sampleDashboard.setName("销售大屏");
        sampleDashboard.setCode("sales_dashboard");
        sampleDashboard.setTitle("销售数据大屏");
        sampleDashboard.setDescription("销售数据可视化");
        sampleDashboard.setStatus(0);
        sampleDashboard.setSortOrder(1);
        sampleDashboard.setWidth(1920);
        sampleDashboard.setHeight(1080);
    }

    @Test
    void page_returnsPagedResults() throws Exception {
        Page<Dashboard> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleDashboard));
        page.setTotal(1);
        when(dashboardService.page(any(Page.class), any())).thenReturn(page);
        when(dashboardService.getCharts(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/dashboard/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].name", is("销售大屏")))
                .andExpect(jsonPath("$.data.total", is(1)));
    }

    @Test
    void list_returnsAllDashboards() throws Exception {
        when(dashboardService.list(any(com.baomidou.mybatisplus.core.conditions.Wrapper.class))).thenReturn(Arrays.asList(sampleDashboard));

        mockMvc.perform(get("/api/dashboard/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].name", is("销售大屏")));
    }

    @Test
    void getById_returnsDashboard() throws Exception {
        when(dashboardService.getById(1L)).thenReturn(sampleDashboard);

        mockMvc.perform(get("/api/dashboard/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name", is("销售大屏")))
                .andExpect(jsonPath("$.data.code", is("sales_dashboard")));
    }

    @Test
    void getById_notFound_returnsError() throws Exception {
        when(dashboardService.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/dashboard/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("大屏不存在"));
    }

    @Test
    void create_success() throws Exception {
        when(dashboardService.createDashboard(any(Dashboard.class))).thenReturn(true);

        Dashboard newDashboard = new Dashboard();
        newDashboard.setName("新大屏");
        newDashboard.setCode("new_dashboard");

        mockMvc.perform(post("/api/dashboard")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newDashboard)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"));

        verify(dashboardService).createDashboard(any(Dashboard.class));
    }

    @Test
    void delete_success() throws Exception {
        when(dashboardService.getById(1L)).thenReturn(sampleDashboard);
        when(dashboardService.getCharts(1L)).thenReturn(Collections.emptyList());
        when(dashboardService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/dashboard/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(dashboardService).removeById(1L);
    }

    @Test
    void publish_success() throws Exception {
        when(dashboardService.publishDashboard(1L)).thenReturn(true);

        mockMvc.perform(post("/api/dashboard/1/publish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("发布成功"));

        verify(dashboardService).publishDashboard(1L);
    }
}
