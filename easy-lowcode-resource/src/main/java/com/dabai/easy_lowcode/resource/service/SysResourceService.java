package com.dabai.easy_lowcode.resource.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.resource.entity.SysResource;

import java.util.List;

/**
 * 资源服务接口
 */
public interface SysResourceService extends IService<SysResource> {
    
    /**
     * 获取用户的资源树
     * 
     * @param userId 用户ID
     * @return 资源树
     */
    List<SysResource> getUserResourceTree(Long userId);
    
    /**
     * 获取所有菜单资源
     * 
     * @return 菜单列表
     */
    List<SysResource> getAllMenus();
    
    /**
     * 获取所有资源树
     * 
     * @return 资源树
     */
    List<SysResource> getAllResourceTree();
    
    /**
     * 根据角色ID获取资源
     * 
     * @param roleId 角色ID
     * @return 资源列表
     */
    List<SysResource> getResourcesByRoleId(Long roleId);

    /**
     * 为角色分配资源
     */
    void assignResourcesToRole(Long roleId, List<Long> resourceIds);

    /**
     * 获取角色的资源ID列表
     */
    List<Long> getRoleResourceIds(Long roleId);
}
