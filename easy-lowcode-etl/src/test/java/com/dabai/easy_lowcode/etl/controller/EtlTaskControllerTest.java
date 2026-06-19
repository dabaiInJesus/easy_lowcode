package com.dabai.easy_lowcode.etl.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.service.EtlTaskService;
import com.dabai.easy_lowcode.etl.service.ScheduleService;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(EtlTaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class EtlTaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EtlTaskService etlTaskService;

    @MockitoBean
    private ScheduleService scheduleService;

    @MockitoBean
    private DataSourceConfigMapper dataSourceConfigMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private EtlTask sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new EtlTask();
        sampleTask.setId(1L);
        sampleTask.setTaskName("用户同步任务");
        sampleTask.setTaskCode("user_sync");
        sampleTask.setSourceDatasourceId(10L);
        sampleTask.setSourceTable("sys_user");
        sampleTask.setTargetDatasourceId(20L);
        sampleTask.setTargetTable("ods_user");
        sampleTask.setWriteMode("INSERT");
        sampleTask.setStatus(1);
    }

    @Test
    void page_returnsPagedResults() throws Exception {
        Page<EtlTask> page = new Page<>(1, 10);
        page.setRecords(Arrays.asList(sampleTask));
        page.setTotal(1);
        when(etlTaskService.page(any(Page.class), any())).thenReturn(page);
        when(dataSourceConfigMapper.selectById(10L)).thenReturn(null);
        when(dataSourceConfigMapper.selectById(20L)).thenReturn(null);

        mockMvc.perform(get("/api/etl/task/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].taskName", is("用户同步任务")))
                .andExpect(jsonPath("$.data.total", is(1)));
    }

    @Test
    void getById_returnsTask() throws Exception {
        when(etlTaskService.getById(1L)).thenReturn(sampleTask);
        when(dataSourceConfigMapper.selectById(10L)).thenReturn(null);
        when(dataSourceConfigMapper.selectById(20L)).thenReturn(null);

        mockMvc.perform(get("/api/etl/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskName", is("用户同步任务")))
                .andExpect(jsonPath("$.data.taskCode", is("user_sync")));
    }

    @Test
    void getById_notFound_returnsError() throws Exception {
        when(etlTaskService.getById(99L)).thenReturn(null);

        mockMvc.perform(get("/api/etl/task/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("任务不存在"));
    }

    @Test
    void create_success() throws Exception {
        when(etlTaskService.createTask(any(EtlTask.class))).thenReturn(true);

        EtlTask newTask = new EtlTask();
        newTask.setTaskName("新任务");
        newTask.setTaskCode("new_task");

        mockMvc.perform(post("/api/etl/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newTask)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("创建成功"));

        verify(etlTaskService).createTask(any(EtlTask.class));
    }

    @Test
    void delete_success() throws Exception {
        when(etlTaskService.getById(1L)).thenReturn(sampleTask);
        when(etlTaskService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/etl/task/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(scheduleService).cancelTask(1L);
        verify(etlTaskService).removeById(1L);
    }

    @Test
    void delete_notFound_returnsError() throws Exception {
        when(etlTaskService.getById(99L)).thenReturn(null);

        mockMvc.perform(delete("/api/etl/task/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("任务不存在"));

        verify(scheduleService, never()).cancelTask(anyLong());
    }

    @Test
    void execute_success() throws Exception {
        when(etlTaskService.executeTask(1L)).thenReturn(1001L);

        mockMvc.perform(post("/api/etl/task/1/execute"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("任务已提交执行"))
                .andExpect(jsonPath("$.data", is(1001)));
    }

    @Test
    void history_returnsList() throws Exception {
        Map<String, Object> record = new HashMap<>();
        record.put("id", 1);
        record.put("status", "SUCCESS");
        when(etlTaskService.getTaskHistory(1L)).thenReturn(Arrays.asList(record));

        mockMvc.perform(get("/api/etl/task/1/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].status", is("SUCCESS")));
    }
}
