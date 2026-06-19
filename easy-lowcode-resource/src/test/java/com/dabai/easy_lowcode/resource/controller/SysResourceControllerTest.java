package com.dabai.easy_lowcode.resource.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.mapper.SysResourceMapper;
import com.dabai.easy_lowcode.resource.mapper.SysRoleResourceMapper;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
import com.dabai.easy_lowcode.resource.service.ResourceExecutionService;
import com.dabai.easy_lowcode.resource.service.SysResourceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SysResourceController.class)
@AutoConfigureMockMvc(addFilters = false)
class SysResourceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SysResourceService resourceService;

    @MockitoBean
    private DynamicDataService dynamicDataService;

    @MockitoBean
    private ResourceExecutionService executionService;

    @MockitoBean
    private SysResourceMapper sysResourceMapper;

    @MockitoBean
    private SysRoleResourceMapper sysRoleResourceMapper;

    @MockitoBean
    private SqlSessionFactory sqlSessionFactory;

    @Autowired
    private ObjectMapper objectMapper;

    private SysResource sampleResource;

    @BeforeEach
    void setUp() {
        Configuration configuration = new Configuration();
        when(sqlSessionFactory.getConfiguration()).thenReturn(configuration);
        when(sqlSessionFactory.openSession()).thenReturn(mock(SqlSession.class));

        sampleResource = new SysResource();
        sampleResource.setId(1L);
        sampleResource.setResourceName("用户管理");
        sampleResource.setResourceCode("system:user");
        sampleResource.setResourceType("menu");
        sampleResource.setParentId(0L);
        sampleResource.setPath("/system/user");
        sampleResource.setSortOrder(1);
        sampleResource.setStatus(1);
    }

    @Test
    void page_returnsPagedResults() throws Exception {
        Page<SysResource> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleResource));
        page.setTotal(1);
        when(resourceService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/resource/page")
                        .param("current", "1")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].resourceName", is("用户管理")))
                .andExpect(jsonPath("$.data.total", is(1)));
    }

    @Test
    void page_withKeyword_filtersByResourceName() throws Exception {
        Page<SysResource> page = new Page<>(1, 10);
        page.setRecords(List.of(sampleResource));
        page.setTotal(1);
        when(resourceService.page(any(Page.class), any())).thenReturn(page);

        mockMvc.perform(get("/api/resource/page")
                        .param("current", "1")
                        .param("size", "10")
                        .param("keyword", "用户"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(resourceService).page(any(Page.class), any());
    }

    @Test
    void getById_returnsResource() throws Exception {
        when(resourceService.getById(1L)).thenReturn(sampleResource);

        mockMvc.perform(get("/api/resource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.resourceName", is("用户管理")))
                .andExpect(jsonPath("$.data.resourceCode", is("system:user")));
    }

    @Test
    void create_success() throws Exception {
        when(resourceService.save(any(SysResource.class))).thenReturn(true);

        mockMvc.perform(post("/api/resource")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleResource)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(resourceService).save(any(SysResource.class));
    }

    @Test
    void delete_noChildren_success() throws Exception {
        when(resourceService.count(any())).thenReturn(0L);
        when(resourceService.removeById(1L)).thenReturn(true);

        mockMvc.perform(delete("/api/resource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("删除成功"));

        verify(resourceService).removeById(1L);
    }

    @Test
    void delete_hasChildren_returnsError() throws Exception {
        when(resourceService.count(any())).thenReturn(3L);

        mockMvc.perform(delete("/api/resource/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.message").value("存在子资源，无法删除"));

        verify(resourceService, never()).removeById(anyLong());
    }

    @Test
    void getResourceTree_returnsTree() throws Exception {
        SysResource root = new SysResource();
        root.setId(1L);
        root.setResourceName("系统管理");
        root.setResourceType("menu");
        root.setChildren(List.of(sampleResource));
        when(resourceService.getAllResourceTree()).thenReturn(List.of(root));

        mockMvc.perform(get("/api/resource/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].children", hasSize(1)));
    }

    @Test
    void assignResources_success() throws Exception {
        doNothing().when(resourceService).assignResourcesToRole(eq(1L), anyList());

        List<Long> resourceIds = List.of(1L, 2L, 3L);
        mockMvc.perform(post("/api/resource/role/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resourceIds)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(resourceService).assignResourcesToRole(eq(1L), anyList());
    }
}
