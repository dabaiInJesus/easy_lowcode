import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  // 状态
  const sidebarCollapsed = ref<boolean>(false)
  const theme = ref<string>('light')
  const language = ref<string>('zh-CN')

  // 方法
  /**
   * 切换侧边栏折叠状态
   */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /**
   * 设置侧边栏状态
   */
  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed
  }

  /**
   * 设置主题
   */
  function setTheme(newTheme: string) {
    theme.value = newTheme
    localStorage.setItem('theme', newTheme)
  }

  /**
   * 设置语言
   */
  function setLanguage(lang: string) {
    language.value = lang
    localStorage.setItem('language', lang)
  }

  /**
   * 从本地存储恢复配置
   */
  function restoreConfig() {
    const savedTheme = localStorage.getItem('theme')
    const savedLanguage = localStorage.getItem('language')
    
    if (savedTheme) {
      theme.value = savedTheme
    }
    if (savedLanguage) {
      language.value = savedLanguage
    }
  }

  return {
    // 状态
    sidebarCollapsed,
    theme,
    language,
    // 方法
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    setLanguage,
    restoreConfig,
  }
})
