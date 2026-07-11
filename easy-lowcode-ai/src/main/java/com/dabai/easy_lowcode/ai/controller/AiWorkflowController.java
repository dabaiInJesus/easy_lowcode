package com.dabai.easy_lowcode.ai.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.ai.engine.WorkflowEngine;
import com.dabai.easy_lowcode.ai.entity.AiWorkflow;
import com.dabai.easy_lowcode.ai.entity.AiWorkflowExecution;
import com.dabai.easy_lowcode.ai.mapper.AiWorkflowExecutionMapper;
import com.dabai.easy_lowcode.ai.service.AiWorkflowService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.result.PageResult;
import com.dabai.easy_lowcode.common.result.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI工作流控制器
 */
@Tag(name = "AI工作流", description = "可视化AI工作流的CRUD和执行")
@RestController
@RequestMapping("/api/ai/workflow")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class AiWorkflowController {

    private final AiWorkflowService workflowService;
    private final WorkflowEngine workflowEngine;
    private final AiWorkflowExecutionMapper executionMapper;

    @Operation(summary = "分页查询工作流列表")
    @GetMapping("/page")
    public Result<PageResult<AiWorkflow>> page(
            @Parameter(description = "当前页码") @RequestParam(defaultValue = "1") Long current,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "10") Long size,
            @Parameter(description = "搜索关键词") @RequestParam(required = false) String keyword,
            @Parameter(description = "状态筛选") @RequestParam(required = false) String status) {
        Page<AiWorkflow> pageResult = workflowService.pageWorkflow(new Page<>(current, size), keyword, status);
        return Result.success(new PageResult<>(pageResult.getRecords(), pageResult.getTotal(),
                pageResult.getCurrent(), pageResult.getSize()));
    }

    @Operation(summary = "获取工作流详情")
    @GetMapping("/{id}")
    public Result<AiWorkflow> getById(@Parameter(description = "工作流ID") @PathVariable Long id) {
        return Result.success(workflowService.getWorkflowDetail(id));
    }

    @Operation(summary = "创建工作流")
    @PostMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> create(@RequestBody AiWorkflow workflow) {
        workflowService.saveWorkflow(workflow);
        return Result.success("创建成功");
    }

    @Operation(summary = "更新工作流")
    @PutMapping
    @PreAuthorize("hasRole('admin')")
    public Result<Void> update(@RequestBody AiWorkflow workflow) {
        workflowService.updateWorkflow(workflow);
        return Result.success("更新成功");
    }

    @Operation(summary = "删除工作流")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@Parameter(description = "工作流ID") @PathVariable Long id) {
        workflowService.deleteWorkflow(id);
        return Result.success("删除成功");
    }

    @Operation(summary = "发布工作流")
    @PostMapping("/{id}/publish")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> publish(@Parameter(description = "工作流ID") @PathVariable Long id) {
        workflowService.publishWorkflow(id);
        return Result.success("发布成功");
    }

    @Operation(summary = "执行工作流（SSE流式）")
    @PostMapping(value = "/{id}/execute", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter execute(
            @Parameter(description = "工作流ID") @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> inputVariables) {

        SseEmitter emitter = new SseEmitter(300_000L); // 5分钟超时

        AiWorkflow workflow = workflowService.getWorkflowDetail(id);
        if (workflow.getNodesJson() == null || workflow.getNodesJson().isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("{\"error\":\"工作流未配置节点\"}"));
            } catch (Exception ignored) {}
            emitter.complete();
            return emitter;
        }

        // 创建执行记录
        AiWorkflowExecution execution = new AiWorkflowExecution();
        execution.setWorkflowId(id);
        execution.setWorkflowName(workflow.getWorkflowName());
        execution.setExecStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        executionMapper.insert(execution);

        // 异步执行
        Map<String, Object> vars = inputVariables != null ? inputVariables : Map.of();
        CompletableFuture.runAsync(() -> {
            try {
                workflowEngine.executeWorkflow(workflow, execution, vars, emitter);
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data("{\"error\":\"" + e.getMessage() + "\"}"));
                } catch (Exception ignored) {}
                emitter.complete();
            }
        });

        return emitter;
    }

    @Operation(summary = "获取执行历史")
    @GetMapping("/{id}/history")
    public Result<List<AiWorkflowExecution>> history(
            @Parameter(description = "流程ID") @PathVariable Long id,
            @Parameter(description = "限制条数") @RequestParam(defaultValue = "20") int limit) {
        return Result.success(workflowService.getExecutionHistory(id, limit));
    }

    @Operation(summary = "获取执行详情")
    @GetMapping("/execution/{executionId}")
    public Result<AiWorkflowExecution> executionDetail(
            @Parameter(description = "执行记录ID") @PathVariable Long executionId) {
        return Result.success(workflowService.getExecutionDetail(executionId));
    }
}