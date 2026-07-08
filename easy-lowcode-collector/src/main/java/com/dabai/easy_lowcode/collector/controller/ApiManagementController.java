package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.ApiManagement;
import com.dabai.easy_lowcode.collector.mapper.ApiManagementMapper;
import com.dabai.easy_lowcode.collector.service.ApiManagementService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API管理控制器
 */
@Tag(name = "API管理", description = "外部接口API的注册、查询、启停管理")
@Slf4j
@RestController
@RequestMapping("/api/collector/api-management")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ApiManagementController {
    
    private final ApiManagementService apiManagementService;
    private final ApiManagementMapper apiManagementMapper;
    
    @Operation(summary = "分页查询API列表", description = "分页查询API列表，支持按名称、类型、状态筛选")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<Page<ApiManagement>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") int current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "API名称（模糊搜索）") @RequestParam(required = false) String apiName,
            @Parameter(description = "API类型") @RequestParam(required = false) String apiType,
            @Parameter(description = "状态（1启用/0禁用）") @RequestParam(required = false) Integer status) {
        
        Page<ApiManagement> page = new Page<>(current, size);
        LambdaQueryWrapper<ApiManagement> wrapper = new LambdaQueryWrapper<>();
        
        if (apiName != null && !apiName.trim().isEmpty()) {
            wrapper.like(ApiManagement::getApiName, apiName);
        }
        
        if (apiType != null && !apiType.trim().isEmpty()) {
            wrapper.eq(ApiManagement::getApiType, apiType);
        }
        
        if (status != null) {
            wrapper.eq(ApiManagement::getStatus, status);
        }
        
        wrapper.orderByDesc(ApiManagement::getSortOrder)
               .orderByDesc(ApiManagement::getCreateTime);
        
        Page<ApiManagement> result = apiManagementService.page(page, wrapper);
        return Result.success(result);
    }
    
    @Operation(summary = "获取API详情", description = "根据ID获取API详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<ApiManagement> getById(@Parameter(description = "API ID") @PathVariable Long id) {
        ApiManagement api = apiManagementService.getById(id);
        if (api == null) {
            return Result.error("API不存在");
        }
        return Result.success(api);
    }
    
    @Operation(summary = "注册外部接口API", description = "注册一个新的外部接口API")
    @ApiResponse(responseCode = "200", description = "注册成功")
    @PostMapping("/register-external")
    public Result<Void> registerExternalApi(@RequestBody ApiManagement apiManagement) {
        try {
            boolean success = apiManagementService.registerExternalApi(apiManagement);
            if (success) {
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (Exception e) {
            log.error("注册外部接口API失败", e);
            return Result.error(e.getMessage());
        }
    }
    
    @Operation(summary = "更新API信息", description = "更新API的配置信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping("/{id}")
    public Result<Void> update(
            @Parameter(description = "API ID") @PathVariable Long id,
            @RequestBody ApiManagement apiManagement) {
        apiManagement.setId(id);
        boolean success = apiManagementService.updateById(apiManagement);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }
    
    @Operation(summary = "删除API", description = "根据ID删除API")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "API ID") @PathVariable Long id) {
        boolean removed = apiManagementService.removeById(id);
        return removed ? Result.success("删除成功") : Result.error("删除失败");
    }

    @Operation(summary = "批量删除API", description = "批量删除多个API")
    @ApiResponse(responseCode = "200", description = "批量删除成功")
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的API");
        }
        apiManagementService.removeBatchByIds(ids);
        return Result.success("批量删除成功");
    }
    
    @Operation(summary = "启用/禁用API", description = "切换API的启用或禁用状态")
    @ApiResponse(responseCode = "200", description = "操作成功")
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(
            @Parameter(description = "API ID") @PathVariable Long id,
            @Parameter(description = "状态（1启用/0禁用）") @RequestParam Integer status) {
        ApiManagement api = apiManagementService.getById(id);
        if (api == null) {
            return Result.error("API不存在");
        }
        
        api.setStatus(status);
        boolean success = apiManagementService.updateById(api);
        
        if (success) {
            String statusText = status == 1 ? "启用" : "禁用";
            return Result.success(statusText + "成功");
        } else {
            return Result.error("操作失败");
        }
    }
}
