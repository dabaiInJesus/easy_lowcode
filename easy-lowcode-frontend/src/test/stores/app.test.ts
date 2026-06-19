import { describe, it, expect, beforeEach } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useAppStore } from '@/stores/app'

describe('appStore', () => {
  beforeEach(() => {
    localStorage.clear()
    setActivePinia(createPinia())
  })

  it('should have initial state with default values', () => {
    const store = useAppStore()
    expect(store.sidebarCollapsed).toBe(false)
    expect(store.theme).toBe('light')
    expect(store.language).toBe('zh-CN')
  })

  it('should toggle sidebar collapsed state', () => {
    const store = useAppStore()
    expect(store.sidebarCollapsed).toBe(false)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(true)

    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(false)
  })

  it('should toggle sidebar multiple times correctly', () => {
    const store = useAppStore()
    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(true)
    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(false)
    store.toggleSidebar()
    expect(store.sidebarCollapsed).toBe(true)
  })

  it('should set sidebar collapsed state directly', () => {
    const store = useAppStore()
    store.setSidebarCollapsed(true)
    expect(store.sidebarCollapsed).toBe(true)

    store.setSidebarCollapsed(false)
    expect(store.sidebarCollapsed).toBe(false)
  })

  it('should set theme and persist to localStorage', () => {
    const store = useAppStore()
    store.setTheme('dark')
    expect(store.theme).toBe('dark')
    expect(localStorage.getItem('theme')).toBe('dark')
  })

  it('should set language and persist to localStorage', () => {
    const store = useAppStore()
    store.setLanguage('en-US')
    expect(store.language).toBe('en-US')
    expect(localStorage.getItem('language')).toBe('en-US')
  })

  it('should restore config from localStorage via restoreConfig', () => {
    localStorage.setItem('theme', 'dark')
    localStorage.setItem('language', 'en-US')

    const store = useAppStore()
    store.restoreConfig()
    expect(store.theme).toBe('dark')
    expect(store.language).toBe('en-US')
  })

  it('should only restore values that exist in localStorage', () => {
    localStorage.setItem('theme', 'dark')

    const store = useAppStore()
    store.restoreConfig()
    expect(store.theme).toBe('dark')
    expect(store.language).toBe('zh-CN')
  })

  it('should keep defaults when no localStorage values exist', () => {
    const store = useAppStore()
    store.restoreConfig()
    expect(store.theme).toBe('light')
    expect(store.language).toBe('zh-CN')
  })
})
