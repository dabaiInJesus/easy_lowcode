package com.dabai.easy_lowcode.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.mapper.SysResourceMapper;
import com.dabai.easy_lowcode.resource.service.SysResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 资源服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements SysResourceService {
    
    @Override
    public List<SysResource> getUserResourceTree(Long userId) {
        log.info("获取用户资源树, userId: {}", userId);
        
        // TODO: 实际应该从数据库查询用户的角色和资源
        // 这里返回所有启用的菜单资源作为示例
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysResource::getStatus, 1);
        wrapper.orderByAsc(SysResource::getSortOrder);
        
        List<SysResource> allResources = this.list(wrapper);
        
        // 构建树形结构
        return buildResourceTree(allResources, 0L);
    }
    
    @Override
    public List<SysResource> getAllMenus() {
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysResource::getResourceType, "menu");
        wrapper.eq(SysResource::getStatus, 1);
        wrapper.orderByAsc(SysResource::getSortOrder);
        
        return this.list(wrapper);
    }
    
    @Override
    public List<SysResource> getResourcesByRoleId(Long roleId) {
        // TODO: 需要关联查询角色资源表
        log.warn("根据角色ID查询资源功能待实现, roleId: {}", roleId);
        return new ArrayList<>();
    }
    
    /**
     * 构建资源树
     */
    private List<SysResource> buildResourceTree(List<SysResource> allResources, Long parentId) {
        return allResources.stream()
            .filter(resource -> parentId.equals(resource.getParentId()))
            .peek(resource -> {
                // 递归设置子节点
                List<SysResource> children = buildResourceTree(allResources, resource.getId());
                // 注意：SysResource 需要添加 children 字段
            })
            .collect(Collectors.toList());
    }
}
