package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * AI工作流执行记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_workflow_execution")
public class AiWorkflowExecution extends BaseEntity {

    /** 工作流ID */
    private Long workflowId;

    /** 工作流名称 */
    private String workflowName;

    /** 执行状态（RUNNING/SUCCESS/FAILED） */
    private String execStatus = "RUNNING";

    /** 开始时间 */
    private LocalDateTime startTime;

    /** 结束时间 */
    private LocalDateTime endTime;

    /** 输入变量JSON */
    private String inputVariables;

    /** 输出变量JSON */
    private String outputVariables;

    /** 各节点执行详情JSON */
    private String nodeExecutions;

    /** 错误信息 */
    private String errorMessage;
}
