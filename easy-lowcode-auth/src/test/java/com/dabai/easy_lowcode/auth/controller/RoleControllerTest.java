package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.auth.entity.SysRole;
import com.dabai.easy_lowcode.auth.service.SysRoleService;
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
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RoleController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysRoleService roleService;

    @MockitoBean
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private SysRole sampleRole;

    @BeforeEach
    void setUp() {
        sampleRole = new SysRole();
        sampleRole.setId(1L);
        sampleRole.setRoleName("管理员");
        sampleRole.setRoleCode("admin");
        sampleRole.setDescription("系统管理员");
        sampleRole.setStatus(1);
        sampleRole.setSort(1);
    }

    @Test
    void getRoleList_returnsList() throws Exception {
        List<SysRole> roles = Arrays.asList(sampleRole);
        when(roleService.getRoleList()).thenReturn(roles);

        mockMvc.perform(get("/api/auth/role/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].roleName", is("管理员")))
                .andExpect(jsonPath("$.data[0].roleCode", is("admin")));
    }

    @Test
    void createRole_success() throws Exception {
        when(roleService.count(any())).thenReturn(0L);
        when(roleService.save(any(SysRole.class))).thenReturn(true);

        SysRole newRole = new SysRole();
        newRole.setRoleName("测试角色");
        newRole.setRoleCode("test_role");

        mockMvc.perform(post("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(roleService).save(any(SysRole.class));
    }

    @Test
    void createRole_duplicateCode_returnsError() throws Exception {
        when(roleService.count(any())).thenReturn(1L);

        SysRole duplicateRole = new SysRole();
        duplicateRole.setRoleName("重复角色");
        duplicateRole.setRoleCode("admin");

        mockMvc.perform(post("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicateRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("角色编码已存在"));

        verify(roleService, never()).save(any(SysRole.class));
    }

    @Test
    void createRole_autoGenerateCode_whenCodeBlank() throws Exception {
        when(roleService.count(any())).thenReturn(0L);
        when(roleService.save(any(SysRole.class))).thenReturn(true);

        SysRole newRole = new SysRole();
        newRole.setRoleName("自动编码角色");

        mockMvc.perform(post("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(newRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void updateRole_success() throws Exception {
        when(roleService.count(any())).thenReturn(0L);
        when(roleService.updateById(any(SysRole.class))).thenReturn(true);

        sampleRole.setRoleName("更新后的角色");

        mockMvc.perform(put("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(roleService).updateById(any(SysRole.class));
    }

    @Test
    void updateRole_duplicateCode_returnsError() throws Exception {
        when(roleService.count(any())).thenReturn(1L);

        mockMvc.perform(put("/api/auth/role")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleRole)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("角色编码已存在"));

        verify(roleService, never()).updateById(any(SysRole.class));
    }

    @Test
    void deleteRole_success() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), anyLong())).thenReturn(0);
        when(roleService.removeById(anyLong())).thenReturn(true);

        mockMvc.perform(delete("/api/auth/role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("操作成功"));

        verify(roleService).removeById(1L);
    }

    @Test
    void deleteRole_hasUsers_returnsError() throws Exception {
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), eq(1L))).thenReturn(3);

        mockMvc.perform(delete("/api/auth/role/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("该角色下有 3 个用户关联，请先移除用户关联后再删除"));

        verify(roleService, never()).removeById(anyLong());
    }
}
