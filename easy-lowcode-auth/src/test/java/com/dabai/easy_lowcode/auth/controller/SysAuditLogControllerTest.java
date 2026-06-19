package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.auth.entity.SysAuditLog;
import com.dabai.easy_lowcode.auth.service.SysAuditLogService;
import com.dabai.easy_lowcode.common.result.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysAuditLogController.class)
@AutoConfigureMockMvc(addFilters = false)
class SysAuditLogControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysAuditLogService auditLogService;

    @BeforeEach
    void setUp() {
    }

    @Test
    void page_returnsPagedResults() throws Exception {
        Map<String, Object> logEntry = new LinkedHashMap<>();
        logEntry.put("id", 1L);
        logEntry.put("username", "admin");
        logEntry.put("module", "用户管理");
        logEntry.put("action", "登录");
        logEntry.put("createTime", "2025-01-01 12:00:00");

        PageResult<Map<String, Object>> pageResult = new PageResult<>(
                Arrays.asList(logEntry), 1, 1, 20);
        when(auditLogService.queryPage(1L, 20L, null, null, null)).thenReturn(pageResult);

        mockMvc.perform(get("/api/auth/audit/page")
                        .param("current", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].username", is("admin")))
                .andExpect(jsonPath("$.data.records[0].module", is("用户管理")));
    }

    @Test
    void page_withFilters() throws Exception {
        PageResult<Map<String, Object>> pageResult = new PageResult<>(
                List.of(), 0, 1, 20);
        when(auditLogService.queryPage(1L, 20L, "系统", "删除", "admin")).thenReturn(pageResult);

        mockMvc.perform(get("/api/auth/audit/page")
                        .param("current", "1")
                        .param("size", "20")
                        .param("module", "系统")
                        .param("action", "删除")
                        .param("username", "admin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(0)));
    }

    @Test
    void page_emptyResult() throws Exception {
        PageResult<Map<String, Object>> pageResult = new PageResult<>(
                List.of(), 0, 1, 20);
        when(auditLogService.queryPage(1L, 20L, null, null, null)).thenReturn(pageResult);

        mockMvc.perform(get("/api/auth/audit/page"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total", is(0)));
    }

    @Test
    void recent_returnsLogs() throws Exception {
        SysAuditLog log1 = new SysAuditLog();
        log1.setId(1L);
        log1.setUsername("admin");
        log1.setModule("用户管理");
        log1.setAction("登录");
        log1.setOperationStatus("SUCCESS");

        SysAuditLog log2 = new SysAuditLog();
        log2.setId(2L);
        log2.setUsername("user1");
        log2.setModule("数据查询");
        log2.setAction("查看");
        log2.setOperationStatus("SUCCESS");

        when(auditLogService.queryRecent(20)).thenReturn(Arrays.asList(log1, log2));

        mockMvc.perform(get("/api/auth/audit/recent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(2)))
                .andExpect(jsonPath("$.data[0].username", is("admin")))
                .andExpect(jsonPath("$.data[1].username", is("user1")));
    }

    @Test
    void recent_withCustomLimit() throws Exception {
        when(auditLogService.queryRecent(5)).thenReturn(List.of());

        mockMvc.perform(get("/api/auth/audit/recent")
                        .param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(auditLogService).queryRecent(5);
    }

    @Test
    void clean_deletesOldLogs() throws Exception {
        when(auditLogService.deleteBeforeDays(90)).thenReturn(15);

        mockMvc.perform(delete("/api/auth/audit/clean")
                        .param("days", "90"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已清理 15 条历史日志"));

        verify(auditLogService).deleteBeforeDays(90);
    }

    @Test
    void clean_withCustomDays() throws Exception {
        when(auditLogService.deleteBeforeDays(30)).thenReturn(50);

        mockMvc.perform(delete("/api/auth/audit/clean")
                        .param("days", "30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已清理 50 条历史日志"));

        verify(auditLogService).deleteBeforeDays(30);
    }

    @Test
    void clean_noLogsDeleted() throws Exception {
        when(auditLogService.deleteBeforeDays(90)).thenReturn(0);

        mockMvc.perform(delete("/api/auth/audit/clean"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("已清理 0 条历史日志"));
    }
}
