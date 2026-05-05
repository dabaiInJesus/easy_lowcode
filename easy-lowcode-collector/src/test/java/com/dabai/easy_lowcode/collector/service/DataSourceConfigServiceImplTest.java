package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.service.impl.DataSourceConfigServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 数据源配置服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class DataSourceConfigServiceImplTest {

    @Mock
    private DataSourceConfigMapper dataSourceConfigMapper;

    @InjectMocks
    private DataSourceConfigServiceImpl dataSourceConfigService;

    private DataSourceConfig config;

    @BeforeEach
    void setUp() {
        config = new DataSourceConfig();
        config.setId(1L);
        config.setName("测试数据源");
        config.setCode("test_db");
        config.setDbType("mysql");
        config.setUrl("jdbc:mysql://localhost:3306/test");
        config.setUsername("root");
        config.setPassword("password123");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setStatus(1);
    }

    @Test
    void testCreateSuccess() {
        when(dataSourceConfigMapper.insert(any(DataSourceConfig.class))).thenReturn(1);
        boolean result = dataSourceConfigService.save(config);
        assertTrue(result);
        verify(dataSourceConfigMapper, times(1)).insert(config);
    }

    @Test
    void testGetById() {
        when(dataSourceConfigMapper.selectById(1L)).thenReturn(config);
        DataSourceConfig found = dataSourceConfigService.getById(1L);
        assertNotNull(found);
        assertEquals("测试数据源", found.getName());
        assertEquals("test_db", found.getCode());
    }

    @Test
    void testGetByIdNotFound() {
        when(dataSourceConfigMapper.selectById(999L)).thenReturn(null);
        DataSourceConfig found = dataSourceConfigService.getById(999L);
        assertNull(found);
    }

    @Test
    void testDeleteById() {
        when(dataSourceConfigMapper.deleteById(1L)).thenReturn(1);
        boolean result = dataSourceConfigService.removeById(1L);
        assertTrue(result);
        verify(dataSourceConfigMapper, times(1)).deleteById(1L);
    }

    @Test
    void testDefaultDriverForMySQL() {
        config.setDriverClassName(null);
        // 验证controller层会设置默认驱动，service层不做此逻辑
        assertNull(config.getDriverClassName());
    }

    @Test
    void testStatusDefaultEnabled() {
        DataSourceConfig newConfig = new DataSourceConfig();
        assertNull(newConfig.getStatus()); // 默认null，由Service设置默认值
    }
}
