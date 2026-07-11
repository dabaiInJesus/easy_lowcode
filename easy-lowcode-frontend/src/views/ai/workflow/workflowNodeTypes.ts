import type { AiWorkflowNodeType } from '@/types/ai-workflow'

/**
 * 所有工作流节点类型定义
 */
export const WORKFLOW_NODE_TYPES: AiWorkflowNodeType[] = [
  // 流程控制
  { type: 'start', label: '开始', icon: 'VideoPlay', color: '#52c41a', category: 'flow' },
  { type: 'end', label: '结束', icon: 'VideoPause', color: '#ff4d4f', category: 'flow' },

  // AI 能力
  { type: 'llm', label: 'LLM 调用', icon: 'ChatDotRound', color: '#1890ff', category: 'ai' },

  // 逻辑控制
  { type: 'condition', label: '条件分支', icon: 'Switch', color: '#faad14', category: 'logic' },
  { type: 'variable', label: '变量操作', icon: 'Edit', color: '#722ed1', category: 'logic' },

  // 工具
  { type: 'code', label: '代码执行', icon: 'Monitor', color: '#13c2c2', category: 'util' },
  { type: 'http', label: 'HTTP 请求', icon: 'Connection', color: '#eb2f96', category: 'util' },
]

/**
 * 按类别分组
 */
export function getWorkflowNodesByCategory(category: string) {
  return WORKFLOW_NODE_TYPES.filter(n => n.category === category)
}

/**
 * 获取节点类型定义
 */
export function getWorkflowNodeType(type: string): AiWorkflowNodeType | undefined {
  return WORKFLOW_NODE_TYPES.find(n => n.type === type)
}

/**
 * 生成唯一节点 ID
 */
export function generateWorkflowNodeId(): string {
  return `wf_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}

/**
 * 节点类型分组名称
 */
export const CATEGORY_LABELS: Record<string, string> = {
  flow: '流程控制',
  ai: 'AI 能力',
  logic: '逻辑控制',
  util: '工具',
}
