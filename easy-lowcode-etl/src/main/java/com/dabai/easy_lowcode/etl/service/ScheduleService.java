package com.dabai.easy_lowcode.etl.service;

public interface ScheduleService {
    void refresh();
    void cancelTask(Long taskId);
    void scheduleTask(Long taskId);
    void unscheduleTask(Long taskId);
}
