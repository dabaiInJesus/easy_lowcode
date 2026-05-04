package com.dabai.easy_lowcode.auth.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.auth.entity.SysMenu;

import java.util.List;

/**
 * 菜单服务接口
 */
public interface SysMenuService extends IService<SysMenu> {
    
    /**
     * 获取菜单树形列表
     */
    List<SysMenu> getMenuTree();
}
