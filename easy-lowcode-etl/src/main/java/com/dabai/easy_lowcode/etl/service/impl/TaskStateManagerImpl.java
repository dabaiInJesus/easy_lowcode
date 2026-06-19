package com.dabai.easy_lowcode.etl.service.impl;

import com.dabai.easy_lowcode.etl.service.TaskStateManager;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 任务状态管理器实现
 * 使用 ConcurrentHashMap 跟踪运行中的 ETL 任务
 */
@Slf4j
@Component
public class TaskStateManagerImpl implements TaskStateManager {

    private final ConcurrentHashMap<Long, Future<?>> runningTasks = new ConcurrentHashMap<>();

    @Override
    public void registerTask(Long taskId, Future<?> future) {
        runningTasks.put(taskId, future);
    }

    @Override
    public Future<?> getRunningTask(Long taskId) {
        return runningTasks.get(taskId);
    }

    @Override
    public boolean stopTask(Long taskId, Runnable onStop) {
        Future<?> future = runningTasks.get(taskId);
        if (future != null && !future.isDone()) {
            boolean cancelled = future.cancel(true);
            if (cancelled) {
                log.info("ETL任务已停止: taskId={}", taskId);
                if (onStop != null) {
                    onStop.run();
                }
            }
            return cancelled;
        }
        log.warn("未找到运行中的ETL任务: taskId={}", taskId);
        return false;
    }

    @Override
    public void unregisterTask(Long taskId) {
        runningTasks.remove(taskId);
    }

    @Override
    public Set<Long> getRunningTaskIds() {
        return runningTasks.keySet();
    }

    @Override
    public boolean isRunning(Long taskId) {
        Future<?> future = runningTasks.get(taskId);
        return future != null && !future.isDone();
    }

    @PreDestroy
    public void shutdown() {
        log.info("关闭任务状态管理器，取消 {} 个运行中的任务", runningTasks.size());
        runningTasks.forEach((taskId, future) -> {
            if (!future.isDone()) {
                future.cancel(true);
                log.info("取消任务: taskId={}", taskId);
            }
        });
        runningTasks.clear();
    }
}
