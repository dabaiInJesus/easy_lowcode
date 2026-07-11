/**
 * AI provider configuration matching AiConfig.java
 */
export interface AiConfig {
  id?: string
  provider: string
  displayName: string
  baseUrl: string
  apiKey?: string
  secretKey?: string
  model: string
  isDefault?: number
  status?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

/**
 * AI agent entity matching AiAgent.java
 */
export interface AiAgent {
  id?: number
  agentName: string
  agentCode: string
  description?: string
  avatar?: string
  provider: string
  model: string
  promptTemplateId?: number
  temperature?: number
  maxTokens?: number
  enableWorkflow?: number
  workflowConfig?: string
  instructions?: string
  variablesConfig?: string
  openingStatement?: string
  suggestedQuestions?: string
  toolsConfig?: string
  knowledgeIds?: string
  maxIterations?: number
  enablePlanning?: number
  status?: number
  publishStatus?: number
  usageCount?: number
  createTime?: string
  updateTime?: string
}

/**
 * Agent 工具定义
 */
export interface AgentTool {
  name: string
  description: string
  parameters: Record<string, any>
}

/**
 * Agent 对话消息
 */
export interface AgentMessage {
  role: 'user' | 'assistant' | 'tool' | 'system'
  content: string
  toolName?: string
  toolInput?: string
  toolOutput?: string
  iteration?: number
}

/**
 * SSE 事件
 */
export interface AgentSSEEvent {
  type: 'thought' | 'tool_start' | 'tool_result' | 'response' | 'done' | 'error'
  data: Record<string, any>
  timestamp: number
}
