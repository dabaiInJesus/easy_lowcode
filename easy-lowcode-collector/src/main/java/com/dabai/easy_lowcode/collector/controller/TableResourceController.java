package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.service.DataPreviewService;
import com.dabai.easy_lowcode.collector.service.TableResourceService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 表资源控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/collector/table-resource")
@RequiredArgsConstructor
public class TableResourceController {
    
    private final TableResourceService tableResourceService;
    private final DataPreviewService dataPreviewService;
    
    /**
     * 分页查询表资源列表
     */
    @GetMapping("/page")
    public Result<PageResult<TableResource>> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long datasourceId,
            @RequestParam(required = false) String keyword) {
        
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
            
            Page<TableResource> page = tableResourceService.page(new Page<>(current, size), wrapper);
            
            // TODO: 如果需要填充数据源名称，可以在这里注入 DataSourceConfigService 并批量查询
            
            PageResult<TableResource> result = new PageResult<>(
                page.getTotal(),
                page.getCurrent(),
                page.getSize(),
                page.getRecords()
            );
            
            return Result.success(result);
        } catch (Exception e) {
            log.error("查询表资源列表失败", e);
            return Result.error("查询失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取所有表资源列表（不分页）
     */
    @GetMapping("/list")
    public Result<List<TableResource>> list(@RequestParam(required = false) Long datasourceId) {
        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        if (datasourceId != null) {
            wrapper.eq(TableResource::getDatasourceId, datasourceId);
        }
        wrapper.orderByDesc(TableResource::getCreateTime);
        
        List<TableResource> list = tableResourceService.list(wrapper);
        return Result.success(list);
    }
    
    /**
     * 获取表资源详情
     */
    @GetMapping("/{id}")
    public Result<TableResource> getById(@PathVariable Long id) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        return Result.success(resource);
    }
    
    /**
     * 注册表资源
     */
    @PostMapping
    public Result<Void> register(@RequestBody TableResource tableResource) {
        // 参数验证
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
        
        // 验证API路径格式
        if (!tableResource.getApiPath().startsWith("/")) {
            return Result.error("API路径必须以/开头");
        }
        
        // 检查资源编码唯一性
        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getResourceCode, tableResource.getResourceCode());
        if (tableResourceService.count(wrapper) > 0) {
            return Result.error("资源编码已存在: " + tableResource.getResourceCode());
        }
        
        // 检查API路径唯一性
        wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getApiPath, tableResource.getApiPath());
        if (tableResourceService.count(wrapper) > 0) {
            return Result.error("API路径已存在: " + tableResource.getApiPath());
        }
        
        // 设置默认值
        if (tableResource.getMethods() == null || tableResource.getMethods().trim().isEmpty()) {
            tableResource.setMethods("GET");
        }
        if (tableResource.getStatus() == null) {
            tableResource.setStatus(1);
        }
        
        try {
            boolean success = tableResourceService.registerTableResource(tableResource);
            if (success) {
                log.info("注册表资源成功: {} -> {}", tableResource.getTableName(), tableResource.getResourceCode());
                return Result.success("注册成功");
            } else {
                return Result.error("注册失败");
            }
        } catch (Exception e) {
            log.error("注册表资源失败", e);
            return Result.error("注册失败: " + e.getMessage());
        }
    }
    
    /**
     * 更新表资源
     */
    @PutMapping
    public Result<Void> update(@RequestBody TableResource tableResource) {
        if (tableResource.getId() == null) {
            return Result.error("资源ID不能为空");
        }
        
        // 检查资源是否存在
        TableResource existing = tableResourceService.getById(tableResource.getId());
        if (existing == null) {
            return Result.error("表资源不存在");
        }
        
        // 如果修改了资源编码，检查唯一性
        if (tableResource.getResourceCode() != null && !tableResource.getResourceCode().equals(existing.getResourceCode())) {
            LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(TableResource::getResourceCode, tableResource.getResourceCode());
            wrapper.ne(TableResource::getId, tableResource.getId());
            if (tableResourceService.count(wrapper) > 0) {
                return Result.error("资源编码已存在: " + tableResource.getResourceCode());
            }
        }
        
        // 如果修改了API路径，检查唯一性
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
            log.info("更新表资源成功: {}", tableResource.getId());
            return Result.success("更新成功");
        } catch (Exception e) {
            log.error("更新表资源失败", e);
            return Result.error("更新失败: " + e.getMessage());
        }
    }
    
    /**
     * 删除表资源
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        
        try {
            tableResourceService.removeById(id);
            log.info("删除表资源成功: {}", id);
            return Result.success("删除成功");
        } catch (Exception e) {
            log.error("删除表资源失败", e);
            return Result.error("删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 生成API接口
     */
    @PostMapping("/{id}/generate-api")
    public Result<Void> generateApi(@PathVariable Long id) {
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        
        try {
            boolean success = tableResourceService.generateApi(id);
            if (success) {
                log.info("生成API接口成功: {}, 路径: {}", id, resource.getApiPath());
                return Result.success("API生成成功");
            } else {
                return Result.error("API生成失败");
            }
        } catch (Exception e) {
            log.error("生成API接口失败", e);
            return Result.error("API生成失败: " + e.getMessage());
        }
    }
    
    /**
     * 预览数据（根据资源ID查询实际数据）
     */
    @GetMapping("/{id}/preview")
    public Result<List<Map<String, Object>>> previewData(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        log.info("预览数据，资源ID: {}, 限制条数: {}", id, limit);
        
        TableResource resource = tableResourceService.getById(id);
        if (resource == null) {
            return Result.error("表资源不存在");
        }
        
        try {
            List<Map<String, Object>> data = dataPreviewService.previewTableData(id, limit);
            return Result.success(data);
        } catch (Exception e) {
            log.error("预览数据失败", e);
            return Result.error("预览数据失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量删除表资源
     */
    @DeleteMapping("/batch")
    public Result<Void> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error("请选择要删除的资源");
        }
        
        try {
            tableResourceService.removeByIds(ids);
            log.info("批量删除表资源成功，数量: {}", ids.size());
            return Result.success("批量删除成功");
        } catch (Exception e) {
            log.error("批量删除表资源失败", e);
            return Result.error("批量删除失败: " + e.getMessage());
        }
    }
    
    /**
     * 批量生成API接口
     */
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
                log.error("生成API失败，资源ID: {}", id, e);
                failCount++;
            }
        }
        
        String message = String.format("批量生成完成，成功: %d，失败: %d", successCount, failCount);
        log.info(message);
        return Result.success(message);
    }
}
