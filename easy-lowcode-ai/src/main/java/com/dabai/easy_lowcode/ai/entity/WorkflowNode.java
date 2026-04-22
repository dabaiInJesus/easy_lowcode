package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 工作流节点实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_workflow_node")
public class WorkflowNode extends BaseEntity {
    
    /**
     * 智能体ID
     */
    private Long agentId;
    
    /**
     * 节点ID（前端生成，UUID）
     */
    private String nodeId;
    
    /**
     * 节点类型 (start/llm/knowledge/tool/code/end)
     */
    private String nodeType;
    
    /**
     * 节点名称
     */
    private String nodeName;
    
    /**
     * 节点描述
     */
    private String description;
    
    /**
     * 节点配置 JSON
     */
    private String nodeConfig;
    
    /**
     * 位置 X
     */
    private Integer positionX;
    
    /**
     * 位置 Y
     */
    private Integer positionY;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
}
