package com.dabai.easy_lowcode.etl.service;

import java.util.Set;
import java.util.concurrent.Future;

/**
 * 任务状态管理器
 * 负责跟踪运行中的 ETL 任务，支持停止操作
 */
public interface TaskStateManager {

    /**
     * 注册运行中的任务
     */
    void registerTask(Long taskId, Future<?> future);

    /**
     * 停止指定任务
     */
    boolean stopTask(Long taskId, Runnable onStop);

    /**
     * 任务执行完成后移除注册
     */
    void unregisterTask(Long taskId);

    /**
     * 获取所有运行中的任务 ID
     */
    Set<Long> getRunningTaskIds();

    /**
     * 判断任务是否正在运行
     */
    boolean isRunning(Long taskId);
}
