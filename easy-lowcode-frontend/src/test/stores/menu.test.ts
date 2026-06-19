import { describe, it, expect, beforeEach, vi } from 'vitest'
import { setActivePinia, createPinia } from 'pinia'
import { useMenuStore } from '@/stores/menu'
import type { MenuItem } from '@/stores/menu'
import { getMenuTree } from '@/api/auth'
import router from '@/router'

vi.mock('@/api/auth', () => ({
  getMenuTree: vi.fn(),
}))

vi.mock('@/router', () => ({
  default: {
    addRoute: vi.fn(),
    removeRoute: vi.fn(),
    hasRoute: vi.fn(() => true),
  },
}))

function createMockMenu(overrides: Partial<MenuItem> = {}): MenuItem {
  return {
    id: 1,
    parentId: null,
    menuName: 'Test Menu',
    menuCode: 'test-menu',
    menuType: 1,
    path: '/test',
    component: 'system/UserManagement',
    icon: 'el-icon-setting',
    sort: 1,
    visible: 1,
    ...overrides,
  }
}

describe('menuStore', () => {
  beforeEach(() => {
    setActivePinia(createPinia())
    vi.clearAllMocks()
  })

  it('should have initial state with empty menus and not loaded', () => {
    const store = useMenuStore()
    expect(store.menus).toEqual([])
    expect(store.isLoaded).toBe(false)
    expect(store.visibleMenus).toEqual([])
  })

  it('should set menus list by direct assignment', () => {
    const store = useMenuStore()
    const mockMenus = [createMockMenu({ id: 1 })]
    store.menus = mockMenus
    expect(store.menus).toHaveLength(1)
    expect(store.menus[0].id).toBe(1)
    expect(store.isLoaded).toBe(false)
  })

  it('should filter invisible menus in visibleMenus computed', () => {
    const store = useMenuStore()
    store.menus = [
      createMockMenu({ id: 1, visible: 1, path: '/visible' }),
      createMockMenu({ id: 2, visible: 0, path: '/invisible' }),
    ]
    expect(store.visibleMenus).toHaveLength(1)
    expect(store.visibleMenus[0].path).toBe('/visible')
  })

  it('should exclude /dashboard/design and /dashboard/view routes', () => {
    const store = useMenuStore()
    store.menus = [
      createMockMenu({ id: 1, path: '/dashboard/manage', visible: 1 }),
      createMockMenu({ id: 2, path: '/dashboard/design', visible: 1 }),
      createMockMenu({ id: 3, path: '/dashboard/view', visible: 1 }),
    ]
    expect(store.visibleMenus).toHaveLength(1)
    expect(store.visibleMenus[0].path).toBe('/dashboard/manage')
  })

  it('should filter excluded child menu paths recursively', () => {
    const store = useMenuStore()
    store.menus = [
      createMockMenu({
        id: 1,
        path: '/dashboard',
        visible: 1,
        children: [
          createMockMenu({ id: 11, path: '/dashboard/manage', visible: 1 }),
          createMockMenu({ id: 12, path: '/dashboard/design', visible: 1 }),
          createMockMenu({ id: 13, path: '/dashboard/view', visible: 1 }),
        ] as MenuItem[],
      }),
    ]
    expect(store.visibleMenus).toHaveLength(1)
    expect(store.visibleMenus[0].children).toHaveLength(1)
    expect(store.visibleMenus[0].children![0].path).toBe('/dashboard/manage')
  })

  it('should clear menus and remove routes via clearMenus', () => {
    const store = useMenuStore()
    store.menus = [
      createMockMenu({ id: 1, path: '/test', menuCode: 'test-menu' }),
    ]
    store.clearMenus()

    expect(store.menus).toEqual([])
    expect(store.isLoaded).toBe(false)
    expect(router.removeRoute).toHaveBeenCalledWith('test-menu')
  })

  it('should not remove routes that do not exist in router', () => {
    vi.mocked(router.hasRoute).mockReturnValue(false)
    const store = useMenuStore()
    store.menus = [
      createMockMenu({ id: 1, menuCode: 'non-existent-route' }),
    ]
    store.clearMenus()

    expect(router.removeRoute).not.toHaveBeenCalled()
    expect(store.menus).toEqual([])
    expect(store.isLoaded).toBe(false)
  })

  it('loadMenus should fetch menus from API and set state', async () => {
    const mockMenus = [
      createMockMenu({ id: 1, path: '/test', component: 'system/UserManagement' }),
    ]
    vi.mocked(getMenuTree).mockResolvedValue({ data: mockMenus })

    const store = useMenuStore()
    const result = await store.loadMenus()

    expect(getMenuTree).toHaveBeenCalledTimes(1)
    expect(store.isLoaded).toBe(true)
    expect(store.menus).toEqual(mockMenus)
    expect(router.addRoute).toHaveBeenCalled()
  })

  it('loadMenus should not re-fetch if data is already loaded', async () => {
    const store = useMenuStore()
    store.menus = [createMockMenu({ id: 1 })]
    store.isLoaded = true

    const result = await store.loadMenus()

    expect(getMenuTree).not.toHaveBeenCalled()
    expect(result).toEqual([createMockMenu({ id: 1 })])
  })

  it('loadMenus should throw on API error and keep isLoaded false', async () => {
    const error = new Error('API Error')
    vi.mocked(getMenuTree).mockRejectedValue(error)

    const store = useMenuStore()
    await expect(store.loadMenus()).rejects.toThrow('API Error')
    expect(store.isLoaded).toBe(false)
  })

  it('loadMenus should handle response without .data field', async () => {
    vi.mocked(getMenuTree).mockResolvedValue({})

    const store = useMenuStore()
    const result = await store.loadMenus()

    expect(store.menus).toEqual([])
    expect(store.isLoaded).toBe(true)
  })
})
