package com.dabai.easy_lowcode.resource.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.entity.SysRoleResource;
import com.dabai.easy_lowcode.resource.mapper.SysResourceMapper;
import com.dabai.easy_lowcode.resource.mapper.SysRoleResourceMapper;
import com.dabai.easy_lowcode.resource.service.SysResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysResourceServiceImpl extends ServiceImpl<SysResourceMapper, SysResource> implements SysResourceService {

    private final SysRoleResourceMapper roleResourceMapper;
    private final JdbcTemplate jdbcTemplate;

    @Override
    public List<SysResource> getUserResourceTree(Long userId) {
        String sql = "SELECT sr.* FROM sys_resource sr " +
                    "INNER JOIN sys_role_resource rsrc ON sr.id = rsrc.resource_id " +
                    "INNER JOIN sys_user_role ur ON rsrc.role_id = ur.role_id " +
                    "WHERE ur.user_id = ? AND sr.status = 1 AND sr.deleted = 0 " +
                    "ORDER BY sr.sort_order";
        List<SysResource> resources = jdbcTemplate.query(sql, (rs, rowNum) -> {
            SysResource r = new SysResource();
            r.setId(rs.getLong("id"));
            r.setResourceName(rs.getString("resource_name"));
            r.setResourceCode(rs.getString("resource_code"));
            r.setResourceType(rs.getString("resource_type"));
            r.setParentId(rs.getLong("parent_id"));
            r.setPath(rs.getString("path"));
            r.setComponent(rs.getString("component"));
            r.setPermission(rs.getString("permission"));
            r.setIcon(rs.getString("icon"));
            r.setSortOrder(rs.getInt("sort_order"));
            r.setStatus(rs.getInt("status"));
            return r;
        }, userId);
        return buildResourceTree(resources, 0L);
    }

    @Override
    public List<SysResource> getAllMenus() {
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysResource::getResourceType, "menu");
        wrapper.eq(SysResource::getStatus, 1);
        wrapper.orderByAsc(SysResource::getSortOrder);
        List<SysResource> menus = this.list(wrapper);
        return buildResourceTree(menus, 0L);
    }

    @Override
    public List<SysResource> getAllResourceTree() {
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysResource::getStatus, 1);
        wrapper.orderByAsc(SysResource::getSortOrder);
        List<SysResource> allResources = this.list(wrapper);
        return buildResourceTree(allResources, 0L);
    }

    @Override
    public List<SysResource> getResourcesByRoleId(Long roleId) {
        List<Long> resourceIds = roleResourceMapper.selectList(
                new LambdaQueryWrapper<SysRoleResource>()
                        .eq(SysRoleResource::getRoleId, roleId))
                .stream()
                .map(SysRoleResource::getResourceId)
                .collect(Collectors.toList());
        if (resourceIds.isEmpty()) return Collections.emptyList();
        return this.listByIds(resourceIds);
    }

    @Override
    @Transactional
    public void assignResourcesToRole(Long roleId, List<Long> resourceIds) {
        jdbcTemplate.update("DELETE FROM sys_role_resource WHERE role_id = ?", roleId);
        if (resourceIds != null && !resourceIds.isEmpty()) {
            String sql = "INSERT INTO sys_role_resource (role_id, resource_id) VALUES (?, ?)";
            for (Long resourceId : resourceIds) {
                jdbcTemplate.update(sql, roleId, resourceId);
            }
        }
        log.info("为角色 {} 分配资源: {}", roleId, resourceIds);
    }

    @Override
    public List<Long> getRoleResourceIds(Long roleId) {
        return roleResourceMapper.selectList(
                new LambdaQueryWrapper<SysRoleResource>()
                        .eq(SysRoleResource::getRoleId, roleId))
                .stream()
                .map(SysRoleResource::getResourceId)
                .collect(Collectors.toList());
    }

    private List<SysResource> buildResourceTree(List<SysResource> allResources, Long parentId) {
        return allResources.stream()
            .filter(resource -> parentId.equals(resource.getParentId()))
            .peek(resource -> {
                List<SysResource> children = buildResourceTree(allResources, resource.getId());
                if (!children.isEmpty()) {
                    resource.setChildren(children);
                }
            })
            .collect(Collectors.toList());
    }
}
