package com.dabai.easy_lowcode.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 菜单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {
    
    /**
     * 父菜单ID
     */
    private Long parentId;
    
    /**
     * 菜单名称
     */
    private String menuName;
    
    /**
     * 菜单类型(1-目录 2-菜单 3-按钮)
     */
    private Integer menuType;
    
    /**
     * 路由地址
     */
    private String path;
    
    /**
     * 组件路径
     */
    private String component;
    
    /**
     * 权限标识
     */
    private String perms;
    
    /**
     * 菜单图标
     */
    private String icon;
    
    /**
     * 排序
     */
    private Integer sort;
    
    /**
     * 状态(0-隐藏 1-显示)
     */
    private Integer visible;
}
