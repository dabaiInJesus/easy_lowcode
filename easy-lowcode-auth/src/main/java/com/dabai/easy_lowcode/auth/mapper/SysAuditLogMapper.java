package com.dabai.easy_lowcode.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.dabai.easy_lowcode.auth.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * 系统审计日志Mapper
 */
@Mapper
public interface SysAuditLogMapper extends BaseMapper<SysAuditLog> {
}