import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest'
import request from '@/utils/request'

// Mock dependencies
vi.mock('element-plus', () => ({
  ElMessage: {
    error: vi.fn(),
    warning: vi.fn(),
    success: vi.fn()
  },
  ElMessageBox: {
    confirm: vi.fn()
  }
}))

vi.mock('@/stores/user')

describe('Request Tests', () => {
  beforeEach(() => {
    localStorage.clear()
    vi.clearAllMocks()
  })

  afterEach(() => {
    localStorage.clear()
  })

  describe('Request Function', () => {
    it('should be defined', () => {
      expect(request).toBeDefined()
    })

    it('should have get method', () => {
      expect(request.get).toBeDefined()
      expect(typeof request.get).toBe('function')
    })

    it('should have post method', () => {
      expect(request.post).toBeDefined()
      expect(typeof request.post).toBe('function')
    })

    it('should have put method', () => {
      expect(request.put).toBeDefined()
      expect(typeof request.put).toBe('function')
    })

    it('should have delete method', () => {
      expect(request.delete).toBeDefined()
      expect(typeof request.delete).toBe('function')
    })
  })
})
