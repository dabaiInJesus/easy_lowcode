package com.dabai.easy_lowcode.auth.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.auth.entity.SysApp;
import com.dabai.easy_lowcode.auth.service.SysAppService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 应用管理Controller
 */
@RestController
@RequestMapping("/api/auth/app")
@RequiredArgsConstructor
public class SysAppController {
    
    private final SysAppService sysAppService;
    
    /**
     * 分页查询应用列表
     */
    @GetMapping("/page")
    public Result<PageResult<SysApp>> getPage(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword) {
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
    
    /**
     * 获取应用列表
     */
    @GetMapping("/list")
    public Result<List<SysApp>> getList() {
        List<SysApp> list = sysAppService.getAppList();
        return Result.success(list);
    }
    
    /**
     * 根据ID获取应用
     */
    @GetMapping("/{id}")
    public Result<SysApp> getById(@PathVariable Long id) {
        SysApp app = sysAppService.getById(id);
        return Result.success(app);
    }
    
    /**
     * 创建应用
     */
    @PostMapping
    public Result<Void> create(@RequestBody SysApp app) {
        sysAppService.save(app);
        return Result.success();
    }
    
    /**
     * 更新应用
     */
    @PutMapping
    public Result<Void> update(@RequestBody SysApp app) {
        sysAppService.updateById(app);
        return Result.success();
    }
    
    /**
     * 删除应用
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysAppService.removeById(id);
        return Result.success();
    }
}
