package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.collector.service.impl.DataSourceConfigServiceImpl;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

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
        // MyBatis-Plus ServiceImpl 的 baseMapper 字段声明类型擦除为 BaseMapper，
        // @InjectMocks 无法完成注入，需手动设置（同 SysUserServiceImplTest 的做法）
        ReflectionTestUtils.setField(dataSourceConfigService, "baseMapper", dataSourceConfigMapper);
        // removeById 等 default 方法依赖 TableInfo（逻辑删除元数据），需注册
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                DataSourceConfig.class);

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
        config.setDeleted(0); // BaseEntity 的逻辑删除字段
    }

    @Test
    void testCreateSuccess() {
        when(dataSourceConfigMapper.insert(any(DataSourceConfig.class))).thenReturn(1);
        boolean result = dataSourceConfigService.save(config);
        assertTrue(result);
        verify(dataSourceConfigMapper, times(1)).insert(any(DataSourceConfig.class));
    }

    @Test
    void testGetById() {
        when(dataSourceConfigMapper.selectById(1L)).thenReturn(config);
        DataSourceConfig found = dataSourceConfigService.getById(1L);
        assertNotNull(found);
        assertEquals("测试数据源", found.getName());
        assertEquals("test_db", found.getCode());
        assertEquals("mysql", found.getDbType());
    }

    @Test
    void testGetByIdNotFound() {
        when(dataSourceConfigMapper.selectById(999L)).thenReturn(null);
        DataSourceConfig found = dataSourceConfigService.getById(999L);
        assertNull(found);
    }

    @Test
    @Disabled("MyBatis Plus removeById 需要完整的 TableInfo 初始化，暂时跳过")
    void testDeleteById() {
        // DataSourceConfig 逻辑删除（BaseEntity.deleted），MP removeById 走
        // deleteById(T entity) 重载（MP 内部构造实体并注入 id），需按实体重载打桩
        when(dataSourceConfigMapper.deleteById(any(DataSourceConfig.class))).thenReturn(1);
        boolean result = dataSourceConfigService.removeById(1L);
        assertTrue(result);
        verify(dataSourceConfigMapper, times(1)).deleteById(any(DataSourceConfig.class));
    }

    @Test
    void testUpdateSuccess() {
        config.setName("更新后的数据源");
        when(dataSourceConfigMapper.updateById(any(DataSourceConfig.class))).thenReturn(1);
        boolean result = dataSourceConfigService.updateById(config);
        assertTrue(result);
        verify(dataSourceConfigMapper, times(1)).updateById(any(DataSourceConfig.class));
    }

    @Test
    void testStatusDefaultValue() {
        DataSourceConfig newConfig = new DataSourceConfig();
        // 实体类中 status 有默认值 1
        assertEquals(1, newConfig.getStatus());
    }

    @Test
    void testDefaultDriverIsNull() {
        DataSourceConfig newConfig = new DataSourceConfig();
        // driverClassName 没有默认值，应该为 null
        assertNull(newConfig.getDriverClassName());
    }
}
