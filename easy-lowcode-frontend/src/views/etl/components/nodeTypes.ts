import type { NodeTypeDefinition } from '@/types/flow'

/**
 * 所有节点类型定义
 */
export const NODE_TYPES: NodeTypeDefinition[] = [
  // ===== SOURCE 数据源 =====
  { type: 'mysql', category: 'SOURCE', label: 'MySQL', icon: 'Database', color: '#1890ff' },
  { type: 'postgresql', category: 'SOURCE', label: 'PostgreSQL', icon: 'Database', color: '#336791' },
  { type: 'oracle', category: 'SOURCE', label: 'Oracle', icon: 'Database', color: '#f80000' },
  { type: 'csv', category: 'SOURCE', label: 'CSV', icon: 'Document', color: '#52c41a' },
  { type: 'excel', category: 'SOURCE', label: 'Excel', icon: 'Document', color: '#217346' },
  { type: 'json', category: 'SOURCE', label: 'JSON', icon: 'Document', color: '#faad14' },
  { type: 'ftp', category: 'SOURCE', label: 'FTP', icon: 'Connection', color: '#722ed1' },
  { type: 'sftp', category: 'SOURCE', label: 'SFTP', icon: 'Connection', color: '#13c2c2' },
  { type: 'hive', category: 'SOURCE', label: 'Hive', icon: 'Coin', color: '#ff85c0' },

  // ===== TRANSFORM 转换 =====
  { type: 'filter', category: 'TRANSFORM', label: '过滤', icon: 'Filter', color: '#fa541c' },
  { type: 'map', category: 'TRANSFORM', label: '字段映射', icon: 'Switch', color: '#faad14' },
  { type: 'aggregate', category: 'TRANSFORM', label: '聚合', icon: 'DataAnalysis', color: '#1890ff' },

  // ===== TARGET 目标 =====
  { type: 'mysql', category: 'TARGET', label: 'MySQL', icon: 'Database', color: '#1890ff' },
  { type: 'postgresql', category: 'TARGET', label: 'PostgreSQL', icon: 'Database', color: '#336791' },
  { type: 'oracle', category: 'TARGET', label: 'Oracle', icon: 'Database', color: '#f80000' },
  { type: 'csv', category: 'TARGET', label: 'CSV', icon: 'Document', color: '#52c41a' },
  { type: 'excel', category: 'TARGET', label: 'Excel', icon: 'Document', color: '#217346' },
]

/**
 * 按类别分组的节点类型
 */
export function getNodesByCategory(category: 'SOURCE' | 'TRANSFORM' | 'TARGET') {
  return NODE_TYPES.filter(n => n.category === category)
}

/**
 * 获取节点类型定义
 */
export function getNodeType(type: string, category: string): NodeTypeDefinition | undefined {
  return NODE_TYPES.find(n => n.type === type && n.category === category)
}

/**
 * 生成唯一节点 ID
 */
export function generateNodeId(): string {
  return `node_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`
}
