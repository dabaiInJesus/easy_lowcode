import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { getMenuTree } from '@/api/auth'
import router from '@/router'

// 组件路径映射 - 后端返回的 component 字段映射到前端动态 import
const componentMap: Record<string, () => Promise<any>> = {
  // 系统管理
  'system/UserManagement': () => import('../views/system/UserManagement.vue'),
  'system/RoleManagement': () => import('../views/system/RoleManagement.vue'),
  'system/MenuManagement': () => import('../views/system/MenuManagement.vue'),
  'system/DeptManagement': () => import('../views/system/DeptManagement.vue'),
  'system/AppManagement': () => import('../views/system/AppManagement.vue'),
  'system/AuthManagement': () => import('../views/system/AuthManagement.vue'),
  'system/user/index': () => import('../views/system/UserManagement.vue'),
  'system/role/index': () => import('../views/system/RoleManagement.vue'),
  'system/menu/index': () => import('../views/system/MenuManagement.vue'),
  'system/dept/index': () => import('../views/system/DeptManagement.vue'),
  'system/app/index': () => import('../views/system/AppManagement.vue'),
  'system/auth/index': () => import('../views/system/AuthManagement.vue'),
  // 资源管理
  'resource/DataSourceManagement': () => import('../views/resource/DataSourceManagement.vue'),
  'resource/TableResourceManagement': () => import('../views/resource/TableResourceManagement.vue'),
  'resource/ApiManagement': () => import('../views/resource/ApiManagement.vue'),
  'resource/datasource/index': () => import('../views/resource/DataSourceManagement.vue'),
  'resource/table/index': () => import('../views/resource/TableResourceManagement.vue'),
  'resource/api/index': () => import('../views/resource/ApiManagement.vue'),
  // ETL
  'etl/EtlTaskManagement': () => import('../views/etl/EtlTaskManagement.vue'),
  'etl/task/index': () => import('../views/etl/EtlTaskManagement.vue'),
  // 数据大屏
  'dashboard/DashboardManagement': () => import('../views/dashboard/DashboardManagement.vue'),
  'dashboard/DashboardDesigner': () => import('../views/dashboard/DashboardDesigner.vue'),
  'dashboard/DashboardView': () => import('../views/dashboard/DashboardView.vue'),
  'dashboard/manage/index': () => import('../views/dashboard/DashboardManagement.vue'),
  // AI
  'ai/ChatView': () => import('../views/ai/ChatView.vue'),
  'ai/AiConfigManagement': () => import('../views/ai/AiConfigManagement.vue'),
  'ai/chat/index': () => import('../views/ai/ChatView.vue'),
  'ai/config/index': () => import('../views/ai/AiConfigManagement.vue'),
}

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
   * 加载菜单树并注册动态路由
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
      
      // 注册动态路由
      const dynamicRoutes = generateRoutes(menus.value)
      dynamicRoutes.forEach(route => {
        try {
          router.addRoute('main', route)
        } catch (e) {
          console.error('添加路由失败:', route, e)
        }
      })
      
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
    // 移除动态添加的路由
    menus.value.forEach(menu => {
      if (menu.menuCode && router.hasRoute(menu.menuCode)) {
        router.removeRoute(menu.menuCode)
      }
    })
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
      // 过滤掉无效的菜单（设计器和预览页面需要ID，不适合作为菜单）
      .filter(menu => {
        // 过滤掉 /dashboard/design 和 /dashboard/view
        if (menu.path === '/dashboard/design' || menu.path === '/dashboard/view') {
          return false
        }
        // 过滤子菜单中的无效项
        if (menu.children) {
          menu.children = menu.children.filter(
            child => child.path !== '/dashboard/design' && child.path !== '/dashboard/view'
          )
        }
        return true
      })
  }

  /**
   * 将菜单树转换为路由配置
   */
  function generateRoutes(menuList: MenuItem[]): any[] {
    const routes: any[] = []

    menuList.forEach(menu => {
      // 有子菜单的菜单（父菜单如"数据大屏"）
      if (menu.children && menu.children.length > 0) {
        if (menu.path && menu.component) {
          const parentRoute: any = {
            path: menu.path,
            name: menu.menuCode,
            component: () => import('../views/Layout.vue'),
            meta: { title: menu.menuName, icon: menu.icon },
            children: [] as any[],
          }

          // 处理子菜单
          menu.children.forEach(child => {
            if (child.path && child.component) {
              const loader = componentMap[child.component]
              if (!loader) {
                console.warn(`组件不存在: ${child.component}`)
                return
              }

              const childRoute: any = {
                path: child.path.split('/').pop() || child.path,
                name: child.menuCode,
                component: loader,
                meta: { title: child.menuName, icon: child.icon },
              }
              parentRoute.children.push(childRoute)
            }
          })

          routes.push(parentRoute)
        }
        return
      }

      // 没有子菜单的菜单
      if (menu.path && menu.component) {
        const loader = componentMap[menu.component]
        if (!loader) {
          console.warn(`组件不存在: ${menu.component}`)
          return
        }

        const route: any = {
          path: menu.path,
          name: menu.menuCode,
          meta: { title: menu.menuName, icon: menu.icon },
          component: loader,
        }
        routes.push(route)
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
