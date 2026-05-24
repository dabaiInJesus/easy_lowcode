package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.auth.entity.SysApp;
import com.dabai.easy_lowcode.auth.service.SysAppService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理Controller
 */
@Tag(name = "应用管理", description = "应用CRUD及分页查询")
@RestController
@RequestMapping("/api/auth/app")
@RequiredArgsConstructor
public class SysAppController {
    
    private final SysAppService sysAppService;
    
    @Operation(summary = "分页查询应用列表", description = "分页查询应用列表，支持关键词搜索")
    @ApiResponse(responseCode = "200", description = "查询成功")
    @GetMapping("/page")
    public Result<PageResult<SysApp>> getPage(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词（应用名称或编码）") @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<SysApp> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(SysApp::getAppName, keyword)
                    .or()
                    .like(SysApp::getAppCode, keyword);
        }
        wrapper.orderByAsc(SysApp::getSort);
        
        Page<SysApp> page = new Page<>(current, size);
        Page<SysApp> result = sysAppService.page(page, wrapper);
        
        PageResult<SysApp> pageResult = new PageResult<>(
                result.getTotal(),
                result.getCurrent(),
                result.getSize(),
                result.getRecords()
        );
        return Result.success(pageResult);
    }
    
    @Operation(summary = "获取应用列表", description = "获取所有应用列表")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/list")
    public Result<List<SysApp>> getList() {
        List<SysApp> list = sysAppService.getAppList();
        return Result.success(list);
    }
    
    @Operation(summary = "获取应用详情", description = "根据ID获取应用详细信息")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/{id}")
    public Result<SysApp> getById(@Parameter(description = "应用ID") @PathVariable Long id) {
        SysApp app = sysAppService.getById(id);
        return Result.success(app);
    }
    
    @Operation(summary = "创建应用", description = "创建新应用")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @PostMapping
    public Result<Void> create(@RequestBody SysApp app) {
        sysAppService.save(app);
        return Result.success();
    }
    
    @Operation(summary = "更新应用", description = "更新应用信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @PutMapping
    public Result<Void> update(@RequestBody SysApp app) {
        sysAppService.updateById(app);
        return Result.success();
    }
    
    @Operation(summary = "删除应用", description = "根据ID删除应用")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@Parameter(description = "应用ID") @PathVariable Long id) {
        sysAppService.removeById(id);
        return Result.success();
    }
}
