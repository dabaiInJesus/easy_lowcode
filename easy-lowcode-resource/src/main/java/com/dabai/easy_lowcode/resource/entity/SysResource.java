package com.dabai.easy_lowcode.resource.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

/**
 * 系统资源实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_resource")
public class SysResource extends BaseEntity {
    
    /**
     * 资源名称
     */
    private String resourceName;
    
    /**
     * 资源编码
     */
    private String resourceCode;
    
    /**
     * 资源类型 (menu/button/api)
     */
    private String resourceType;
    
    /**
     * 父级ID
     */
    private Long parentId;
    
    /**
     * 路径/URL
     */
    private String path;
    
    /**
     * 组件路径
     */
    private String component;
    
    /**
     * 权限标识
     */
    private String permission;
    
    /**
     * 图标
     */
    private String icon;
    
    /**
     * 排序
     */
    private Integer sortOrder;
    
    /**
     * 状态 (0-禁用 1-启用)
     */
    private Integer status = 1;
    
    /**
     * 子资源列表（非数据库字段）
     */
    @TableField(exist = false)
    private List<SysResource> children;
}
