package com.dabai.easy_lowcode.etl.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 可视化流程定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("etl_flow")
public class EtlFlow extends BaseEntity {

    /** 流程名称 */
    private String flowName;

    /** 流程编码（唯一标识） */
    private String flowCode;

    /** 流程描述 */
    private String description;

    /** 节点配置JSON（Vue Flow nodes） */
    private String nodesJson;

    /** 连线配置JSON（Vue Flow edges） */
    private String edgesJson;

    /** 流程状态（DRAFT/READY/RUNNING/COMPLETED/FAILED），映射到数据库 status 列 */
    @TableField("status")
    private String flowStatus = "DRAFT";

    /** 调度方式（MANUAL/CRON/INTERVAL） */
    private String scheduleType = "MANUAL";

    /** CRON表达式 */
    private String cronExpression;

    /** 间隔秒数 */
    private Integer intervalSeconds;

    /** 最后执行时间 */
    private LocalDateTime lastExecTime;

    /** 最后执行状态 */
    private String lastExecStatus;

    /** 备注 */
    private String remark;

    /** 非数据库字段：节点列表 */
    @TableField(exist = false)
    private List<EtlNodeConfig> nodes;

    /** 非数据库字段：执行记录 */
    @TableField(exist = false)
    private EtlFlowExecution lastExecution;
}
