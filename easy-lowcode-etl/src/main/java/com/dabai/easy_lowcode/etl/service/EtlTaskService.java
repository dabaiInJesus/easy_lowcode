package com.dabai.easy_lowcode.etl.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.etl.entity.EtlTask;

import java.util.List;
import java.util.Map;

/**
 * ETL任务服务接口
 */
public interface EtlTaskService extends IService<EtlTask> {

    /**
     * 创建ETL任务
     */
    boolean createTask(EtlTask task);

    /**
     * 更新ETL任务
     */
    boolean updateTask(EtlTask task);

    /**
     * 执行ETL任务
     *
     * @param taskId 任务ID
     * @return 执行日志ID
     */
    Long executeTask(Long taskId);

    /**
     * 停止ETL任务
     */
    boolean stopTask(Long taskId);

    /**
     * 测试源数据源连接
     */
    boolean testSourceConnection(Long datasourceId);

    /**
     * 测试目标数据源连接
     */
    boolean testTargetConnection(Long datasourceId);

    /**
     * 扫描源表结构
     */
    List<Map<String, Object>> scanSourceColumns(Long datasourceId, String tableName);

    /**
     * 扫描目标表结构
     */
    List<Map<String, Object>> scanTargetColumns(Long datasourceId, String tableName);

    /**
     * 预览源数据
     */
    List<Map<String, Object>> previewSourceData(Long taskId, int limit);

    /**
     * 获取任务执行历史
     */
    List<Map<String, Object>> getTaskHistory(Long taskId);
}
