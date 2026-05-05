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
}
