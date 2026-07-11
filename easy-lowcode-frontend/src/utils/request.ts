import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'
import { API_BASE_URL, REQUEST_TIMEOUT } from '@/config/app'

// 响应数据类型
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

// 请求配置输入类型（headers 等可选，方便 API 层调用）
export interface RequestConfig extends AxiosRequestConfig {
  silentError?: boolean // 是否静默错误（不显示错误提示）
  retryCount?: number // 请求重试次数
  loadingId?: string // 加载状态标识（防止重复请求）
}

// 内部拦截器使用的完整配置类型
interface InternalConfig extends InternalAxiosRequestConfig {
  silentError?: boolean
  retryCount?: number
  loadingId?: string
}

// 防止重复请求的 Map
const pendingRequests = new Map<string, AbortController>()

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: API_BASE_URL,
  timeout: REQUEST_TIMEOUT,
})

// 请求拦截器
service.interceptors.request.use(
  (config: InternalConfig) => {
    // 防止重复请求
    if (config.loadingId) {
      const key = `${config.method}:${config.url}:${JSON.stringify(config.params || config.data || {})}`
      const existing = pendingRequests.get(key)
      if (existing) {
        existing.abort() // 取消之前的请求
      }
      const controller = new AbortController()
      config.signal = controller.signal
      pendingRequests.set(key, controller)
      controller.signal.addEventListener('abort', () => {
        pendingRequests.delete(key)
      })
    }

    // 从 Pinia store 获取 token
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }

    // 添加请求ID（用于链路追踪）
    config.headers['X-Request-Id'] = generateRequestId()

    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器 — 解包 response.data，调用方直接拿到业务数据
service.interceptors.response.use(
  // @ts-expect-error — 拦截器解包后返回 data 而非 AxiosResponse，这是 axios 常见用法
  (response) => {
    const res: ApiResponse<unknown> = response.data

    // 如果返回的状态码不是 200，则认为是错误
    if (res.code !== 200) {
      const config = response.config as InternalConfig
      if (!config.silentError) {
        ElMessage.error(res.message || '请求失败')
      }

      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.clearUser()
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    return res.data
  },
  (error) => {
    const errMsg = error.response?.data?.message || error.message || '网络错误'
    const config = error.config as InternalConfig
    if (!config?.silentError) {
      ElMessage.error(errMsg)
    }
    return Promise.reject(new Error(errMsg))
  }
)

// 生成请求ID
function generateRequestId(): string {
  return `${Date.now()}-${Math.random().toString(36).substring(2, 11)}`
}

// 导出 axios 实例
export { service }

// 导出带类型的 request，支持 silentError
function request<T = unknown>(config: RequestConfig): Promise<T> {
  return service(config as InternalAxiosRequestConfig) as Promise<T>
}

request.get = <T = unknown>(url: string, config?: RequestConfig) =>
  service.get<T>(url, config as InternalAxiosRequestConfig) as Promise<T>
request.post = <T = unknown>(url: string, data?: unknown, config?: RequestConfig) =>
  service.post<T>(url, data, config as InternalAxiosRequestConfig) as Promise<T>
request.put = <T = unknown>(url: string, data?: unknown, config?: RequestConfig) =>
  service.put<T>(url, data, config as InternalAxiosRequestConfig) as Promise<T>
request.delete = <T = unknown>(url: string, config?: RequestConfig) =>
  service.delete<T>(url, config as InternalAxiosRequestConfig) as Promise<T>

export default request
