import request from '@/utils/request'

/**
 * 分页查询 Agent 列表
 */
export function getAgentPage(current: number, size: number, keyword?: string) {
  return request({
    url: '/ai/agent/list',
    method: 'get',
    params: { current, size, keyword },
  })
}

/**
 * 创建 Agent
 */
export function createAgent(data: any) {
  return request({
    url: '/ai/agent/create',
    method: 'post',
    data,
  })
}

/**
 * 更新 Agent
 */
export function updateAgent(data: any) {
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
  return request({
    url: '/ai/agent/tools',
    method: 'get',
  })
}
