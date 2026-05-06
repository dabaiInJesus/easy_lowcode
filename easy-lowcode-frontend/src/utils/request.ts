import axios from 'axios'
import type { AxiosInstance, InternalAxiosRequestConfig, AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores'

// 响应数据类型
export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
}

// 扩展请求配置，添加静默错误选项
export interface CustomRequestConfig extends InternalAxiosRequestConfig {
  silentError?: boolean // 是否静默错误（不显示错误提示）
}

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: '/api',
  timeout: 15000,
})

// 请求拦截器
service.interceptors.request.use(
  (config: CustomRequestConfig) => {
    // 从 Pinia store 获取 token
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers['Authorization'] = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => {
    console.error('Request error:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res: ApiResponse<any> = response.data

    // 如果返回的状态码不是 200，则认为是错误
    if (res.code !== 200) {
      // 检查是否设置了静默错误
      const config = response.config as CustomRequestConfig
      if (!config.silentError) {
        ElMessage.error(res.message || '请求失败')
      }

      // 401: 未授权，需要重新登录（总是显示）
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.clearUser()
        window.location.href = '/login'
      }

      return Promise.reject(new Error(res.message || '请求失败'))
    }

    // 返回解包后的数据（ApiResponse.data）
    return res.data
  },
  (error) => {
    console.error('Response error:', error)
    // 检查是否设置了静默错误
    const config = error.config as CustomRequestConfig
    if (!config?.silentError) {
      ElMessage.error(error.message || '网络错误')
    }
    return Promise.reject(error)
  }
)

// 导出 axios 实例（支持 request(config) 方式）
export { service }

// 导出带类型的 request，支持 silentError
// eslint-disable-next-line @typescript-eslint/no-explicit-any
const request: any = function (config: CustomRequestConfig) {
  return service(config as any)
}

request.get = <T = any>(url: string, config?: CustomRequestConfig) => service.get(url, config as any) as Promise<T>
request.post = <T = any>(url: string, data?: any, config?: CustomRequestConfig) => service.post(url, data, config as any) as Promise<T>
request.put = <T = any>(url: string, data?: any, config?: CustomRequestConfig) => service.put(url, data, config as any) as Promise<T>
request.delete = <T = any>(url: string, config?: CustomRequestConfig) => service.delete(url, config as any) as Promise<T>

export default request
