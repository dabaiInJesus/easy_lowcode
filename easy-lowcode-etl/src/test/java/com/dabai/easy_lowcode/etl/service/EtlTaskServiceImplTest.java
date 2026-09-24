package com.dabai.easy_lowcode.etl.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.mapper.DataSourceConfigMapper;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.mapper.EtlTaskMapper;
import com.dabai.easy_lowcode.etl.service.impl.EtlTaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
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
 * ETL任务服务单元测试
 */
@ExtendWith(MockitoExtension.class)
class EtlTaskServiceImplTest {

    @Mock
    private EtlTaskMapper etlTaskMapper;

    @Mock
    private EtlTaskLogService etlTaskLogService;

    @Mock
    private DataSourceConfigMapper dataSourceConfigMapper;

    @InjectMocks
    private EtlTaskServiceImpl etlTaskService;

    private EtlTask task;

    @BeforeEach
    void setUp() {
        // MP ServiceImpl.baseMapper 声明类型擦除为 BaseMapper，@InjectMocks 注入不了，需手动设置
        ReflectionTestUtils.setField(etlTaskService, "baseMapper", etlTaskMapper);

        task = new EtlTask();
        task.setTaskName("用户数据同步");
        task.setTaskCode("sync_user");
        task.setSourceDatasourceId(1L);
        task.setSourceTable("source_user");
        task.setTargetDatasourceId(2L);
        task.setTargetTable("target_user");
        task.setReadMode("TABLE");
        task.setWriteMode("INSERT");
        task.setBatchSize(1000);
        task.setStatus(1);
    }

    @Test
    void testCreateTaskWithValidData() {
        DataSourceConfig sourceDs = new DataSourceConfig();
        sourceDs.setId(1L);
        sourceDs.setName("源数据源");

        DataSourceConfig targetDs = new DataSourceConfig();
        targetDs.setId(2L);
        targetDs.setName("目标数据源");

        when(dataSourceConfigMapper.selectById(1L)).thenReturn(sourceDs);
        when(dataSourceConfigMapper.selectById(2L)).thenReturn(targetDs);
        when(etlTaskMapper.selectCount(any())).thenReturn(0L);
        when(etlTaskMapper.insert(any(EtlTask.class))).thenReturn(1);

        assertDoesNotThrow(() -> etlTaskService.createTask(task));
    }

    @Test
    void testCreateTaskWithoutName() {
        task.setTaskName(null);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.createTask(task));
        assertTrue(ex.getMessage().contains("任务名称不能为空"));
    }

    @Test
    void testCreateTaskWithoutCode() {
        task.setTaskCode(null);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.createTask(task));
        assertTrue(ex.getMessage().contains("任务编码不能为空"));
    }

    @Test
    void testCreateTaskWithoutSourceDs() {
        task.setSourceDatasourceId(null);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.createTask(task));
        assertTrue(ex.getMessage().contains("源数据源不能为空"));
    }

    @Test
    void testCreateTaskWithoutTargetTable() {
        task.setTargetTable(null);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.createTask(task));
        assertTrue(ex.getMessage().contains("目标表名不能为空"));
    }

    @Test
    void testCreateTaskWithInvalidSourceDs() {
        // 实现校验顺序：源数据源判空/存在性校验在前，查不到即抛出，不会查目标数据源
        when(dataSourceConfigMapper.selectById(1L)).thenReturn(null);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.createTask(task));
        assertTrue(ex.getMessage().contains("源数据源不存在"));
    }

    @Test
    void testExecuteTaskOnDisabledTask() {
        task.setStatus(0);
        task.setId(1L);
        when(etlTaskMapper.selectById(1L)).thenReturn(task);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.executeTask(1L));
        // 实现的提示文案为"任务未启用，无法执行"
        assertTrue(ex.getMessage().contains("任务未启用"));
    }

    @Test
    void testExecuteTaskOnNonExistentTask() {
        when(etlTaskMapper.selectById(999L)).thenReturn(null);
        Exception ex = assertThrows(Exception.class, () -> etlTaskService.executeTask(999L));
        assertTrue(ex.getMessage().contains("任务不存在"));
    }

    @Test
    void testGetTaskHistory() {
        when(etlTaskMapper.selectById(1L)).thenReturn(task);
        // 验证服务正常返回
        assertNotNull(etlTaskService.getById(1L));
    }

    @Test
    void testDefaultValues() {
        // EtlTask 实体在字段上给出了新建任务的合理默认值
        EtlTask newTask = new EtlTask();
        assertEquals("TABLE", newTask.getReadMode());
        assertEquals("INSERT", newTask.getWriteMode());
        assertEquals("MANUAL", newTask.getScheduleType());
        assertEquals(1000, newTask.getBatchSize());
        assertEquals(1, newTask.getStatus());
    }
}
