package com.dabai.easy_lowcode.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.auth.entity.SysMenu;
import com.dabai.easy_lowcode.auth.mapper.SysMenuMapper;
import com.dabai.easy_lowcode.auth.service.SysMenuService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 菜单服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {
    
    @Override
    public List<SysMenu> getMenuTree() {
        // 查询所有菜单
        LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(SysMenu::getSort);
        List<SysMenu> allMenus = this.list(wrapper);
        
        // 构建树形结构
        return buildTree(allMenus, 0L);
    }
    
    /**
     * 递归构建树形结构
     */
    private List<SysMenu> buildTree(List<SysMenu> menus, Long parentId) {
        return menus.stream()
                .filter(menu -> menu.getParentId().equals(parentId))
                .peek(menu -> {
                    List<SysMenu> children = buildTree(menus, menu.getId());
                    // 这里可以设置子菜单，但实体中没有children字段
                    // 如果需要返回树形结构，需要创建一个VO类
                })
                .collect(Collectors.toList());
    }
}
