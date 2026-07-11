package com.dabai.easy_lowcode.etl.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.etl.engine.FlowExecutionEngine;
import com.dabai.easy_lowcode.etl.entity.EtlFlow;
import com.dabai.easy_lowcode.etl.entity.EtlFlowExecution;
import com.dabai.easy_lowcode.etl.entity.EtlNodeConfig;
import com.dabai.easy_lowcode.etl.mapper.EtlFlowExecutionMapper;
import com.dabai.easy_lowcode.etl.mapper.EtlFlowMapper;
import com.dabai.easy_lowcode.etl.mapper.EtlNodeConfigMapper;
import com.dabai.easy_lowcode.etl.service.EtlFlowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 可视化流程服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EtlFlowServiceImpl extends ServiceImpl<EtlFlowMapper, EtlFlow> implements EtlFlowService {

    private final EtlFlowMapper flowMapper;
    private final EtlNodeConfigMapper nodeConfigMapper;
    private final EtlFlowExecutionMapper executionMapper;
    private final FlowExecutionEngine flowExecutionEngine;

    @Override
    public Page<EtlFlow> pageFlow(Page<EtlFlow> page, String keyword, String status) {
        LambdaQueryWrapper<EtlFlow> wrapper = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w.like(EtlFlow::getFlowName, keyword)
                    .or().like(EtlFlow::getFlowCode, keyword));
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(EtlFlow::getFlowStatus, status);
        }
        wrapper.orderByDesc(EtlFlow::getUpdateTime);
        return flowMapper.selectPage(page, wrapper);
    }

    @Override
    public EtlFlow getFlowDetail(Long flowId) {
        EtlFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException("流程不存在");
        }
        // 加载节点配置
        List<EtlNodeConfig> nodes = nodeConfigMapper.selectList(
                new LambdaQueryWrapper<EtlNodeConfig>()
                        .eq(EtlNodeConfig::getFlowId, flowId)
                        .orderByAsc(EtlNodeConfig::getCreateTime));
        flow.setNodes(nodes);
        return flow;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean saveFlow(EtlFlow flow) {
        // 检查编码唯一性
        LambdaQueryWrapper<EtlFlow> check = new LambdaQueryWrapper<>();
        check.eq(EtlFlow::getFlowCode, flow.getFlowCode());
        if (this.count(check) > 0) {
            throw new BusinessException("流程编码已存在: " + flow.getFlowCode());
        }
        flow.setFlowStatus("DRAFT");
        this.save(flow);
        // 保存节点配置
        saveNodeConfigs(flow);
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateFlow(EtlFlow flow) {
        EtlFlow existing = flowMapper.selectById(flow.getId());
        if (existing == null) {
            throw new BusinessException("流程不存在");
        }
        // 检查编码唯一性（排除自身）
        if (flow.getFlowCode() != null && !flow.getFlowCode().equals(existing.getFlowCode())) {
            LambdaQueryWrapper<EtlFlow> check = new LambdaQueryWrapper<>();
            check.eq(EtlFlow::getFlowCode, flow.getFlowCode());
            check.ne(EtlFlow::getId, flow.getId());
            if (this.count(check) > 0) {
                throw new BusinessException("流程编码已存在: " + flow.getFlowCode());
            }
        }
        this.updateById(flow);
        // 更新节点配置（删除旧的，插入新的）
        if (flow.getNodes() != null) {
            nodeConfigMapper.delete(new LambdaQueryWrapper<EtlNodeConfig>()
                    .eq(EtlNodeConfig::getFlowId, flow.getId()));
            saveNodeConfigs(flow);
        }
        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean deleteFlow(Long flowId) {
        EtlFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException("流程不存在");
        }
        if ("RUNNING".equals(flow.getFlowStatus())) {
            throw new BusinessException("运行中的流程不能删除");
        }
        // 删除节点配置
        nodeConfigMapper.delete(new LambdaQueryWrapper<EtlNodeConfig>()
                .eq(EtlNodeConfig::getFlowId, flowId));
        // 删除流程
        this.removeById(flowId);
        return true;
    }

    @Override
    public Long executeFlow(Long flowId) {
        EtlFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException("流程不存在");
        }
        if (flow.getNodesJson() == null || flow.getNodesJson().isBlank()) {
            throw new BusinessException("流程未配置节点");
        }
        if (isFlowRunning(flowId)) {
            throw new BusinessException("流程正在运行中");
        }
        // 调用 FlowExecutionEngine 异步执行流程
        return flowExecutionEngine.executeAsync(flow).join();
    }

    @Override
    public boolean stopFlow(Long flowId) {
        EtlFlow flow = flowMapper.selectById(flowId);
        if (flow == null) {
            throw new BusinessException("流程不存在");
        }
        if (!flowExecutionEngine.isRunning(flowId)) {
            throw new BusinessException("流程未在运行中");
        }
        // 查找正在运行的执行记录
        EtlFlowExecution runningExec = executionMapper.selectOne(
                new LambdaQueryWrapper<EtlFlowExecution>()
                        .eq(EtlFlowExecution::getFlowId, flowId)
                        .eq(EtlFlowExecution::getExecStatus, "RUNNING")
                        .orderByDesc(EtlFlowExecution::getCreateTime)
                        .last("LIMIT 1"));
        if (runningExec == null) {
            throw new BusinessException("未找到运行中的执行记录");
        }
        // 停止执行
        return flowExecutionEngine.stopExecution(runningExec.getId());
    }

    @Override
    public List<EtlFlowExecution> getExecutionHistory(Long flowId, int limit) {
        return executionMapper.selectList(
                new LambdaQueryWrapper<EtlFlowExecution>()
                        .eq(EtlFlowExecution::getFlowId, flowId)
                        .orderByDesc(EtlFlowExecution::getCreateTime)
                        .last("LIMIT " + limit));
    }

    @Override
    public EtlFlowExecution getExecutionDetail(Long executionId) {
        return executionMapper.selectById(executionId);
    }

    @Override
    public boolean isFlowRunning(Long flowId) {
        return flowExecutionEngine.isRunning(flowId);
    }

    /**
     * 批量保存节点配置
     */
    private void saveNodeConfigs(EtlFlow flow) {
        if (flow.getNodes() == null || flow.getNodes().isEmpty()) {
            return;
        }
        for (EtlNodeConfig node : flow.getNodes()) {
            node.setFlowId(flow.getId());
            nodeConfigMapper.insert(node);
        }
    }
}
