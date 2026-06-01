package com.dabai.easy_lowcode.common.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 审计日志实体
 */
@Data
public class SysAuditLog implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户ID
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 模块
     */
    private String module;

    /**
     * 操作类型
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
     * User-Agent
     */
    private String userAgent;

    /**
     * 执行时间（毫秒）
     */
    private Long executionTime;

    /**
     * 操作状态 SUCCESS/FAILURE
     */
    private String operationStatus;

    /**
     * 错误信息
     */
    private String errorMessage;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 删除标志
     */
    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;
}
