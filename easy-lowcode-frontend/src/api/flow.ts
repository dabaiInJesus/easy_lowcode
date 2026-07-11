import request from '@/utils/request'
import type { FlowDefinition, FlowExecution } from '@/types/flow'

/**
 * 分页查询流程列表
 */
export function getFlowPage(current: number, size: number, keyword?: string, status?: string) {
  return request({
    url: '/etl/flow/page',
    method: 'get',
    params: { current, size, keyword, status },
  })
}

/**
 * 获取流程详情
 */
export function getFlowDetail(id: number): Promise<FlowDefinition> {
  return request({
    url: `/etl/flow/${id}`,
    method: 'get',
  })
}

/**
 * 创建流程
 */
export function createFlow(data: Partial<FlowDefinition>) {
  return request({
    url: '/etl/flow',
    method: 'post',
    data,
  })
}

/**
 * 更新流程
 */
export function updateFlow(data: Partial<FlowDefinition>) {
  return request({
    url: '/etl/flow',
    method: 'put',
    data,
  })
}

/**
 * 删除流程
 */
export function deleteFlow(id: number) {
  return request({
    url: `/etl/flow/${id}`,
    method: 'delete',
  })
}

/**
 * 执行流程
 */
export function executeFlow(id: number): Promise<number> {
  return request({
    url: `/etl/flow/${id}/execute`,
    method: 'post',
  })
}

/**
 * 停止流程
 */
export function stopFlow(id: number) {
  return request({
    url: `/etl/flow/${id}/stop`,
    method: 'post',
  })
}

/**
 * 获取执行历史
 */
export function getExecutionHistory(flowId: number, limit?: number): Promise<FlowExecution[]> {
  return request({
    url: `/etl/flow/${flowId}/history`,
    method: 'get',
    params: { limit: limit || 20 },
  })
}

/**
 * 获取执行详情
 */
export function getExecutionDetail(executionId: number): Promise<FlowExecution> {
  return request({
    url: `/etl/flow/execution/${executionId}`,
    method: 'get',
  })
}
