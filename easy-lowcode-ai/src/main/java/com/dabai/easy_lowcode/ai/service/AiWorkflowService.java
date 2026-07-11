package com.dabai.easy_lowcode.ai.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.ai.entity.AiWorkflow;
import com.dabai.easy_lowcode.ai.entity.AiWorkflowExecution;

import java.util.List;
import java.util.Map;

/**
 * AI工作流服务接口
 */
public interface AiWorkflowService extends IService<AiWorkflow> {

    /**
     * 分页查询工作流列表
     */
    Page<AiWorkflow> pageWorkflow(Page<AiWorkflow> page, String keyword, String status);

    /**
     * 获取工作流详情
     */
    AiWorkflow getWorkflowDetail(Long workflowId);

    /**
     * 保存工作流（含节点和连线）
     */
    boolean saveWorkflow(AiWorkflow workflow);

    /**
     * 更新工作流
     */
    boolean updateWorkflow(AiWorkflow workflow);

    /**
     * 删除工作流
     */
    boolean deleteWorkflow(Long workflowId);

    /**
     * 发布工作流
     */
    boolean publishWorkflow(Long workflowId);

    /**
     * 执行工作流（SSE流式）
     */
    Long executeWorkflow(Long workflowId, Map<String, Object> inputVariables);

    /**
     * 获取执行历史
     */
    List<AiWorkflowExecution> getExecutionHistory(Long workflowId, int limit);

    /**
     * 获取执行详情
     */
    AiWorkflowExecution getExecutionDetail(Long executionId);
}
