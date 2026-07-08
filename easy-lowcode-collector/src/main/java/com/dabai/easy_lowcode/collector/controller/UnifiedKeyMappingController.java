package com.dabai.easy_lowcode.collector.controller;

import com.dabai.easy_lowcode.collector.entity.UnifiedKeyMapping;
import com.dabai.easy_lowcode.collector.service.UnifiedKeyMappingService;
import com.dabai.easy_lowcode.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "统一Key映射管理", description = "统一Key的CRUD、自动映射建议")
@Slf4j
@RestController
@RequestMapping("/api/collector/unified-key-mapping")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class UnifiedKeyMappingController {

    private final UnifiedKeyMappingService mappingService;

    @Operation(summary = "获取所有统一Key列表")
    @GetMapping("/keys")
    public Result<List<UnifiedKeyMapping>> getDistinctKeys() {
        return Result.success(mappingService.getDistinctKeys());
    }

    @Operation(summary = "获取指定统一Key的映射列表")
    @GetMapping("/keys/{unifiedKey}")
    public Result<List<UnifiedKeyMapping>> getMappingsByKey(@PathVariable String unifiedKey) {
        return Result.success(mappingService.getMappingsByKey(unifiedKey));
    }

    @Operation(summary = "获取指定资源的映射列表")
    @GetMapping("/resource/{resourceCode}")
    public Result<List<UnifiedKeyMapping>> getMappingsByResource(@PathVariable String resourceCode) {
        return Result.success(mappingService.getMappingsByResourceCode(resourceCode));
    }

    @Operation(summary = "分页查询所有映射")
    @GetMapping("/page")
    public Result<com.baomidou.mybatisplus.extension.plugins.pagination.Page<UnifiedKeyMapping>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String unifiedKey) {
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<UnifiedKeyMapping> page =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(current, size);
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UnifiedKeyMapping> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();
        wrapper.eq(UnifiedKeyMapping::getDeleted, 0);
        if (unifiedKey != null && !unifiedKey.isEmpty()) {
            wrapper.eq(UnifiedKeyMapping::getUnifiedKey, unifiedKey);
        }
        wrapper.orderByAsc(UnifiedKeyMapping::getUnifiedKey, UnifiedKeyMapping::getSortOrder);
        return Result.success(mappingService.page(page, wrapper));
    }

    @Operation(summary = "创建映射")
    @PostMapping
    public Result<Void> create(@RequestBody UnifiedKeyMapping mapping) {
        mappingService.save(mapping);
        return Result.success("创建成功");
    }

    @Operation(summary = "批量创建映射")
    @PostMapping("/batch")
    public Result<Void> batchCreate(@RequestBody List<UnifiedKeyMapping> mappings) {
        mappingService.batchSave(mappings);
        return Result.success("批量创建成功");
    }

    @Operation(summary = "更新映射")
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody UnifiedKeyMapping mapping) {
        mapping.setId(id);
        mappingService.updateById(mapping);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除映射")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mappingService.removeById(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "自动检测映射建议", description = "根据统一Key同义词，自动扫描所有数据源匹配字段")
    @GetMapping("/suggest")
    public Result<List<Map<String, Object>>> suggestMappings(
            @RequestParam String unifiedKey,
            @RequestParam(required = false) String displayName) {
        return Result.success(mappingService.suggestMappings(unifiedKey, displayName));
    }
}
