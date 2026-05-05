package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.ApiManagement;
import com.dabai.easy_lowcode.collector.mapper.ApiManagementMapper;
import com.dabai.easy_lowcode.collector.service.ApiManagementService;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/collector/api-management")
@RequiredArgsConstructor
public class ApiManagementController {
    
    private final ApiManagementService apiManagementService;
    private final ApiManagementMapper apiManagementMapper;
    
    /**
     * 分页查询API列表
     */
    @GetMapping("/page")
    public Result<Page<ApiManagement>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String apiName,
            @RequestParam(required = false) String apiType,
            @RequestParam(required = false) Integer status) {
        
        Page<ApiManagement> page = new Page<>(current, size);
        LambdaQueryWrapper<ApiManagement> wrapper = new LambdaQueryWrapper<>();
        
        // 模糊搜索API名称
        if (apiName != null && !apiName.trim().isEmpty()) {
            wrapper.like(ApiManagement::getApiName, apiName);
        }
        
        // 精确匹配API类型
        if (apiType != null && !apiType.trim().isEmpty()) {
            wrapper.eq(ApiManagement::getApiType, apiType);
        }
        
        // 精确匹配状态
        if (status != null) {
            wrapper.eq(ApiManagement::getStatus, status);
        }
        
        // 按排序号降序，创建时间降序
        wrapper.orderByDesc(ApiManagement::getSortOrder)
               .orderByDesc(ApiManagement::getCreateTime);
        
        Page<ApiManagement> result = apiManagementService.page(page, wrapper);
        return Result.success(result);
    }
    
    /**
     * 获取API详情
     */
    @GetMapping("/{id}")
    public Result<ApiManagement> getById(@PathVariable Long id) {
        ApiManagement api = apiManagementService.getById(id);
        if (api == null) {
            return Result.error("API不存在");
        }
        return Result.success(api);
    }
    
    /**
     * 注册外部接口API
     */
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
    
    /**
     * 更新API信息
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ApiManagement apiManagement) {
        apiManagement.setId(id);
        boolean success = apiManagementService.updateById(apiManagement);
        if (success) {
            return Result.success("更新成功");
        } else {
            return Result.error("更新失败");
        }
    }
    
    /**
     * 删除API
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        // 物理删除：使用自定义Mapper方法
        int count = apiManagementMapper.physicalDeleteById(id);
        if (count > 0) {
            return Result.success("删除成功");
        } else {
            return Result.error("删除失败");
        }
    }
    
    /**
     * 批量删除API
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的API");
        }
        
        // 物理删除：使用自定义Mapper方法
        int count = apiManagementMapper.physicalDeleteBatchIds(ids);
        return Result.success("批量删除成功，共删除 " + count + " 条记录");
    }
    
    /**
     * 启用/禁用API
     */
    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
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
