package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 智能体实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_agent")
public class AiAgent extends BaseEntity {
    
    /**
     * 智能体名称
     */
    private String agentName;
    
    /**
     * 智能体编码（唯一标识）
     */
    private String agentCode;
    
    /**
     * 智能体描述
     */
    private String description;
    
    /**
     * 智能体头像/图标
     */
    private String avatar;
    
    /**
     * AI 提供商 (openai/dashscope/wenxin等)
     */
    private String provider;
    
    /**
     * 模型名称
     */
    private String model;
    
    /**
     * 系统提示词模板ID
     */
    private Long promptTemplateId;
    
    /**
     * 温度参数 (0-2)
     */
    private Double temperature = 0.7;
    
    /**
     * 最大 token 数
     */
    private Integer maxTokens = 2000;
    
    /**
     * 是否启用工作流 (0-否 1-是)
     */
    private Integer enableWorkflow = 0;
    
    /**
     * 工作流配置 JSON
     */
    @TableField(value = "workflow_config")
    private String workflowConfig;
    
    /**
     * 变量配置 JSON（如：{{name}}）
     */
    @TableField(value = "variables_config")
    private String variablesConfig;
    
    /**
     * 开场白
     */
    private String openingStatement;
    
    /**
     * 建议问题列表 JSON
     */
    @TableField(value = "suggested_questions")
    private String suggestedQuestions;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
    
    /**
     * 发布状态 (0-草稿 1-已发布)
     */
    private Integer publishStatus = 0;
    
    /**
     * 使用次数
     */
    private Integer usageCount = 0;
}
