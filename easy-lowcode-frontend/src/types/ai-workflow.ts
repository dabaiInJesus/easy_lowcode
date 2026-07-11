/**
 * AI 工作流类型定义
 */

export interface AiWorkflowDef {
  id?: number
  workflowName: string
  workflowCode: string
  description?: string
  nodesJson?: string
  edgesJson?: string
  variablesJson?: string
  status: string
  remark?: string
  nodes?: WorkflowNode[]
  edges?: WorkflowEdge[]
}

export interface WorkflowNode {
  id: string
  type: 'start' | 'llm' | 'code' | 'http' | 'condition' | 'variable' | 'end'
  data: WorkflowNodeData
  position: { x: number; y: number }
}

export interface WorkflowNodeData {
  label?: string
  nodeType?: string
  config?: Record<string, any>
  [key: string]: any
}

export interface WorkflowEdge {
  id: string
  source: string
  target: string
  label?: string
  animated?: boolean
  style?: Record<string, any>
}

export interface AiWorkflowExecution {
  id?: number
  workflowId: number
  workflowName?: string
  execStatus: string
  startTime?: string
  endTime?: string
  inputVariables?: string
  outputVariables?: string
  nodeExecutions?: string
  errorMessage?: string
}

export interface AiWorkflowNodeType {
  type: string
  label: string
  icon: string
  color: string
  category: 'flow' | 'ai' | 'logic' | 'util'
  configSchema?: Record<string, any>
}

export interface WorkflowSSEEvent {
  type: 'node_start' | 'node_output' | 'node_complete' | 'node_error' | 'workflow_complete' | 'workflow_error'
  data: Record<string, any>
  timestamp: number
}
