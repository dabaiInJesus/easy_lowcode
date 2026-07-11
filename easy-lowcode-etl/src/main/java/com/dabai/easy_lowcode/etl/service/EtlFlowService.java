package com.dabai.easy_lowcode.etl.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.etl.entity.EtlFlow;
import com.dabai.easy_lowcode.etl.entity.EtlFlowExecution;

import java.util.List;

/**
 * 可视化流程服务接口
 */
public interface EtlFlowService extends IService<EtlFlow> {

    /**
     * 分页查询流程列表
     */
    Page<EtlFlow> pageFlow(Page<EtlFlow> page, String keyword, String status);

    /**
     * 获取流程详情（含节点和连线）
     */
    EtlFlow getFlowDetail(Long flowId);

    /**
     * 保存流程（含节点和连线）
     */
    boolean saveFlow(EtlFlow flow);

    /**
     * 更新流程（含节点和连线）
     */
    boolean updateFlow(EtlFlow flow);

    /**
     * 删除流程（级联删除节点配置）
     */
    boolean deleteFlow(Long flowId);

    /**
     * 执行流程
     */
    Long executeFlow(Long flowId);

    /**
     * 停止流程
     */
    boolean stopFlow(Long flowId);

    /**
     * 获取流程执行历史
     */
    List<EtlFlowExecution> getExecutionHistory(Long flowId, int limit);

    /**
     * 获取执行详情
     */
    EtlFlowExecution getExecutionDetail(Long executionId);

    /**
     * 检查流程是否正在运行
     */
    boolean isFlowRunning(Long flowId);
}
