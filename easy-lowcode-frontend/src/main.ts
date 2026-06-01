import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import '@/style.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'

// 全局错误处理
window.addEventListener('unhandledrejection', (event) => {
  console.error('Unhandled promise rejection:', event.reason)
  // 可以在这里添加错误上报逻辑
})

window.addEventListener('error', (event) => {
  // 过滤某些无意义的错误
  if (event.message && event.message.includes('ResizeObserver')) {
    return
  }
  console.error('Global error:', event.error)
})

// 创建Vue应用
const app = createApp(App)

// 使用插件
app.use(createPinia())
app.use(router)
app.use(ElementPlus)

// 注册所有Element Plus图标为全局组件
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 全局错误边界
app.config.errorHandler = (err, instance, info) => {
  console.error('Vue error:', err)
  console.error('Component:', instance)
  console.error('Info:', info)
  
  // 对于特定的错误类型进行友好提示
  if (err instanceof Error) {
    if (err.message.includes('Failed to fetch') || err.message.includes('Network Error')) {
      ElMessage.error('网络连接失败，请检查网络后重试')
    } else if (err.message.includes('timeout')) {
      ElMessage.error('请求超时，请稍后重试')
    }
  }
}

// 挂载应用
app.mount('#app')