import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import axios, { AxiosError } from 'axios'
import { createRequest } from '@/utils/request'
import { useUserStore } from '@/stores/user'
import router from '@/router'
import { ElMessage } from 'element-plus'

// Mock dependencies
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  },
  ElNotification: {
    error: vi.fn()
  }
}))

vi.mock('@/stores/user')
vi.mock('@/router')

describe('Request Interceptor Tests (API请求拦截器)', () => {
  let request: any
  const mockToken = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...'

  beforeEach(() => {
    // 创建新的axios实例用于测试
    request = createRequest()
    // 清空localStorage
    localStorage.clear()
    // 重置mocks
    vi.clearAllMocks()
  })

  afterEach(() => {
    localStorage.clear()
  })

  // ====== 测试套件1: 请求拦截器 ======

  describe('Request Interceptor', () => {
    /**
     * 测试1: 自动注入Authorization Token
     */
    it('should inject Authorization header with Bearer token', async () => {
      // Arrange
      localStorage.setItem('token', mockToken)
      const requestConfig = { method: 'GET', url: '/api/user/list' }

      // 使用axios的adapter来拦截请求（不真实发送）
      const mockAdapter = vi.spyOn(request, 'request')

      // Act
      try {
        await request.get('/api/user/list')
      } catch (e) {
        // 期望网络错误是正常的（因为没有真实服务器）
      }

      // Assert
      expect(mockAdapter).toHaveBeenCalled()
      const config = mockAdapter.mock.calls[0][0]
      expect(config.headers.Authorization).toBe(`Bearer ${mockToken}`)
    })

    /**
     * 测试2: 没有Token时不注入Authorization
     */
    it('should not inject token when token is not present', async () => {
      // Arrange - 不设置token
      const mockAdapter = vi.spyOn(request, 'request')

      // Act
      try {
        await request.get('/api/public/about')
      } catch (e) {
        // 期望网络错误
      }

      // Assert
      expect(mockAdapter).toHaveBeenCalled()
      const config = mockAdapter.mock.calls[0][0]
      expect(config.headers.Authorization).toBeUndefined()
    })

    /**
     * 测试3: 自动生成X-Request-ID (幂等性)
     */
    it('should generate X-Request-ID for idempotency', async () => {
      // Arrange
      const mockAdapter = vi.spyOn(request, 'request')

      // Act
      try {
        await request.post('/api/datasource/create', { sourceName: 'mysql' })
      } catch (e) {
        // 期望网络错误
      }

      // Assert
      const config = mockAdapter.mock.calls[0][0]
      expect(config.headers['X-Request-ID']).toBeDefined()
      expect(config.headers['X-Request-ID']).toMatch(
        /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i
      ) // UUID格式
    })

    /**
     * 测试4: Content-Type自动设置为application/json
     */
    it('should set Content-Type to application/json', async () => {
      // Arrange
      const mockAdapter = vi.spyOn(request, 'request')

      // Act
      try {
        await request.post('/api/datasource/create', { name: 'test' })
      } catch (e) {
        // 期望网络错误
      }

      // Assert
      const config = mockAdapter.mock.calls[0][0]
      expect(config.headers['Content-Type']).toContain('application/json')
    })

    /**
     * 测试5: 超时设置
     */
    it('should set timeout to 30 seconds', () => {
      // Assert
      expect(request.defaults.timeout).toBe(30000)
    })
  })

  // ====== 测试套件2: 响应拦截器 (成功场景) ======

  describe('Response Interceptor - Success Cases', () => {
    /**
     * 测试6: code=200时自动解包data
     */
    it('should unwrap data when code=200', async () => {
      // Arrange
      const mockResponse = {
        code: 200,
        message: '操作成功',
        data: { id: 1, username: 'admin', email: 'admin@example.com' },
        timestamp: Date.now()
      }

      const mockAdapter = vi.fn().mockResolvedValue({ data: mockResponse })
      request.defaults.adapter = mockAdapter

      // Act
      const result = await request.get('/api/user/1')

      // Assert
      expect(result).toEqual(mockResponse.data) // 应该只返回data部分
      expect(result.code).toBeUndefined() // code不应该返回
    })

    /**
     * 测试7: code=201(创建成功)
     */
    it('should handle code=201 (Created)', async () => {
      // Arrange
      const mockResponse = {
        code: 201,
        message: '创建成功',
        data: { id: 123, resourceCode: 'new_resource' },
        timestamp: Date.now()
      }

      const mockAdapter = vi.fn().mockResolvedValue({ data: mockResponse })
      request.defaults.adapter = mockAdapter

      // Act
      const result = await request.post('/api/resource/create', {
        resourceCode: 'new_resource'
      })

      // Assert
      expect(result).toEqual(mockResponse.data)
      // 前端应该刷新列表或跳转
      expect(result.id).toBe(123)
    })

    /**
     * 测试8: 分页查询返回格式
     */
    it('should return page data with pagination fields', async () => {
      // Arrange
      const mockResponse = {
        code: 200,
        data: {
          current: 1,
          size: 20,
          total: 100,
          pages: 5,
          records: [
            { id: 1, name: 'User 1' },
            { id: 2, name: 'User 2' }
          ]
        }
      }

      const mockAdapter = vi.fn().mockResolvedValue({ data: mockResponse })
      request.defaults.adapter = mockAdapter

      // Act
      const result = await request.get('/api/user/page?current=1&size=20')

      // Assert
      expect(result).toHaveProperty('current')
      expect(result).toHaveProperty('total')
      expect(result).toHaveProperty('records')
      expect(Array.isArray(result.records)).toBe(true)
    })
  })

  // ====== 测试套件3: 响应拦截器 (错误场景) ======

  describe('Response Interceptor - Error Cases', () => {
    /**
     * 测试9: 401未授权 - 跳转登录
     */
    it('should redirect to login on 401 Unauthorized', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 401,
          data: { code: 401, message: 'Token已过期' }
        }
      }

      const userStore = vi.mocked(useUserStore)
      userStore.mockReturnValue({
        clearUser: vi.fn()
      } as any)

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/user/profile')).rejects.toThrow()

      // 验证localStorage被清空
      expect(localStorage.getItem('token')).toBeNull()
    })

    /**
     * 测试10: 403禁止访问 - 显示权限提示
     */
    it('should show permission denied message on 403', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 403,
          data: { code: 403, message: '缺少权限: datasource:delete' }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.delete('/api/datasource/1')).rejects.toThrow()

      // 验证错误消息被显示
      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('权限'))
    })

    /**
     * 测试11: 400参数错误 - 显示具体错误信息
     */
    it('should show validation error on 400', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 400,
          data: {
            code: 400,
            message: '参数验证失败: sourceName不能为空'
          }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(
        request.post('/api/datasource/create', { sourceType: 'mysql' })
      ).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('sourceName')
      )
    })

    /**
     * 测试12: 404资源不存在
     */
    it('should handle 404 Not Found', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 404,
          data: { code: 404, message: '资源不存在' }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/datasource/99999')).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(expect.stringContaining('不存在'))
    })

    /**
     * 测试13: 409冲突 - 资源已存在或重复
     */
    it('should handle 409 Conflict', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 409,
          data: {
            code: 409,
            message: '数据源名称已存在'
          }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(
        request.post('/api/datasource/create', { sourceName: 'existing' })
      ).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('已存在')
      )
    })

    /**
     * 测试14: 429速率限制 - 显示稍后重试提示
     */
    it('should handle 429 Too Many Requests', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 429,
          data: {
            code: 429,
            message: '请求过于频繁，请在5秒后重试'
          }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.post('/api/datasource/test-connection', {})).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('频繁')
      )
    })

    /**
     * 测试15: 500服务器错误 - 显示系统异常
     */
    it('should show system error on 500', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 500,
          data: {
            code: 500,
            message: '服务器内部错误',
            errorId: 'ERR_ABC123'
          }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/datasource/list')).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('系统异常')
      )
    })

    /**
     * 测试16: 503服务不可用 - 显示维护提示
     */
    it('should show maintenance message on 503', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 503,
          data: {
            code: 503,
            message: '服务不可用'
          }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/datasource/list')).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('维护')
      )
    })

    /**
     * 测试17: 网络错误 - 无response对象
     */
    it('should handle network errors gracefully', async () => {
      // Arrange
      const mockError = new Error('Network Error')

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/datasource/list')).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('网络')
      )
    })
  })

  // ====== 测试套件4: 自动重试机制 ======

  describe('Auto Retry Mechanism', () => {
    /**
     * 测试18: 5xx错误自动重试
     */
    it('should auto-retry on 5xx errors', async () => {
      // Arrange
      let callCount = 0
      const mockAdapter = vi.fn().mockImplementation(() => {
        callCount++
        if (callCount < 3) {
          return Promise.reject({
            response: { status: 500, data: { message: 'Server Error' } }
          })
        }
        return Promise.resolve({
          data: { code: 200, data: { success: true } }
        })
      })
      request.defaults.adapter = mockAdapter

      // Act
      const result = await request.get('/api/datasource/list')

      // Assert
      expect(callCount).toBeGreaterThan(1) // 至少重试了一次
      expect(result.success).toBe(true)
    })

    /**
     * 测试19: 4xx错误不重试
     */
    it('should NOT retry on 4xx errors', async () => {
      // Arrange
      let callCount = 0
      const mockAdapter = vi.fn().mockImplementation(() => {
        callCount++
        return Promise.reject({
          response: { status: 400, data: { message: 'Bad Request' } }
        })
      })
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/datasource/list')).rejects.toThrow()

      expect(callCount).toBe(1) // 只调用了一次，没有重试
    })

    /**
     * 测试20: 超过最大重试次数后放弃
     */
    it('should give up after max retries exceeded', async () => {
      // Arrange
      let callCount = 0
      const mockAdapter = vi.fn().mockImplementation(() => {
        callCount++
        return Promise.reject({
          response: { status: 503, data: { message: 'Service Unavailable' } }
        })
      })
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/datasource/list')).rejects.toThrow()

      // 应该重试3次后放弃 (1次原始 + 3次重试 = 4次总调用)
      expect(callCount).toBeLessThanOrEqual(4)
    })
  })

  // ====== 测试套件5: 业务错误码处理 ======

  describe('Business Error Code Handling', () => {
    /**
     * 测试21: SQL注入检测 (30011)
     */
    it('should handle SQL injection detection (30011)', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 403,
          data: { code: 30011, message: 'SQL注入风险检测' }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(
        request.post('/api/resource/query', { sql: "'; DROP TABLE--" })
      ).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('注入')
      )
    })

    /**
     * 测试22: 字段权限检查 (30012)
     */
    it('should handle unauthorized field access (30012)', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 403,
          data: { code: 30012, message: '字段不在白名单: password' }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(
        request.post('/api/resource/query', {
          fields: ['id', 'username', 'password']
        })
      ).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('password')
      )
    })

    /**
     * 测试23: Token过期 (10011)
     */
    it('should handle token expiration (10011)', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 401,
          data: { code: 10011, message: 'Token已过期' }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(request.get('/api/user/profile')).rejects.toThrow()

      // 应该清除token并跳转登录
      expect(localStorage.getItem('token')).toBeNull()
    })

    /**
     * 测试24: 数据源连接失败 (20001)
     */
    it('should handle datasource connection error (20001)', async () => {
      // Arrange
      const mockError = {
        response: {
          status: 500,
          data: { code: 20001, message: '数据源连接失败: Connection refused' }
        }
      }

      const mockAdapter = vi.fn().mockRejectedValue(mockError)
      request.defaults.adapter = mockAdapter

      // Act & Assert
      await expect(
        request.post('/api/datasource/test', { host: 'invalid-host' })
      ).rejects.toThrow()

      expect(ElMessage.error).toHaveBeenCalledWith(
        expect.stringContaining('连接失败')
      )
    })
  })
})
