package com.dabai.easy_lowcode.etl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dabai.easy_lowcode.etl.entity.EtlTask;
import com.dabai.easy_lowcode.etl.mapper.EtlTaskMapper;
import com.dabai.easy_lowcode.etl.service.EtlTaskService;
import com.dabai.easy_lowcode.etl.service.ScheduleService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

@Slf4j
@Service
public class ScheduleServiceImpl implements ScheduleService {

    private final EtlTaskMapper etlTaskMapper;

    @Autowired @Lazy
    private EtlTaskService etlTaskService;

    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    public ScheduleServiceImpl(EtlTaskMapper etlTaskMapper) {
        this.etlTaskMapper = etlTaskMapper;
    }

    private final Map<Long, ScheduledFuture<?>> scheduledJobs = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        taskScheduler = new ThreadPoolTaskScheduler();
        taskScheduler.setPoolSize(10);
        taskScheduler.setThreadNamePrefix("scheduled-etl-");
        taskScheduler.setWaitForTasksToCompleteOnShutdown(true);
        taskScheduler.setAwaitTerminationSeconds(30);
        taskScheduler.initialize();

        refresh();

        taskScheduler.scheduleAtFixedRate(this::refresh, Duration.ofSeconds(60));
        log.info("ETL调度引擎已启动");
    }

    @PreDestroy
    public void destroy() {
        scheduledJobs.values().forEach(f -> f.cancel(false));
        scheduledJobs.clear();
        if (taskScheduler != null) {
            taskScheduler.shutdown();
        }
        log.info("ETL调度引擎已关闭");
    }

    @Override
    public void refresh() {
        try {
            List<EtlTask> tasks = etlTaskMapper.selectList(
                    new LambdaQueryWrapper<EtlTask>()
                            .eq(EtlTask::getStatus, 1)
                            .in(EtlTask::getScheduleType, "CRON", "INTERVAL")
            );

            for (EtlTask task : tasks) {
                if (!scheduledJobs.containsKey(task.getId())) {
                    scheduleTask(task.getId());
                }
            }

            scheduledJobs.keySet().removeIf(id ->
                    tasks.stream().noneMatch(t -> t.getId().equals(id)));
        } catch (Exception e) {
            log.error("刷新调度任务失败", e);
        }
    }

    @Override
    public void scheduleTask(Long taskId) {
        EtlTask task = etlTaskMapper.selectById(taskId);
        if (task == null || task.getStatus() != 1) return;
        if (scheduledJobs.containsKey(taskId)) return;

        ScheduledFuture<?> future;
        try {
            if ("CRON".equals(task.getScheduleType()) && task.getCronExpression() != null) {
                CronTrigger trigger = new CronTrigger(task.getCronExpression());
                future = taskScheduler.schedule(() -> execute(taskId), trigger);
                log.info("注册CRON调度: taskId={}, cron={}", taskId, task.getCronExpression());
            } else if ("INTERVAL".equals(task.getScheduleType()) && task.getIntervalSeconds() != null) {
                future = taskScheduler.scheduleAtFixedRate(
                        () -> execute(taskId),
                        Duration.ofSeconds(task.getIntervalSeconds()));
                log.info("注册INTERVAL调度: taskId={}, interval={}s", taskId, task.getIntervalSeconds());
            } else {
                return;
            }
            scheduledJobs.put(taskId, future);
        } catch (Exception e) {
            log.error("注册调度失败: taskId={}", taskId, e);
        }
    }

    @Override
    public void cancelTask(Long taskId) {
        ScheduledFuture<?> future = scheduledJobs.remove(taskId);
        if (future != null && !future.isDone()) {
            future.cancel(false);
            log.info("取消调度: taskId={}", taskId);
        }
    }

    @Override
    public void unscheduleTask(Long taskId) {
        cancelTask(taskId);
    }

    private void execute(Long taskId) {
        try {
            etlTaskService.executeTask(taskId);
        } catch (Exception e) {
            log.error("调度执行ETL任务失败: taskId={}", taskId, e);
        }
    }
}
