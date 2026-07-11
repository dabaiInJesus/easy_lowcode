package com.dabai.easy_lowcode.etl.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 流程执行记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("etl_flow_execution")
public class EtlFlowExecution extends BaseEntity {

    /** 流程ID */
    private Long flowId;

    /** 流程名称（冗余） */
    private String flowName;

    /** 执行状态（RUNNING/SUCCESS/FAILED/STOPPED） */
    private String execStatus = "RUNNING";

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 读取记录数 */
    private Long readCount = 0L;

    /** 写入记录数 */
    private Long writeCount = 0L;

    /** 跳过记录数 */
    private Long skipCount = 0L;

    /** 错误记录数 */
    private Long errorCount = 0L;

    /** 错误信息 */
    private String errorMessage;

    /** 各节点执行详情JSON */
    private String execDetail;
}
