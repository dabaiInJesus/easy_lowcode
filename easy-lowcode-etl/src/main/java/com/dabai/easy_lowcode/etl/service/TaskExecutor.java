package com.dabai.easy_lowcode.etl.service;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.etl.entity.EtlTask;

import java.util.concurrent.Future;

/**
 * ETL 任务执行器
 * 负责实际的 ETL 数据抽取、转换、加载操作
 */
public interface TaskExecutor {

    /**
     * 异步执行 ETL 任务
     */
    void executeAsync(EtlTask task, DataSourceConfig sourceDs, DataSourceConfig targetDs, Long logId);

    /**
     * 停止正在执行的任务
     */
    boolean stop(Long taskId);
}
