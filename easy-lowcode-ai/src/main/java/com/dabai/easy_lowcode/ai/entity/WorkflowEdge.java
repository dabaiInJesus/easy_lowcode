package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流边（连接关系）实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_workflow_edge")
public class WorkflowEdge extends BaseEntity {
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 边ID（前端生成，UUID）
     */
    private String edgeId;
    
    /**
     * 源节点ID
     */
    private String sourceNodeId;
    
    /**
     * 目标节点ID
     */
    private String targetNodeId;
    
    /**
     * 源节点输出端口
     */
    private String sourceHandle;
    
    /**
     * 目标节点输入端口
     */
    private String targetHandle;
    
    /**
     * 边的标签/条件
     */
    private String label;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
}
