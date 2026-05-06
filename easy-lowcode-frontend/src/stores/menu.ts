import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMenuTree } from '@/api/auth'

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
    const result = filterVisibleMenus(menus.value)
    console.log('visibleMenus 计算结果:', result)
    return result
  })

  // 方法
  /**
   * 加载菜单树
   */
  async function loadMenus() {
    if (isLoaded.value && menus.value.length > 0) {
      console.log('菜单已缓存，直接返回:', menus.value)
      return menus.value
    }

    try {
      const response = await getMenuTree()
      console.log('从后端获取的菜单数据:', response)
      // axios 拦截器已经解包了 data，所以直接使用 response
      const menuData = response.data || response
      console.log('菜单数据:', menuData)
      menus.value = Array.isArray(menuData) ? menuData : []
      console.log('store 中的 menus:', menus.value)
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
          // 动态导入组件
          route.component = () => import(`../views${menu.component}.vue`)
        }

        routes.push(route)
      }

      // 递归处理子菜单
      if (menu.children && menu.children.length > 0) {
        const childRoutes = generateRoutes(menu.children)
        if (route.children) {
          route.children.push(...childRoutes)
        } else {
          route.children = childRoutes
        }
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
