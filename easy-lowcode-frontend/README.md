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

开发环境下，API 请求会代理到后端服务：

```typescript
// vite.config.ts
server: {
  port: 3000,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',
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
import axios from 'axios'

// GET 请求
const response = await axios.get('/api/ai/agent/page')

// POST 请求
const response = await axios.post('/api/ai/agent', {
  agentName: '客服助手',
  agentCode: 'customer_service'
})
```

## 核心功能页面（待实现）

### 1. 登录页面 `/login`
- 用户名/密码登录
- Token 存储

### 2. 主布局 `/`
- 侧边栏导航
- 顶部栏
- 内容区域

### 3. 智能体管理 `/agents`
- 智能体列表（分页、搜索）
- 创建/编辑智能体
- 发布/下架智能体
- 删除智能体

### 4. AI 聊天 `/chat/:agentCode`
- 与智能体对话
- 消息历史
- 参数调整

### 5. 提示词模板 `/prompts`
- 模板列表
- 创建/编辑模板
- 模板预览

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

1. **确保后端服务已启动**：前端需要连接后端 API（默认 http://localhost:8080）
2. **跨域配置**：开发环境通过 Vite 代理解决，生产环境需配置 Nginx 反向代理
3. **环境变量**：可以创建 `.env` 文件配置不同环境的 API 地址

## 后续开发建议

1. **完善路由守卫**：添加登录验证
2. **状态管理**：使用 Pinia 管理用户信息、Token 等
3. **API 封装**：统一封装 Axios 请求和响应拦截器
4. **组件库**：抽取公共组件（如智能体卡片、聊天窗口等）
5. **工作流编辑器**：集成 Vue Flow 或 LogicFlow 实现可视化工作流设计
6. **国际化**：支持多语言切换

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
        proxy_pass http://localhost:8080;
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

**注意**：由于篇幅限制，这里只提供了前端项目的基础结构和配置。完整的页面实现需要根据实际需求继续开发。

您可以：
1. 运行 `npm install` 安装依赖
2. 运行 `npm run dev` 启动开发服务器
3. 根据上述示例代码逐步实现各个功能页面
4. 参考 Element Plus 和 TailwindCSS 文档进行 UI 开发

祝开发顺利！🚀
