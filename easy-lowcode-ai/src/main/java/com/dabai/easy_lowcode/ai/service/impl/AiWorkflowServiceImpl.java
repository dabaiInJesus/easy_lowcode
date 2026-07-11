package com.dabai.easy_lowcode.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.ai.entity.AiWorkflow;
import com.dabai.easy_lowcode.ai.entity.AiWorkflowExecution;
import com.dabai.easy_lowcode.ai.mapper.AiWorkflowExecutionMapper;
import com.dabai.easy_lowcode.ai.mapper.AiWorkflowMapper;
import com.dabai.easy_lowcode.ai.service.AiWorkflowService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * AI工作流服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiWorkflowServiceImpl extends ServiceImpl<AiWorkflowMapper, AiWorkflow> implements AiWorkflowService {

    private final AiWorkflowMapper workflowMapper;
    private final AiWorkflowExecutionMapper executionMapper;

    @Override
    public Page<AiWorkflow> pageWorkflow(Page<AiWorkflow> page, String keyword, String status) {
        LambdaQueryWrapper<AiWorkflow> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(AiWorkflow::getWorkflowName, keyword)
                    .or().like(AiWorkflow::getWorkflowCode, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(AiWorkflow::getStatus, status);
        }
        wrapper.orderByDesc(AiWorkflow::getUpdateTime);
        return workflowMapper.selectPage(page, wrapper);
    }

    @Override
    public AiWorkflow getWorkflowDetail(Long workflowId) {
        AiWorkflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }
        return workflow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveWorkflow(AiWorkflow workflow) {
        LambdaQueryWrapper<AiWorkflow> check = new LambdaQueryWrapper<>();
        check.eq(AiWorkflow::getWorkflowCode, workflow.getWorkflowCode());
        if (this.count(check) > 0) {
            throw new BusinessException("工作流编码已存在: " + workflow.getWorkflowCode());
        }
        workflow.setStatus("DRAFT");
        return this.save(workflow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateWorkflow(AiWorkflow workflow) {
        AiWorkflow existing = workflowMapper.selectById(workflow.getId());
        if (existing == null) {
            throw new BusinessException("工作流不存在");
        }
        if (workflow.getWorkflowCode() != null && !workflow.getWorkflowCode().equals(existing.getWorkflowCode())) {
            LambdaQueryWrapper<AiWorkflow> check = new LambdaQueryWrapper<>();
            check.eq(AiWorkflow::getWorkflowCode, workflow.getWorkflowCode());
            check.ne(AiWorkflow::getId, workflow.getId());
            if (this.count(check) > 0) {
                throw new BusinessException("工作流编码已存在: " + workflow.getWorkflowCode());
            }
        }
        return this.updateById(workflow);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteWorkflow(Long workflowId) {
        AiWorkflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }
        this.removeById(workflowId);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean publishWorkflow(Long workflowId) {
        AiWorkflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }
        if (workflow.getNodesJson() == null || workflow.getNodesJson().isBlank()) {
            throw new BusinessException("工作流未配置节点");
        }
        workflow.setStatus("PUBLISHED");
        return this.updateById(workflow);
    }

    @Override
    public Long executeWorkflow(Long workflowId, Map<String, Object> inputVariables) {
        AiWorkflow workflow = workflowMapper.selectById(workflowId);
        if (workflow == null) {
            throw new BusinessException("工作流不存在");
        }
        if (workflow.getNodesJson() == null || workflow.getNodesJson().isBlank()) {
            throw new BusinessException("工作流未配置节点");
        }

        // 创建执行记录
        AiWorkflowExecution execution = new AiWorkflowExecution();
        execution.setWorkflowId(workflowId);
        execution.setWorkflowName(workflow.getWorkflowName());
        execution.setExecStatus("RUNNING");
        execution.setStartTime(LocalDateTime.now());
        executionMapper.insert(execution);

        // TODO: 调用工作流执行引擎异步执行
        log.info("准备执行工作流: workflowId={}, executionId={}", workflowId, execution.getId());

        return execution.getId();
    }

    @Override
    public List<AiWorkflowExecution> getExecutionHistory(Long workflowId, int limit) {
        return executionMapper.selectList(
                new LambdaQueryWrapper<AiWorkflowExecution>()
                        .eq(AiWorkflowExecution::getWorkflowId, workflowId)
                        .orderByDesc(AiWorkflowExecution::getCreateTime)
                        .last("LIMIT " + limit));
    }

    @Override
    public AiWorkflowExecution getExecutionDetail(Long executionId) {
        return executionMapper.selectById(executionId);
    }
}
