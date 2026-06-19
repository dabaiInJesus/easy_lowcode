# Easy Lowcode 前端项目

基于 Vue 3 + Vite + TypeScript + Element Plus + TailwindCSS 的低代码平台前端。

## 技术栈

- **Vue 3.4+** - 渐进式 JavaScript 框架
- **Vite 5.x** - 下一代前端构建工具
- **TypeScript 5.x** - JavaScript 的超集
- **Element Plus** - Vue 3 组件库
- **TailwindCSS 3.4+** - 原子化 CSS 框架
- **Pinia** - Vue 状态管理
- **Vue Router 4.x** - Vue 官方路由
- **Axios** - HTTP 客户端

## 项目结构

```
easy-lowcode-frontend/
├── src/
│   ├── api/              # API 接口
│   ├── assets/           # 静态资源
│   ├── components/       # 公共组件
│   ├── router/           # 路由配置
│   ├── stores/           # Pinia 状态管理
│   ├── views/            # 页面组件
│   ├── App.vue           # 根组件
│   ├── main.ts           # 入口文件
│   └── style.css         # 全局样式
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tailwind.config.js
└── postcss.config.js
```

## 快速开始

### 1. 安装依赖

```bash
cd easy-lowcode-frontend
npm install
```

### 2. 启动开发服务器

```bash
npm run dev
```

访问 http://localhost:3000

### 3. 构建生产版本

```bash
npm run build
```

构建产物在 `dist/` 目录

### 4. 预览生产构建

```bash
npm run preview
```

## 开发指南

### API 代理配置

开发环境下,API 请求会代理到后端服务:

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8081',
      changeOrigin: true
    }
  }
}
```

### 添加新页面

1. 在 `src/views/` 创建页面组件
2. 在 `src/router/index.ts` 添加路由
3. 在侧边栏菜单中添加导航项

### 调用 API

```typescript
import request from '@/utils/request'

// GET 请求
const res = await request({
  url: '/auth/user/page',
  method: 'get',
  params: { current: 1, size: 10 }
})

// POST 请求
const res = await request({
  url: '/auth/user',
  method: 'post',
  data: {
    username: 'test',
    password: '123456'
  }
})
```

## 用户认证

### 登录功能

项目已实现完整的用户认证流程，包括登录、登出、Token 管理和路由守卫。

#### 默认账号

- 用户名: `admin`
- 密码: `123456`

#### 认证流程

```
1. 用户访问系统
   ↓
2. 路由守卫检查 Token
   ↓
3. 无 Token → 跳转到登录页
   ↓
4. 用户输入账号密码
   ↓
5. 调用后端 /api/auth/login 接口
   ↓
6. 后端验证成功，返回 Token
   ↓
7. 前端保存 Token 到 localStorage
   ↓
8. 跳转到首页
   ↓
9. 后续请求自动携带 Token
```

#### API 接口

**登录接口**
- URL: `POST /api/auth/login`
- 参数: 
  ```json
  {
    "username": "admin",
    "password": "123456"
  }
  ```
- 响应:
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "xxx-xxx-xxx"
    }
  }
  ```

**获取用户信息**
- URL: `GET /api/auth/current`
- Headers: `Authorization: Bearer {token}`

**退出登录**
- URL: `POST /api/auth/logout`
- Headers: `Authorization: Bearer {token}`

#### 安全特性

- ✅ Token 存储在 localStorage
- ✅ 请求自动携带 Authorization Header
- ✅ 401 自动跳转登录页
- ✅ BCrypt 密码加密（后端）
- ✅ JWT 会话管理（后端）

## 状态管理（Pinia）

本项目使用 Pinia 作为 Vue 3 的状态管理解决方案。Pinia 是 Vue 官方推荐的状态管理库，具有更好的 TypeScript 支持和更简洁的 API。

### Store 结构

#### 1. 用户状态 Store (`stores/user.ts`)

管理用户认证和相关信息。

**状态**
- `token`: 用户认证令牌
- `userInfo`: 用户详细信息

**计算属性**
- `isLoggedIn`: 是否已登录
- `username`: 用户名
- `nickname`: 用户昵称
- `avatar`: 用户头像

**方法**
- `setToken(token)`: 设置 Token
- `setUserInfo(info)`: 设置用户信息
- `clearUser()`: 清除用户信息（登出）
- `restoreToken()`: 从本地存储恢复 Token

**使用示例**

```typescript
import { useUserStore } from '@/stores'

const userStore = useUserStore()

// 登录
userStore.setToken(token)
userStore.setUserInfo(userInfo)

// 检查登录状态
if (userStore.isLoggedIn) {
  console.log('已登录')
}

// 获取用户信息
console.log(userStore.username)
console.log(userStore.nickname)

// 登出
userStore.clearUser()
```

#### 2. 应用配置 Store (`stores/app.ts`)

管理应用全局配置。

**状态**
- `sidebarCollapsed`: 侧边栏折叠状态
- `theme`: 主题（light/dark）
- `language`: 语言（zh-CN/en-US）

**方法**
- `toggleSidebar()`: 切换侧边栏状态
- `setTheme(theme)`: 设置主题
- `setLanguage(lang)`: 设置语言

**使用示例**

```typescript
import { useAppStore } from '@/stores'

const appStore = useAppStore()

// 切换侧边栏
appStore.toggleSidebar()

// 设置主题
appStore.setTheme('dark')

// 设置语言
appStore.setLanguage('en-US')
```

### 在组件中使用

```vue
<script setup lang="ts">
import { useUserStore } from '@/stores'

const userStore = useUserStore()

// 直接使用 state
console.log(userStore.token)
console.log(userStore.userInfo)

// 使用计算属性
console.log(userStore.isLoggedIn)

// 调用方法
userStore.setToken('new-token')
</script>

<template>
  <div>
    <p>用户名: {{ userStore.username }}</p>
    <p>昵称: {{ userStore.nickname }}</p>
    <button @click="userStore.clearUser()">退出</button>
  </div>
</template>
```

### 最佳实践

1. **Store 命名规范**
   - 文件名：小写，如 `user.ts`、`app.ts`
   - Store 名称：useXxxStore，如 `useUserStore`

2. **状态持久化**
   - Token 等重要数据同时存储在 localStorage
   - Store 初始化时从 localStorage 恢复数据

3. **类型安全**
   - 使用 TypeScript 定义状态类型
   - 为所有方法和参数添加类型注解

4. **避免直接修改 State**
   - 始终通过 actions/methods 修改状态
   - 保持状态变更的可追踪性

5. **解构会失去响应性**
   ```typescript
   // ❌ 错误 - 失去响应性
   const { token } = userStore
   
   // ✅ 正确 - 保持响应性
   import { storeToRefs } from 'pinia'
   const { token } = storeToRefs(userStore)
   ```

### 与后端对接

**登录流程**

```typescript
// 1. 调用登录接口
const res = await login({ username, password })

// 2. 保存 token 到 store
userStore.setToken(res.token)

// 3. 获取用户信息
const userInfo = await getCurrentUser()
userStore.setUserInfo(userInfo)
```

**请求拦截器**

```typescript
// request.ts 中自动从 store 获取 token
service.interceptors.request.use((config) => {
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})
```

**响应拦截器**

```typescript
// 401 时自动清除用户信息
if (res.code === 401) {
  const userStore = useUserStore()
  userStore.clearUser()
  window.location.href = '/login'
}
```

### Pinia 优势

✅ **TypeScript 支持**: 完整的类型推断  
✅ **轻量级**: 体积小巧（~1KB）  
✅ **模块化**: 清晰的模块划分  
✅ **DevTools**: 支持 Vue DevTools  
✅ **SSR 友好**: 支持服务端渲染  
✅ **无 mutations**: 直接修改 state，更简洁

## 核心功能页面

### 1. 登录页面 `/login`
- 用户名/密码登录
- Token 存储
- 表单验证

### 2. 主布局 `/`
- 侧边栏导航
- 顶部栏
- 内容区域

### 3. 首页 `/home`
- 系统统计数据展示
- 快捷入口

### 4. 系统管理
- **用户管理** `/system/user` - 用户列表、新增、编辑、删除
- **角色管理** `/system/role` - 角色列表、权限配置
- **菜单管理** `/system/menu` - 菜单树形结构、菜单配置
- **部门管理** `/system/dept` - 部门树形结构、部门配置

### 5. 资源管理
- **数据源配置** `/resource/datasource` - 数据库连接配置、测试连接
- **表资源注册** `/resource/table` - 将数据库表注册为 API 接口

## 示例代码

### 智能体列表页面

```vue
<template>
  <div class="p-4">
    <el-card>
      <template #header>
        <div class="flex justify-between items-center">
          <span>智能体管理</span>
          <el-button type="primary" @click="handleCreate">新建智能体</el-button>
        </div>
      </template>
      
      <el-table :data="agents" v-loading="loading">
        <el-table-column prop="agentName" label="名称" />
        <el-table-column prop="agentCode" label="编码" />
        <el-table-column prop="provider" label="提供商" />
        <el-table-column prop="status" label="状态">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="success" @click="handlePublish(row)">发布</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      
      <el-pagination
        v-model:current-page="currentPage"
        v-model:page-size="pageSize"
        :total="total"
        @current-change="fetchAgents"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import axios from 'axios'

const agents = ref([])
const loading = ref(false)
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const fetchAgents = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/ai/agent/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value
      }
    })
    agents.value = data.data.records
    total.value = data.data.total
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  // 打开创建对话框
}

const handleEdit = (row) => {
  // 打开编辑对话框
}

const handlePublish = async (row) => {
  await axios.post(`/api/ai/agent/${row.id}/publish`)
  ElMessage.success('发布成功')
  fetchAgents()
}

const handleDelete = async (row) => {
  await ElMessageBox.confirm('确定删除吗？')
  await axios.delete(`/api/ai/agent/${row.id}`)
  ElMessage.success('删除成功')
  fetchAgents()
}

onMounted(() => {
  fetchAgents()
})
</script>
```

## 注意事项

1. **确保后端服务已启动**：前端需要连接后端 API（默认 http://localhost:8081）
2. **跨域配置**：开发环境通过 Vite 代理解决，生产环境需配置 Nginx 反向代理
3. **环境变量**：可以创建 `.env` 文件配置不同环境的 API 地址
4. **端口占用**：如果 3000 端口被占用，Vite 会自动切换到 3001、3002 等端口
5. **浏览器缓存**：更新后建议强制刷新（Ctrl + F5）以清除缓存

## 后续开发建议

1. **完善路由守卫**：添加更细粒度的权限控制
2. **组件库**：抽取公共组件（如表格、表单、对话框等）
3. **工作流编辑器**：集成 Vue Flow 或 LogicFlow 实现可视化工作流设计
4. **国际化**：支持多语言切换
5. **主题定制**：支持动态主题切换
6. **性能优化**：路由懒加载、组件按需引入

## 部署

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;
    
    root /usr/share/nginx/html;
    index index.html;
    
    location / {
        try_files $uri $uri/ /index.html;
    }
    
    location /api {
        proxy_pass http://localhost:8081;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

## 开发资源

- [Vue 3 文档](https://cn.vuejs.org/)
- [Vite 文档](https://cn.vitejs.dev/)
- [Element Plus 文档](https://element-plus.org/zh-CN/)
- [TailwindCSS 文档](https://tailwindcss.com/docs)
- [TypeScript 文档](https://www.typescriptlang.org/zh/)

---

**注意**：本项目已完成基础框架搭建和核心功能实现，包括用户认证、状态管理、系统管理等功能。您可以根据实际需求继续扩展其他业务模块。

您可以：
1. 运行 `npm install` 安装依赖
2. 运行 `npm run dev` 启动开发服务器
3. 参考上述文档了解项目架构和使用方法
4. 根据业务需求继续开发新功能页面

祝开发顺利！🚀
