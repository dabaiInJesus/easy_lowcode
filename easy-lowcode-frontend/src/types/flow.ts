/**
 * 流程定义类型
 */
export interface FlowDefinition {
  id?: number
  flowName: string
  flowCode: string
  description?: string
  nodesJson?: string
  edgesJson?: string
  flowStatus: string
  scheduleType: string
  cronExpression?: string
  intervalSeconds?: number
  lastExecTime?: string
  lastExecStatus?: string
  remark?: string
  nodes?: FlowNodeConfig[]
  lastExecution?: FlowExecution
}

/**
 * 节点配置
 */
export interface FlowNodeConfig {
  id?: number
  flowId?: number
  nodeId: string
  nodeType: 'SOURCE' | 'TRANSFORM' | 'TARGET'
  nodeSubType: string
  nodeName: string
  configJson?: string
  positionX: number
  positionY: number
}

/**
 * 执行记录
 */
export interface FlowExecution {
  id?: number
  flowId: number
  flowName?: string
  execStatus: string
  startTime?: string
  endTime?: string
  readCount: number
  writeCount: number
  skipCount: number
  errorCount: number
  errorMessage?: string
  execDetail?: string
}

/**
 * 节点类型定义
 */
export interface NodeTypeDefinition {
  type: string
  category: 'SOURCE' | 'TRANSFORM' | 'TARGET'
  label: string
  icon: string
  color: string
  configSchema?: Record<string, FieldConfig>
}

/**
 * 字段配置 schema
 */
export interface FieldConfig {
  type: 'string' | 'number' | 'boolean' | 'select' | 'textarea' | 'password'
  label: string
  placeholder?: string
  default?: any
  options?: string[]
  inputType?: string
}
