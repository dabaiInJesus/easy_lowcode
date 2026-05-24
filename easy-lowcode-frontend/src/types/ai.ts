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
  id?: string
  agentName: string
  agentCode: string
  description?: string
  avatar?: string
  provider: string
  model: string
  promptTemplateId?: string
  temperature?: number
  maxTokens?: number
  enableWorkflow?: number
  workflowConfig?: string
  instructions?: string
  variablesConfig?: string
  openingStatement?: string
  suggestedQuestions?: string
  status?: number
  publishStatus?: number
  usageCount?: number
  createTime?: string
  updateTime?: string
}
