import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMenuTree } from '@/api/auth'

/** 允许动态加载的组件路径白名单 */
const ALLOWED_COMPONENTS = new Set([
  'system/UserManagement', 'system/RoleManagement', 'system/MenuManagement',
  'system/DeptManagement', 'system/AppManagement',
  'resource/DataSourceManagement', 'resource/TableResourceManagement', 'resource/ApiManagement',
  'etl/EtlTaskManagement', 'dashboard/DashboardManagement', 'dashboard/DashboardView',
  'dashboard/DashboardDesigner', 'ai/ChatView', 'ai/AiConfigManagement',
  'Home', 'Layout'
])

export interface MenuItem {
  id: number | string
  parentId: number | null | string
  menuName: string
  menuCode: string
  menuType: number | string // 1-菜单, 2-按钮
  path?: string
  component?: string
  icon?: string
  sort: number
  visible: number // 0-隐藏 1-显示
  perms?: string
  children?: MenuItem[]
}

export const useMenuStore = defineStore('menu', () => {
  // 状态
  const menus = ref<MenuItem[]>([])
  const isLoaded = ref(false)

  // 计算属性：获取可见的菜单（用于侧边栏）
  const visibleMenus = computed(() => {
    return filterVisibleMenus(menus.value)
  })

  // 方法
  /**
   * 加载菜单树
   */
  async function loadMenus() {
    if (isLoaded.value && menus.value.length > 0) {
      return menus.value
    }

    try {
      const response = await getMenuTree()
      const menuData = response.data || response
      menus.value = Array.isArray(menuData) ? menuData : []
      isLoaded.value = true
      return menus.value
    } catch (error) {
      console.error('加载菜单失败:', error)
      throw error
    }
  }

  /**
   * 清除菜单（登出时调用）
   */
  function clearMenus() {
    menus.value = []
    isLoaded.value = false
  }

  /**
   * 过滤可见菜单
   */
  function filterVisibleMenus(menuList: MenuItem[]): MenuItem[] {
    return menuList
      .filter(menu => menu.visible === 1)
      .map(menu => ({
        ...menu,
        children: menu.children ? filterVisibleMenus(menu.children) : undefined,
      }))
  }

  /**
   * 将菜单树转换为路由配置
   */
  function generateRoutes(menuList: MenuItem[]): any[] {
    const routes: any[] = []

    menuList.forEach(menu => {
      if (menu.path && menu.component) {
        const route: any = {
          path: menu.path,
          name: menu.menuCode,
          meta: {
            title: menu.menuName,
            icon: menu.icon,
          },
        }

        // 如果有子菜单
        if (menu.children && menu.children.length > 0) {
          route.children = generateRoutes(menu.children)
        } else if (menu.component) {
          // 白名单校验：防止恶意路径加载任意模块
          if (!ALLOWED_COMPONENTS.has(menu.component)) {
            console.warn(`组件路径不在白名单中: ${menu.component}`)
            return
          }
          route.component = () => import(`../views${menu.component}.vue`)
        }

        routes.push(route)
      }

      // 递归处理子菜单（如果当前菜单没有 path/component，但有 children）
      if (menu.children && menu.children.length > 0 && (!menu.path || !menu.component)) {
        const childRoutes = generateRoutes(menu.children)
        routes.push(...childRoutes)
      }
    })

    return routes
  }

  return {
    menus,
    isLoaded,
    visibleMenus,
    loadMenus,
    clearMenus,
    generateRoutes,
  }
})
