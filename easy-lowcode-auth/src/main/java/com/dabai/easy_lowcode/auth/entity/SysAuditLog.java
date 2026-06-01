package com.dabai.easy_lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统审计日志实体
 */
@Data
@TableName("sys_audit_log")
public class SysAuditLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 操作模块
     */
    private String module;

    /**
     * 操作动作
     */
    private String action;

    /**
     * 操作描述
     */
    private String description;

    /**
     * 请求方法
     */
    private String requestMethod;

    /**
     * 请求URL
     */
    private String requestUrl;

    /**
     * 请求参数
     */
    private String requestParams;

    /**
     * IP地址
     */
    private String ipAddress;

    /**
     * 用户代理
     */
    private String userAgent;

    /**
     * 操作状态：SUCCESS/FAILURE
     */
    private String operationStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}