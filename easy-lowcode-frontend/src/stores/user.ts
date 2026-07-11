import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态：使用 sessionStorage 替代 localStorage，避免 XSS 跨标签页 token 泄露
  const token = ref<string>(sessionStorage.getItem('token') || '')
  const userInfo = ref<UserInfo | null>(null)

  // 计算属性
  const isLoggedIn = computed(() => !!token.value)
  const username = computed(() => userInfo.value?.username || '')
  const nickname = computed(() => userInfo.value?.nickname || '')
  const avatar = computed(() => userInfo.value?.avatar || '')

  // 方法
  /**
   * 设置 Token
   */
  function setToken(newToken: string) {
    token.value = newToken
    sessionStorage.setItem('token', newToken)
  }

  /**
   * 设置用户信息
   */
  function setUserInfo(info: UserInfo) {
    userInfo.value = info
  }

  /**
   * 清除用户信息（登出）
   */
  function clearUser() {
    token.value = ''
    userInfo.value = null
    sessionStorage.removeItem('token')
  }

  /**
   * 从会话存储恢复 Token
   */
  function restoreToken() {
    const savedToken = sessionStorage.getItem('token')
    if (savedToken) {
      token.value = savedToken
    }
  }

  /**
   * 登出（清除用户信息）
   */
  function logout() {
    clearUser()
  }

  return {
    // 状态
    token,
    userInfo,
    // 计算属性
    isLoggedIn,
    username,
    nickname,
    avatar,
    // 方法
    setToken,
    setUserInfo,
    clearUser,
    restoreToken,
    logout,
  }
})
