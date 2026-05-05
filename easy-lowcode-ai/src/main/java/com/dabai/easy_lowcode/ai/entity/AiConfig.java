package com.dabai.easy_lowcode.ai.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI供应商配置（基于UI管理，不依赖application.yaml硬编码）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_config")
public class AiConfig extends BaseEntity {

    /** 供应商编码 (openai/dashscope/deepseek/ollama/minimax) */
    private String provider;

    /** 显示名称 */
    private String displayName;

    /** API基础URL */
    private String baseUrl;

    /** API Key（加密存储） */
    private String apiKey;

    /** 额外密钥（如文心的Secret Key） */
    private String secretKey;

    /** 模型名称 */
    private String model;

    /** 是否默认 (0-否 1-是) */
    private Integer isDefault = 0;

    /** 状态 (0-禁用 1-启用) */
    private Integer status = 1;

    /** 排序号 */
    private Integer sortOrder = 0;
}
