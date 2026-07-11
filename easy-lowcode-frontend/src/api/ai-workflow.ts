import request from '@/utils/request'
import type { AiWorkflowDef, AiWorkflowExecution } from '@/types/ai-workflow'
import type { PageResult } from '@/types/common'

/**
 * 分页查询工作流列表
 */
export function getWorkflowPage(current: number, size: number, keyword?: string, status?: string) {
  return request<PageResult<AiWorkflowDef>>({
    url: '/ai/workflow/page',
    method: 'get',
    params: { current, size, keyword, status },
  })
}

/**
 * 获取工作流详情
 */
export function getWorkflowDetail(id: number): Promise<AiWorkflowDef> {
  return request<AiWorkflowDef>({
    url: `/ai/workflow/${id}`,
    method: 'get',
  })
}

/**
 * 创建工作流
 */
export function createWorkflow(data: Partial<AiWorkflowDef>) {
  return request({
    url: '/ai/workflow',
    method: 'post',
    data,
  })
}

/**
 * 更新工作流
 */
export function updateWorkflow(data: Partial<AiWorkflowDef>) {
  return request({
    url: '/ai/workflow',
    method: 'put',
    data,
  })
}

/**
 * 删除工作流
 */
export function deleteWorkflow(id: number) {
  return request({
    url: `/ai/workflow/${id}`,
    method: 'delete',
  })
}

/**
 * 发布工作流
 */
export function publishWorkflow(id: number) {
  return request({
    url: `/ai/workflow/${id}/publish`,
    method: 'post',
  })
}

/**
 * 获取执行历史
 */
export function getWorkflowHistory(workflowId: number, limit?: number): Promise<AiWorkflowExecution[]> {
  return request<AiWorkflowExecution[]>({
    url: `/ai/workflow/${workflowId}/history`,
    method: 'get',
    params: { limit: limit || 20 },
  })
}

/**
 * 获取执行详情
 */
export function getWorkflowExecutionDetail(executionId: number): Promise<AiWorkflowExecution> {
  return request<AiWorkflowExecution>({
    url: `/ai/workflow/execution/${executionId}`,
    method: 'get',
  })
}

/**
 * 执行工作流（SSE流式）
 */
export function executeWorkflowSSE(workflowId: number, inputVariables?: Record<string, unknown>): EventSource {
  const params = new URLSearchParams()
  if (inputVariables) {
    Object.entries(inputVariables).forEach(([k, v]) => {
      params.append(k, String(v))
    })
  }

  const url = `/api/ai/workflow/${workflowId}/execute?${params.toString()}`

  return new EventSource(url, {
    withCredentials: true,
  })
}
