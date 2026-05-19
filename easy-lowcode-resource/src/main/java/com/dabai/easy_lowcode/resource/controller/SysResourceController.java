package com.dabai.easy_lowcode.resource.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.model.QueryTemplate;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
import com.dabai.easy_lowcode.resource.service.ResourceExecutionService;
import com.dabai.easy_lowcode.resource.service.SysResourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
public class SysResourceController {
    
    private final SysResourceService resourceService;
    private final DynamicDataService dynamicDataService;
    private final ResourceExecutionService executionService;
    
    /**
     * 分页查询资源列表
     */
    @GetMapping("/page")
    public Result<PageResult<SysResource>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysResource::getResourceName, keyword);
        }
        wrapper.orderByAsc(SysResource::getSortOrder);
        
        Page<SysResource> page = resourceService.page(new Page<>(current, size), wrapper);
        
        PageResult<SysResource> result = new PageResult<>(
            page.getTotal(),
            page.getCurrent(),
            page.getSize(),
            page.getRecords()
        );
        
        return Result.success(result);
    }
    
    /**
     * 获取用户的资源树（用于前端菜单）
     */
    @GetMapping("/user-menu-tree")
    public Result<List<SysResource>> getUserMenuTree() {
        Long userId = StpUtil.getLoginIdAsLong();
        List<SysResource> tree = resourceService.getUserResourceTree(userId);
        return Result.success(tree);
    }
    
    /**
     * 获取所有菜单
     */
    @GetMapping("/menus")
    public Result<List<SysResource>> getAllMenus() {
        List<SysResource> menus = resourceService.getAllMenus();
        return Result.success(menus);
    }
    
    /**
     * 获取资源树（用于前端菜单展示）
     */
    @GetMapping("/tree")
    public Result<List<SysResource>> getResourceTree() {
        List<SysResource> tree = resourceService.getAllResourceTree();
        return Result.success(tree);
    }
    
    /**
     * 获取资源详情
     */
    @GetMapping("/{id}")
    public Result<SysResource> getById(@PathVariable Long id) {
        SysResource resource = resourceService.getById(id);
        return Result.success(resource);
    }
    
    /**
     * 创建资源
     */
    @PostMapping
    public Result<Void> create(@RequestBody SysResource resource) {
        resourceService.save(resource);
        return Result.success("创建成功");
    }
    
    /**
     * 更新资源
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysResource resource) {
        resourceService.updateById(resource);
        return Result.success("更新成功");
    }
    
    /**
     * 删除资源
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 检查是否有子资源
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysResource::getParentId, id);
        long count = resourceService.count(wrapper);
        
        if (count > 0) {
            return Result.error("存在子资源，无法删除");
        }
        
        resourceService.removeById(id);
        return Result.success("删除成功");
    }
    
    /**
     * 根据角色ID获取资源列表
     */
    @GetMapping("/role/{roleId}")
    public Result<List<SysResource>> getByRoleId(@PathVariable Long roleId) {
        List<SysResource> resources = resourceService.getResourcesByRoleId(roleId);
        return Result.success(resources);
    }

    /**
     * 获取角色已分配的资源ID列表
     */
    @GetMapping("/role/{roleId}/ids")
    public Result<List<Long>> getRoleResourceIds(@PathVariable Long roleId) {
        return Result.success(resourceService.getRoleResourceIds(roleId));
    }

    /**
     * 为角色分配资源
     */
    @PostMapping("/role/{roleId}")
    public Result<Void> assignResources(@PathVariable Long roleId, @RequestBody List<Long> resourceIds) {
        resourceService.assignResourcesToRole(roleId, resourceIds);
        return Result.success("分配成功");
    }

    /**
     * 根据资源编码查询数据（动态API）
     */
    @GetMapping("/data/{resourceCode}")
    public Result<List<Map<String, Object>>> getDataByResourceCode(
            @PathVariable String resourceCode,
            @RequestParam(required = false) Map<String, Object> params) {
        log.info("查询资源数据: {}, 参数: {}", resourceCode, params);
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        List<Map<String, Object>> data = dynamicDataService.queryDataByResourceCode(resourceCode, params);
        return Result.success(data);
    }

    /**
     * 根据资源编码查询数据（POST方式，支持body传参 + 模板选择）
     */
    @PostMapping("/data/{resourceCode}")
    public Result<List<Map<String, Object>>> postDataByResourceCode(
            @PathVariable String resourceCode,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            body = new java.util.HashMap<>();
        }
        List<Map<String, Object>> data = executionService.executeQuery(resourceCode, body, null);
        return Result.success(data);
    }

    /**
     * 获取资源的查询模板列表
     */
    @GetMapping("/{resourceCode}/templates")
    public Result<List<QueryTemplate>> getTemplates(@PathVariable String resourceCode) {
        List<QueryTemplate> templates = executionService.getTemplates(resourceCode);
        return Result.success(templates);
    }
}
