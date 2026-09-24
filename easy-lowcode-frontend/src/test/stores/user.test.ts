import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useUserStore } from '@/stores/user'
import type { UserInfo } from '@/api/auth'

describe('userStore', () => {
  beforeEach(() => {
    // user store 基于安全考虑使用 sessionStorage，测试需同时清理两种 storage 防止用例间串味
    localStorage.clear()
    sessionStorage.clear()
    setActivePinia(createPinia())
  })

  it('should have initial state with empty token and null userInfo', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(store.username).toBe('')
    expect(store.nickname).toBe('')
    expect(store.avatar).toBe('')
  })

  it('should set token and persist to sessionStorage', () => {
    const store = useUserStore()
    store.setToken('jwt-token-abc')
    expect(store.token).toBe('jwt-token-abc')
    expect(store.isLoggedIn).toBe(true)
    expect(sessionStorage.getItem('token')).toBe('jwt-token-abc')
  })

  it('should set user info and compute derived fields', () => {
    const store = useUserStore()
    const mockUser: UserInfo = {
      id: 1,
      username: 'admin',
      nickname: '管理员',
      realName: '张三',
      phone: '13800138000',
      email: 'admin@example.com',
      avatar: '/avatars/admin.png',
      gender: 1,
      status: 1,
      deptId: 1,
    }
    store.setUserInfo(mockUser)
    expect(store.userInfo).toEqual(mockUser)
    expect(store.username).toBe('admin')
    expect(store.nickname).toBe('管理员')
    expect(store.avatar).toBe('/avatars/admin.png')
  })

  it('should clear all user data and remove token from sessionStorage on logout', () => {
    const store = useUserStore()
    store.setToken('jwt-token-abc')
    store.setUserInfo({
      id: 1,
      username: 'admin',
      nickname: '管理员',
      realName: '',
      phone: '',
      email: '',
      avatar: '',
      gender: 0,
      status: 1,
      deptId: 1,
    } as UserInfo)
    expect(store.isLoggedIn).toBe(true)

    store.logout()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(store.isLoggedIn).toBe(false)
    expect(sessionStorage.getItem('token')).toBeNull()
  })

  it('should restore token from sessionStorage at store creation', () => {
    sessionStorage.setItem('token', 'pre-existing-token')
    const store = useUserStore()
    expect(store.token).toBe('pre-existing-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('should persist token across store recreation via sessionStorage', () => {
    const store = useUserStore()
    store.setToken('persistent-token')
    expect(sessionStorage.getItem('token')).toBe('persistent-token')

    const newStore = useUserStore()
    expect(newStore.token).toBe('persistent-token')
    expect(newStore.isLoggedIn).toBe(true)
  })

  it('should restore token on demand via restoreToken', () => {
    const store = useUserStore()
    expect(store.token).toBe('')
    sessionStorage.setItem('token', 'restored-token')
    store.restoreToken()
    expect(store.token).toBe('restored-token')
    expect(store.isLoggedIn).toBe(true)
  })

  it('should clear user via clearUser without calling logout', () => {
    const store = useUserStore()
    store.setToken('temp-token')
    store.setUserInfo({
      id: 2,
      username: 'test',
      nickname: 'Test',
      realName: '',
      phone: '',
      email: '',
      avatar: '',
      gender: 0,
      status: 1,
      deptId: 2,
    } as UserInfo)

    store.clearUser()
    expect(store.token).toBe('')
    expect(store.userInfo).toBeNull()
    expect(sessionStorage.getItem('token')).toBeNull()
  })

  it('should handle partial userInfo gracefully', () => {
    const store = useUserStore()
    store.setUserInfo({
      id: 3,
      username: 'guest',
      nickname: '',
      realName: '',
      phone: '',
      email: '',
      avatar: '',
      gender: 0,
      status: 1,
      deptId: 3,
    } as UserInfo)
    expect(store.username).toBe('guest')
    expect(store.nickname).toBe('')
    expect(store.avatar).toBe('')
  })
})
