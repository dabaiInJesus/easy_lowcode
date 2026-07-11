import request from '@/utils/request'
import type { PageResult } from '@/types/common'
import type { AiAgent, AgentMessage } from '@/types/ai'

export interface AiConfig {
  id?: number
  provider: string
  displayName: string
  apiKey?: string
  baseUrl?: string
  model?: string
  status?: number
  remark?: string
}

export function chat(data: { message: string; history?: {role:string;content:string}[]; provider?: string }): Promise<string> {
  if (data.provider) {
    return request({ url: `/ai/chat/${data.provider}`, method: 'post', data })
  }
  return request({ url: '/ai/chat', method: 'post', data })
}

export function streamChat(data: { message: string; history?: {role:string;content:string}[]; provider?: string }): Promise<ReadableStream> {
  return request({
    url: '/ai/chat/stream',
    method: 'post',
    data,
    responseType: 'text',
    headers: { 'Accept': 'text/event-stream' },
    timeout: 60000,
  })
}

export function getProviders(): Promise<{name:string;label:string;enabled:boolean}[]> {
  return request({ url: '/ai/providers', method: 'get' })
}

export function testAiConnection(data: { provider: string; apiKey?: string; apiUrl?: string; model?: string }): Promise<boolean> {
  return request({ url: '/ai/test', method: 'post', data })
}

export function getAiConfigPage(current: number, size: number): Promise<PageResult<AiConfig>> {
  return request({ url: '/ai/config/page', method: 'get', params: { current, size } })
}

export function getAiConfigList(): Promise<AiConfig[]> {
  return request({ url: '/ai/config/list', method: 'get' })
}

export function getAiConfigById(id: number): Promise<AiConfig> {
  return request({ url: `/ai/config/${id}`, method: 'get' })
}

export function createAiConfig(data: Partial<AiConfig>): Promise<void> {
  return request({ url: '/ai/config', method: 'post', data })
}

export function updateAiConfig(data: Partial<AiConfig>): Promise<void> {
  return request({ url: '/ai/config', method: 'put', data })
}

export function deleteAiConfig(id: number): Promise<void> {
  return request({ url: `/ai/config/${id}`, method: 'delete' })
}

export function executeAgent(data: { agentId: string; input: string }): Promise<string> {
  return request({ url: '/ai/agent/execute', method: 'post', data })
}

export function getAgentList(): Promise<{id:string;name:string;description:string}[]> {
  return request({ url: '/ai/agent/list', method: 'get' })
}

export function getAgentDetail(id: number) {
  return request({ url: `/ai/agent/${id}`, method: 'get' })
}

export function updateAgent(data: AiAgent) {
  return request({ url: '/ai/agent', method: 'put', data })
}

export function getAgentTools() {
  return request<Array<{ name: string; description: string }>>({ url: '/ai/agent/tools', method: 'get' })
}

export function getAgentHistory(agentCode: string, sessionId?: string) {
  return request<AgentMessage[]>({ url: `/ai/agent/history/${agentCode}`, method: 'get', params: { sessionId } })
}

export function clearAgentSession(agentCode: string, sessionId?: string) {
  return request({ url: `/ai/agent/session/${agentCode}`, method: 'delete', params: { sessionId } })
}
