package com.dabai.easy_lowcode.resource.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
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
}
