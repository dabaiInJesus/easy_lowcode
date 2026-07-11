package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * AI工作流定义
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_workflow")
public class AiWorkflow extends BaseEntity {

    /** 工作流名称 */
    private String workflowName;

    /** 工作流编码 */
    private String workflowCode;

    /** 工作流描述 */
    private String description;

    /** 节点配置JSON */
    private String nodesJson;

    /** 连线配置JSON */
    private String edgesJson;

    /** 全局变量定义JSON */
    private String variablesJson;

    /** 状态（DRAFT/PUBLISHED） */
    private String status = "DRAFT";

    /** 备注 */
    private String remark;

    /** 非数据库字段 */
    @TableField(exist = false)
    private List<?> nodes;

    @TableField(exist = false)
    private List<?> edges;
}
