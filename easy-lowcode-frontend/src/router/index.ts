import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('../views/Login.vue'),
      meta: { requiresAuth: false },
    },
    {
      path: '/',
      component: () => import('../views/Layout.vue'),
      redirect: '/home',
      meta: { requiresAuth: true, title: '首页' },
      children: [
        {
          path: 'home',
          name: 'home',
          component: () => import('../views/Home.vue'),
          meta: { title: '首页' },
        },
        {
          path: 'system',
          meta: { title: '系统管理' },
          children: [
            {
              path: 'user',
              name: 'userManagement',
              component: () => import('../views/system/UserManagement.vue'),
              meta: { title: '用户管理' },
            },
            {
              path: 'role',
              name: 'roleManagement',
              component: () => import('../views/system/RoleManagement.vue'),
              meta: { title: '角色管理' },
            },
            {
              path: 'menu',
              name: 'menuManagement',
              component: () => import('../views/system/MenuManagement.vue'),
              meta: { title: '菜单管理' },
            },
            {
              path: 'dept',
              name: 'deptManagement',
              component: () => import('../views/system/DeptManagement.vue'),
              meta: { title: '部门管理' },
            },
            {
              path: 'auth',
              name: 'authManagement',
              component: () => import('../views/system/AuthManagement.vue'),
              meta: { title: '授权管理' },
            },
            {
              path: 'app',
              name: 'appManagement',
              component: () => import('../views/system/AppManagement.vue'),
              meta: { title: '应用管理' },
            },
          ],
        },
        {
          path: 'resource',
          meta: { title: '资源配置' },
          children: [
            {
              path: 'datasource',
              name: 'datasourceManagement',
              component: () => import('../views/resource/DataSourceManagement.vue'),
              meta: { title: '数据源管理' },
            },
            {
              path: 'table',
              name: 'tableResourceManagement',
              component: () => import('../views/resource/TableResourceManagement.vue'),
              meta: { title: '表资源注册' },
            },
            {
              path: 'api',
              name: 'apiManagement',
              component: () => import('../views/resource/ApiManagement.vue'),
              meta: { title: 'API管理' },
            },
          ],
        },
        {
          path: 'ai',
          meta: { title: 'AI智能' },
          children: [
            {
              path: 'chat',
              name: 'aiChat',
              component: () => import('../views/ai/ChatView.vue'),
              meta: { title: 'AI对话' },
            },
            {
              path: 'config',
              name: 'aiConfig',
              component: () => import('../views/ai/AiConfigManagement.vue'),
              meta: { title: 'AI配置' },
            },
          ],
        },
        {
          path: 'etl',
          meta: { title: 'ETL管理' },
          children: [
            {
              path: 'task',
              name: 'etlTaskManagement',
              component: () => import('../views/etl/EtlTaskManagement.vue'),
              meta: { title: 'ETL任务' },
            },
          ],
        },
        {
          path: 'dashboard',
          meta: { title: '数据大屏' },
          children: [
            {
              path: 'manage',
              name: 'dashboardManagement',
              component: () => import('../views/dashboard/DashboardManagement.vue'),
              meta: { title: '大屏管理' },
            },
          ],
        },
      ],
    },
    {
      path: '/dashboard/design/:id',
      name: 'dashboardDesigner',
      component: () => import('../views/dashboard/DashboardDesigner.vue'),
      meta: { requiresAuth: true, title: '大屏设计' },
    },
    {
      path: '/dashboard/view/:id',
      name: 'dashboardView',
      component: () => import('../views/dashboard/DashboardView.vue'),
      meta: { requiresAuth: true, title: '大屏预览' },
    },
  ],
})

// 路由守卫
router.beforeEach((to, _, next) => {
  const userStore = useUserStore()
  
  console.log('路由守卫检查:', {
    path: to.path,
    requiresAuth: to.meta.requiresAuth,
    isLoggedIn: userStore.isLoggedIn,
    token: userStore.token ? 'exists' : 'empty'
  })
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    // 需要认证但没有 token，跳转到登录页
    console.log('未登录，跳转到登录页')
    next('/login')
  } else if (to.path === '/login' && userStore.isLoggedIn) {
    // 已登录但访问登录页，跳转到首页
    console.log('已登录，跳转到首页')
    next('/')
  } else {
    console.log('允许访问')
    next()
  }
})

export default router
