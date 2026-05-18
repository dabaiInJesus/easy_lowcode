package com.dabai.easy_lowcode.etl.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.etl.entity.EtlTaskLog;

/**
 * ETL任务日志服务接口
 */
public interface EtlTaskLogService extends IService<EtlTaskLog> {

    /**
     * 记录执行日志
     */
    boolean recordLog(EtlTaskLog log);

    /**
     * 更新执行状态
     */
    boolean updateStatus(Long logId, String status, String errorMessage);

    /**
     * 更新执行日志（完整信息）
     */
    boolean updateLog(Long logId, String status, String endTime, Long readCount, Long writeCount, Long skipCount);

    /**
     * 更新最后一条日志状态（按任务ID）
     */
    boolean updateLastLogStatus(Long taskId, String status);

    /**
     * 获取指定任务的日志列表
     */
    java.util.List<EtlTaskLog> getLogsByTaskId(Long taskId, int limit);
}
