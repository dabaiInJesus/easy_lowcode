import request from '@/utils/request'
import type { AiAgent } from '@/types/ai'
import type { PageResult } from '@/types/common'

/**
 * 分页查询 Agent 列表
 */
export function getAgentPage(current: number, size: number, keyword?: string) {
  return request<PageResult<AiAgent>>({
    url: '/ai/agent/list',
    method: 'get',
    params: { current, size, keyword },
  })
}

/**
 * 创建 Agent
 */
export function createAgent(data: Omit<AiAgent, 'id'>) {
  return request({
    url: '/ai/agent/create',
    method: 'post',
    data,
  })
}

/**
 * 更新 Agent
 */
export function updateAgent(data: AiAgent) {
  return request({
    url: '/ai/agent',
    method: 'put',
    data,
  })
}

/**
 * 删除 Agent
 */
export function deleteAgent(id: number) {
  return request({
    url: `/ai/agent/${id}`,
    method: 'delete',
  })
}

/**
 * 发布 Agent
 */
export function publishAgent(id: number) {
  return request({
    url: `/ai/agent/${id}/publish`,
    method: 'post',
  })
}

/**
 * 获取可用工具列表
 */
export function getAgentTools() {
  return request<Array<{ name: string; description: string }>>({
    url: '/ai/agent/tools',
    method: 'get',
  })
}
