package com.dabai.easy_lowcode.dashboard.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 数据大屏配置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("dashboard")
public class Dashboard extends BaseEntity {

    /**
     * 大屏名称
     */
    private String name;

    /**
     * 大屏编码（唯一标识）
     */
    private String code;

    /**
     * 大屏标题
     */
    private String title;

    /**
     * 大屏描述
     */
    private String description;

    /**
     * 大屏宽度(px)
     */
    private Integer width = 1920;

    /**
     * 大屏高度(px)
     */
    private Integer height = 1080;

    /**
     * 背景色
     */
    private String backgroundColor = "#0a1628";

    /**
     * 背景图URL
     */
    private String backgroundImage;

    /**
     * 布局配置JSON (网格布局信息)
     */
    private String layoutConfig;

    /**
     * 全局样式JSON
     */
    private String styleConfig;

    /**
     * 自动刷新间隔(秒)，0表示不自动刷新
     */
    private Integer refreshInterval = 0;

    /**
     * 状态 (0-草稿 1-发布 2-下线)
     */
    private Integer status = 0;

    /**
     * 缩略图URL
     */
    private String thumbnail;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签（逗号分隔）
     */
    private String tags;

    /**
     * 排序号
     */
    private Integer sortOrder = 0;

    /**
     * 是否内置模板 (0-否 1-是)
     */
    private Integer isBuiltin = 0;

    /** 非数据库字段 */
    @TableField(exist = false)
    private Integer chartCount;
}
