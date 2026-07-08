package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.collector.service.DataPreviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dabai.easy_lowcode.collector.service.TableResourceService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
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
 * 表资源控制器
 */
@Tag(name = "表资源管理", description = "数据库表资源的注册、API生成、数据预览")
@Slf4j
@RestController
@RequestMapping("/api/collector/table-resource")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class TableResourceController {
    
    private final TableResourceService tableResourceService;
    private final DataPreviewService dataPreviewService;
    private final TableResourceMapper tableResourceMapper;
    
    @Operation(summary = "分页查询表资源列表", description = "分页查询表资源，支持按数据源和关键词筛选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<TableResource>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "数据源ID") @RequestParam(required = false) Long datasourceId,
            @Parameter(description = "搜索关键词（表名或资源编码）") @RequestParam(required = false) String keyword) {
        
        try {
            LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
            if (datasourceId != null) {
                wrapper.eq(TableResource::getDatasourceId, datasourceId);
            }
            if (keyword != null && !keyword.isEmpty()) {
                wrapper.like(TableResource::getTableName, keyword)
                       .or()
                       .like(TableResource::getResourceCode, keyword);
            }
            wrapper.orderByDesc(TableResource::getCreateTime);
            
            Page<TableResource> page = ((com.dabai.easy_lowcode.collector.service.impl.TableResourceServiceImpl) tableResourceService).pageWithDatasourceName(new Page<>(current, size), wrapper);
            
            PageResult<TableResource> result = new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
            );
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询表资源列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "获取表资源列表", description = "获取所有表资源列表（不分页）")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<TableResource>> list(
            @Parameter(description = "数据源ID") @RequestParam(required = false) Long datasourceId) {
        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        if (datasourceId != null) {
            wrapper.eq(TableResource::getDatasourceId, datasourceId);
        }
        wrapper.orderByDesc(TableResource::getCreateTime);
        
        List<TableResource> list = ((com.dabai.easy_lowcode.collector.service.impl.TableResourceServiceImpl) tableResourceService).listWithDatasourceName(wrapper);
        return Result.success(list);
    }
    
    @Operation(summary = "获取表资源详情", description = "根据ID获取表资源详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<TableResource> getById(@Parameter(description = "表资源ID") @PathVariable Long id) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        return Result.success(resource);
    }
    
    @Operation(summary = "注册表资源", description = "注册数据库表为API资源")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @PostMapping
    public Result<Void> register(@RequestBody TableResource tableResource) {
        log.info("收到注册请求: {}", tableResource);
        
        if (tableResource.getDatasourceId() == null) {
            return Result.error("数据源ID不能为空");
        }
        if (tableResource.getTableName() == null || tableResource.getTableName().trim().isEmpty()) {
            return Result.error("表名不能为空");
        }
        if (tableResource.getResourceCode() == null || tableResource.getResourceCode().trim().isEmpty()) {
            return Result.error("资源编码不能为空");
        }
        if (tableResource.getApiPath() == null || tableResource.getApiPath().trim().isEmpty()) {
            return Result.error("API路径不能为空");
        }
        
        if (!tableResource.getApiPath().startsWith("/")) {
            return Result.error("API路径必须以/开头");
        }
        
        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getResourceCode, tableResource.getResourceCode());
        if (tableResourceService.count(wrapper) > 0) {
            return Result.error("资源编码已存在: " + tableResource.getResourceCode());
        }
        
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getApiPath, tableResource.getApiPath());
        if (tableResourceService.count(wrapper) > 0) {
            return Result.error("API路径已存在: " + tableResource.getApiPath());
        }
        
        if (tableResource.getMethods() == null || tableResource.getMethods().trim().isEmpty()) {
            tableResource.setMethods("GET");
        }
        if (tableResource.getStatus() == null) {
            tableResource.setStatus(1);
        }
        
        try {
            boolean success = tableResourceService.registerTableResource(tableResource);
            if (success) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (Exception e) {
            return Result.error("注册失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "更新表资源", description = "更新表资源配置信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> update(@RequestBody TableResource tableResource) {
        if (tableResource.getId() == null) {
            return Result.error("资源ID不能为空");
        }
        
        TableResource existing = tableResourceService.getById(tableResource.getId());
        if (existing == null) {
            return Result.error("表资源不存在");
        }
        
        if (tableResource.getResourceCode() != null && !tableResource.getResourceCode().equals(existing.getResourceCode())) {
            LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TableResource::getResourceCode, tableResource.getResourceCode());
            wrapper.ne(TableResource::getId, tableResource.getId());
            if (tableResourceService.count(wrapper) > 0) {
                return Result.error("资源编码已存在: " + tableResource.getResourceCode());
            }
        }
        
        if (tableResource.getApiPath() != null && !tableResource.getApiPath().equals(existing.getApiPath())) {
            LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TableResource::getApiPath, tableResource.getApiPath());
            wrapper.ne(TableResource::getId, tableResource.getId());
            if (tableResourceService.count(wrapper) > 0) {
                return Result.error("API路径已存在: " + tableResource.getApiPath());
            }
        }
        
        try {
            tableResourceService.updateById(tableResource);
            return Result.success("更新成功");
        } catch (Exception e) {
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "删除表资源", description = "删除表资源（需确保无关联API）")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "表资源ID") @PathVariable Long id) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        
        if (tableResourceService.hasRelatedApi(id)) {
            return Result.error(
                String.format("无法删除表资源「%s」\n该表已生成 API 接口（%s），请先在 API 管理中删除相关接口后再试",
                    resource.getTableName(),
                    resource.getApiPath()
                )
            );
        }
        
        boolean removed = tableResourceService.removeById(id);
        if (removed) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
    
    @Operation(summary = "生成API接口", description = "为指定表资源生成API接口")
    @ApiResponse(responseCode = "200", description = "生成成功")
    @PostMapping("/{id}/generate-api")
    public Result<Void> generateApi(@Parameter(description = "表资源ID") @PathVariable Long id) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        
        try {
            boolean success = tableResourceService.generateApi(id);
            if (success) {
                return Result.success("API生成成功");
            } else {
                return Result.error("API生成失败");
            }
        } catch (Exception e) {
            return Result.error("API生成失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "预览数据", description = "预览表资源的实际数据")
    @ApiResponse(responseCode = "200", description = "预览成功")
    @GetMapping("/{id}/preview")
    public Result<List<Map<String, Object>>> previewData(
            @Parameter(description = "表资源ID") @PathVariable Long id,
            @Parameter(description = "限制条数") @RequestParam(defaultValue = "10") int limit) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        
        try {
            List<Map<String, Object>> data = dataPreviewService.previewTableData(id, limit);
            return Result.success(data);
        } catch (Exception e) {
            return Result.error("预览数据失败: " + e.getMessage());
        }
    }
    
    @Operation(summary = "批量删除表资源", description = "批量删除多个表资源")
    @ApiResponse(responseCode = "200", description = "批量删除成功")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的资源");
        }
        
        tableResourceService.removeBatchByIds(ids);
        return Result.success("批量删除成功");
    }
    
    @Operation(summary = "批量生成API接口", description = "批量为多个表资源生成API接口")
    @ApiResponse(responseCode = "200", description = "批量生成完成")
    @PostMapping("/batch-generate-api")
    public Result<Void> batchGenerateApi(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要生成API的资源");
        }
        
        int successCount = 0;
        int failCount = 0;
        
        for (Long id : ids) {
            try {
                boolean success = tableResourceService.generateApi(id);
                if (success) {
                    successCount++;
                } else {
                    failCount++;
                }
            } catch (Exception e) {
                failCount++;
            }
        }
        
        String message = String.format("批量生成完成，成功: %d，失败: %d", successCount, failCount);
        return Result.success(message);
    }
}
