package com.dabai.easy_lowcode.auth.controller;

import com.dabai.easy_lowcode.auth.service.SysAuditLogService;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 审计日志控制器
 */
@Tag(name = "审计日志", description = "系统操作审计日志查询")
@RestController
@RequestMapping("/api/auth/audit")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class SysAuditLogController {

    private final SysAuditLogService auditLogService;

    @Operation(summary = "分页查询审计日志", description = "支持按模块、操作、用户名搜索")
    @GetMapping("/page")
    public Result<PageResult<Map<String, Object>>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") Long size,
            @Parameter(description = "模块") @RequestParam(required = false) String module,
            @Parameter(description = "操作") @RequestParam(required = false) String action,
            @Parameter(description = "用户名") @RequestParam(required = false) String username) {
        PageResult<Map<String, Object>> result = auditLogService.queryPage(current, size, module, action, username);
        return Result.success(result);
    }

    @Operation(summary = "查询最近操作", description = "获取最近的N条审计日志")
    @GetMapping("/recent")
    public Result<?> recent(@Parameter(description = "条数") @RequestParam(defaultValue = "20") Integer limit) {
        var logs = auditLogService.queryRecent(limit);
        return Result.success(logs);
    }

    @Operation(summary = "清理历史日志", description = "删除指定天数之前的审计日志")
    @DeleteMapping("/clean")
    @PreAuthorize("hasRole('admin')")
    public Result<?> clean(@Parameter(description = "保留天数") @RequestParam(defaultValue = "90") Integer days) {
        int count = auditLogService.deleteBeforeDays(days);
        return Result.success("已清理 " + count + " 条历史日志");
    }
}