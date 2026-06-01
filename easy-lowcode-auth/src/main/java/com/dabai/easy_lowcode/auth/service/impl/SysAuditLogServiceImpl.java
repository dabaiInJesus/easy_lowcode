package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysAuditLog;
import com.dabai.easy_lowcode.auth.mapper.SysAuditLogMapper;
import com.dabai.easy_lowcode.auth.service.SysAuditLogService;
import com.dabai.easy_lowcode.common.result.PageResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 审计日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuditLogServiceImpl extends ServiceImpl<SysAuditLogMapper, SysAuditLog> implements SysAuditLogService {

    @Override
    @Async
    public void recordLog(SysAuditLog auditLog) {
        try {
            this.save(auditLog);
            log.debug("审计日志记录成功: {} - {}", auditLog.getModule(), auditLog.getAction());
        } catch (Exception e) {
            log.error("审计日志记录失败: {}", e.getMessage());
        }
    }

    @Override
    public PageResult<Map<String, Object>> queryPage(Long current, Long size, String module, String action, String username) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        
        if (module != null && !module.isBlank()) {
            wrapper.like(SysAuditLog::getModule, module);
        }
        if (action != null && !action.isBlank()) {
            wrapper.like(SysAuditLog::getAction, action);
        }
        if (username != null && !username.isBlank()) {
            wrapper.like(SysAuditLog::getUsername, username);
        }
        
        wrapper.orderByDesc(SysAuditLog::getCreateTime);
        
        Page<SysAuditLog> page = this.page(new Page<>(current, size), wrapper);
        
        List<Map<String, Object>> records = page.getRecords().stream()
            .map(this::toMap)
            .collect(Collectors.toList());
        
        return new PageResult<>(records, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<SysAuditLog> queryRecent(Integer limit) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(SysAuditLog::getCreateTime)
               .last("LIMIT " + limit);
        return this.list(wrapper);
    }

    @Override
    public int deleteBeforeDays(int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(SysAuditLog::getCreateTime, before);
        boolean removed = this.remove(wrapper);
        return removed ? 1 : 0;
    }

    private Map<String, Object> toMap(SysAuditLog log) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", log.getId());
        map.put("userId", log.getUserId());
        map.put("username", log.getUsername());
        map.put("module", log.getModule());
        map.put("action", log.getAction());
        map.put("description", log.getDescription());
        map.put("requestMethod", log.getRequestMethod());
        map.put("requestUrl", log.getRequestUrl());
        map.put("ipAddress", log.getIpAddress());
        map.put("operationStatus", log.getOperationStatus());
        map.put("errorMessage", log.getErrorMessage());
        map.put("executionTime", log.getExecutionTime());
        map.put("createTime", log.getCreateTime() != null ? log.getCreateTime().toString() : null);
        return map;
    }
}