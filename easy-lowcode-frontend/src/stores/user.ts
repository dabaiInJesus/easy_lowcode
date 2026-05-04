import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/api/auth'

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string>(localStorage.getItem('token') || '')
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
    localStorage.setItem('token', newToken)
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
    localStorage.removeItem('token')
  }

  /**
   * 从本地存储恢复 Token
   */
  function restoreToken() {
    const savedToken = localStorage.getItem('token')
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
