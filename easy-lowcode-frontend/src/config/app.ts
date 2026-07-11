/**
 * 应用全局配置
 * 集中管理常量，避免硬编码分散在各处
 */

// 分页默认值
export const PAGINATION = {
  DEFAULT_PAGE_SIZE: 10,
  PAGE_SIZES: [10, 20, 50, 100] as number[],
  MAX_PAGE_SIZE: 200,
} as const

// 请求超时（毫秒）
export const REQUEST_TIMEOUT = 30000

// API 基础路径
export const API_BASE_URL = '/api'

// AI Provider 默认配置
export const AI_PROVIDERS = {
  OPENAI: { name: 'openai', baseUrl: 'https://api.openai.com/v1' },
  DASHSCOPE: { name: 'dashscope', baseUrl: 'https://dashscope.aliyuncs.com/api/v1' },
  DEEPSEEK: { name: 'deepseek', baseUrl: 'https://api.deepseek.com/v1' },
  MINIMAX: { name: 'minimax', baseUrl: 'https://api.minimax.chat/v1' },
} as const

// 操作确认消息
export const CONFIRM_MESSAGES = {
  DELETE: '确定要删除吗？此操作不可撤销。',
  DELETE_ITEM: (name: string) => `确定要删除 "${name}" 吗？此操作不可撤销。`,
} as const
