package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 提示词模板实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_prompt_template")
public class PromptTemplate extends BaseEntity {
    
    /**
     * 模板名称
     */
    private String templateName;
    
    /**
     * 模板编码（唯一标识）
     */
    private String templateCode;
    
    /**
     * 模板描述
     */
    private String description;
    
    /**
     * 模板内容（支持变量 {{variable}}）
     */
    private String content;
    
    /**
     * 模板类型 (system/user/assistant)
     */
    private String templateType = "system";
    
    /**
     * 分类标签
     */
    private String category;
    
    /**
     * 版本
     */
    private String version = "1.0.0";
    
    /**
     * 是否默认模板 (0-否 1-是)
     */
    private Integer isDefault = 0;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
}
