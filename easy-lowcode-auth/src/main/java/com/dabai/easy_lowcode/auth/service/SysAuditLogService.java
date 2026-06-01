package com.dabai.easy_lowcode.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.auth.entity.SysAuditLog;
import com.dabai.easy_lowcode.common.result.PageResult;

import java.util.List;
import java.util.Map;

/**
 * 审计日志服务接口
 */
public interface SysAuditLogService extends IService<SysAuditLog> {

    /**
     * 记录审计日志
     */
    void recordLog(SysAuditLog log);

    /**
     * 分页查询审计日志
     */
    PageResult<Map<String, Object>> queryPage(Long current, Long size, String module, String action, String username);

    /**
     * 查询最近的操作记录
     */
    List<SysAuditLog> queryRecent(Integer limit);

    /**
     * 删除指定天数之前的日志
     */
    int deleteBeforeDays(int days);
}