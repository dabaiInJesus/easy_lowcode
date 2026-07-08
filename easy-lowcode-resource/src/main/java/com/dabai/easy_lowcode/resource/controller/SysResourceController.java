package com.dabai.easy_lowcode.resource.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.resource.entity.SysResource;
import com.dabai.easy_lowcode.resource.model.QueryTemplate;
import com.dabai.easy_lowcode.resource.service.DynamicDataService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dabai.easy_lowcode.resource.service.ResourceExecutionService;
import com.dabai.easy_lowcode.resource.service.SysResourceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 资源管理控制器
 */
@Tag(name = "资源管理", description = "资源CRUD、资源树、角色资源分配、动态数据查询")
@Slf4j
@RestController
@RequestMapping("/api/resource")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SysResourceController {
    
    private final SysResourceService resourceService;
    private final DynamicDataService dynamicDataService;
    private final ResourceExecutionService executionService;
    
    @Operation(summary = "分页查询资源列表", description = "分页查询资源列表，支持关键词搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<SysResource>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（资源名称）") @RequestParam(required = false) String keyword) {
        
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysResource::getResourceName, keyword);
        }
        wrapper.orderByAsc(SysResource::getSortOrder);
        
        Page<SysResource> page = resourceService.page(new Page<>(current, size), wrapper);
        
        PageResult<SysResource> result = new PageResult<>(
            page.getRecords(),
            page.getTotal(),
            page.getCurrent(),
            page.getSize()
        );
        
        return Result.success(result);
    }
    
    @Operation(summary = "获取用户菜单树", description = "获取当前用户有权限的资源树")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/user-menu-tree")
    public Result<List<SysResource>> getUserMenuTree() {
        Long userId = getCurrentUserIdFromToken();
        if (userId == null) {
            return Result.error("用户未登录");
        }
        List<SysResource> tree = resourceService.getUserResourceTree(userId);
        return Result.success(tree);
    }
    
    private Long getCurrentUserIdFromToken() {
        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof com.dabai.easy_lowcode.common.security.LoginUser loginUser) {
            return loginUser.getUserId();
        }
        return null;
    }
    
    @Operation(summary = "获取所有菜单", description = "获取所有菜单类型的资源")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/menus")
    public Result<List<SysResource>> getAllMenus() {
        List<SysResource> menus = resourceService.getAllMenus();
        return Result.success(menus);
    }
    
    @Operation(summary = "获取资源树", description = "获取完整的资源树形结构")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/tree")
    public Result<List<SysResource>> getResourceTree() {
        List<SysResource> tree = resourceService.getAllResourceTree();
        return Result.success(tree);
    }
    
    @Operation(summary = "获取资源详情", description = "根据ID获取资源详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<SysResource> getById(@Parameter(description = "资源ID") @PathVariable Long id) {
        SysResource resource = resourceService.getById(id);
        return Result.success(resource);
    }
    
    @Operation(summary = "创建资源", description = "创建新资源")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> create(@RequestBody SysResource resource) {
        resourceService.save(resource);
        return Result.success("创建成功");
    }
    
    @Operation(summary = "更新资源", description = "更新资源信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> update(@RequestBody SysResource resource) {
        resourceService.updateById(resource);
        return Result.success("更新成功");
    }
    
    @Operation(summary = "删除资源", description = "删除资源（需确保无子资源）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@Parameter(description = "资源ID") @PathVariable Long id) {
        LambdaQueryWrapper<SysResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysResource::getParentId, id);
        long count = resourceService.count(wrapper);
        
        if (count > 0) {
            return Result.error("存在子资源，无法删除");
        }
        
        resourceService.removeById(id);
        return Result.success("删除成功");
    }
    
    @Operation(summary = "获取角色资源列表", description = "根据角色ID获取该角色有权限的资源列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/role/{roleId}")
    public Result<List<SysResource>> getByRoleId(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        List<SysResource> resources = resourceService.getResourcesByRoleId(roleId);
        return Result.success(resources);
    }

    @Operation(summary = "获取角色已分配的资源ID", description = "获取角色已分配的资源ID列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/role/{roleId}/ids")
    public Result<List<Long>> getRoleResourceIds(@Parameter(description = "角色ID") @PathVariable Long roleId) {
        return Result.success(resourceService.getRoleResourceIds(roleId));
    }

    @Operation(summary = "为角色分配资源", description = "为指定角色分配资源权限")
    @ApiResponse(responseCode = "200", description = "分配成功")
    @PostMapping("/role/{roleId}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> assignResources(
            @Parameter(description = "角色ID") @PathVariable Long roleId,
            @RequestBody List<Long> resourceIds) {
        resourceService.assignResourcesToRole(roleId, resourceIds);
        return Result.success("分配成功");
    }

    @Operation(summary = "根据资源编码查询数据（GET）", description = "根据资源编码查询数据，支持URL参数")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/data/{resourceCode}")
    public Result<List<Map<String, Object>>> getDataByResourceCode(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
            @Parameter(description = "查询参数") @RequestParam(required = false) Map<String, Object> params) {
        if (params == null) {
            params = new java.util.HashMap<>();
        }
        List<Map<String, Object>> data = dynamicDataService.queryDataByResourceCode(resourceCode, params);
        return Result.success(data);
    }

    @Operation(summary = "根据资源编码查询数据（POST）", description = "根据资源编码查询数据，支持body传参和模板选择")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @PostMapping("/data/{resourceCode}")
    public Result<List<Map<String, Object>>> postDataByResourceCode(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
            @RequestBody(required = false) Map<String, Object> body) {
        if (body == null) {
            body = new java.util.HashMap<>();
        }
        List<Map<String, Object>> data = executionService.executeQuery(resourceCode, body, null);
        return Result.success(data);
    }

    @Operation(summary = "获取资源的查询模板列表", description = "获取指定资源可用的查询模板")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{resourceCode}/templates")
    public Result<List<QueryTemplate>> getTemplates(@Parameter(description = "资源编码") @PathVariable String resourceCode) {
        List<QueryTemplate> templates = executionService.getTemplates(resourceCode);
        return Result.success(templates);
    }
}
