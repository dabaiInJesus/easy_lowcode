package com.dabai.easy_lowcode.etl.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.etl.entity.EtlTaskLog;
import com.dabai.easy_lowcode.etl.mapper.EtlTaskLogMapper;
import com.dabai.easy_lowcode.etl.service.EtlTaskLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * ETL任务日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlTaskLogServiceImpl extends ServiceImpl<EtlTaskLogMapper, EtlTaskLog> implements EtlTaskLogService {

    @Override
    public boolean recordLog(EtlTaskLog log) {
        return this.save(log);
    }

    @Override
    public boolean updateStatus(Long logId, String status, String errorMessage) {
        LambdaUpdateWrapper<EtlTaskLog> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(EtlTaskLog::getId, logId)
               .set(EtlTaskLog::getExecStatus, status)
               .set(EtlTaskLog::getEndTime, new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date()));
        if (errorMessage != null) {
            wrapper.set(EtlTaskLog::getErrorMessage, errorMessage);
        }
        return this.update(wrapper);
    }
}
