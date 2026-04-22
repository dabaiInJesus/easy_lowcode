package com.dabai.easy_lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 第三方应用实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_app")
public class SysApp extends BaseEntity {
    
    /**
     * 应用名称
     */
    private String appName;
    
    /**
     * 应用编码
     */
    private String appCode;
    
    /**
     * 应用图标
     */
    private String appIcon;
    
    /**
     * 应用地址
     */
    private String appUrl;
    
    /**
     * Client ID
     */
    private String clientId;
    
    /**
     * Client Secret
     */
    private String clientSecret;
    
    /**
     * 回调地址
     */
    private String redirectUri;
    
    /**
     * 状态(0-禁用 1-正常)
     */
    private Integer status;
    
    /**
     * 排序
     */
    private Integer sort;
}
