/**
 * 通用工具函数
 */

/**
 * 防抖函数
 */
export function debounce<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let timer: ReturnType<typeof setTimeout> | null = null
  return function (this: any, ...args: Parameters<T>) {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn.apply(this, args)
    }, delay)
  }
}

/**
 * 节流函数
 */
export function throttle<T extends (...args: any[]) => any>(
  fn: T,
  delay: number = 300
): (...args: Parameters<T>) => void {
  let lastTime = 0
  return function (this: any, ...args: Parameters<T>) {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn.apply(this, args)
    }
  }
}

/**
 * 深拷贝
 */
export function deepClone<T>(obj: T): T {
  if (obj === null || typeof obj !== 'object') return obj
  if (Array.isArray(obj)) {
    return obj.map(item => deepClone(item)) as any
  }
  const cloned = {} as T
  for (const key in obj) {
    if (obj.hasOwnProperty(key)) {
      cloned[key] = deepClone(obj[key])
    }
  }
  return cloned
}

/**
 * 生成随机字符串
 */
export function randomString(length: number = 8): string {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789'
  let result = ''
  for (let i = 0; i < length; i++) {
    result += chars.charAt(Math.floor(Math.random() * chars.length))
  }
  return result
}

/**
 * 生成随机数字
 */
export function randomNumber(min: number, max: number): number {
  return Math.floor(Math.random() * (max - min + 1)) + min
}

/**
 * 格式化文件大小
 */
export function formatFileSize(bytes: number): string {
  if (bytes === 0) return '0 B'
  const k = 1024
  const sizes = ['B', 'KB', 'MB', 'GB', 'TB']
  const i = Math.floor(Math.log(bytes) / Math.log(k))
  return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i]
}

/**
 * 格式化日期
 */
export function formatDate(
  date: Date | string | number,
  format: string = 'YYYY-MM-DD HH:mm:ss'
): string {
  const d = new Date(date)
  const year = d.getFullYear()
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const hours = String(d.getHours()).padStart(2, '0')
  const minutes = String(d.getMinutes()).padStart(2, '0')
  const seconds = String(d.getSeconds()).padStart(2, '0')

  return format
    .replace('YYYY', String(year))
    .replace('MM', month)
    .replace('DD', day)
    .replace('HH', hours)
    .replace('mm', minutes)
    .replace('ss', seconds)
}

/**
 * 相对时间（多久以前）
 */
export function timeAgo(date: Date | string | number): string {
  const now = Date.now()
  const timestamp = new Date(date).getTime()
  const diff = now - timestamp

  const minute = 60 * 1000
  const hour = 60 * minute
  const day = 24 * hour
  const week = 7 * day
  const month = 30 * day
  const year = 365 * day

  if (diff < minute) return '刚刚'
  if (diff < hour) return Math.floor(diff / minute) + '分钟前'
  if (diff < day) return Math.floor(diff / hour) + '小时前'
  if (diff < week) return Math.floor(diff / day) + '天前'
  if (diff < month) return Math.floor(diff / week) + '周前'
  if (diff < year) return Math.floor(diff / month) + '个月前'
  return Math.floor(diff / year) + '年前'
}

/**
 * URL参数解析
 */
export function parseQuery(url: string = window.location.href): Record<string, string> {
  const query: Record<string, string> = {}
  const queryString = url.split('?')[1]
  if (!queryString) return query

  queryString.split('&').forEach(param => {
    const [key, value] = param.split('=')
    if (key) {
      query[decodeURIComponent(key)] = decodeURIComponent(value || '')
    }
  })
  return query
}

/**
 * 对象转URL参数
 */
export function stringifyQuery(params: Record<string, any>): string {
  const query = Object.keys(params)
    .filter(key => params[key] !== undefined && params[key] !== null && params[key] !== '')
    .map(key => `${encodeURIComponent(key)}=${encodeURIComponent(params[key])}`)
    .join('&')
  return query ? `?${query}` : ''
}

/**
 * 睡眠函数
 */
export function sleep(ms: number): Promise<void> {
  return new Promise(resolve => setTimeout(resolve, ms))
}

/**
 * 下载文件
 */
export function downloadFile(data: Blob | string, filename: string, mimeType: string = 'application/octet-stream') {
  const blob = typeof data === 'string' ? new Blob([data], { type: mimeType }) : data
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = filename
  document.body.appendChild(link)
  link.click()
  document.body.removeChild(link)
  URL.revokeObjectURL(url)
}

/**
 * 复制到剪贴板
 */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(text)
      return true
    }
    // 兼容旧浏览器
    const textarea = document.createElement('textarea')
    textarea.value = text
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    const success = document.execCommand('copy')
    document.body.removeChild(textarea)
    return success
  } catch {
    return false
  }
}

/**
 * 字符串脱敏
 */
export function maskString(str: string, start: number = 3, end: number = 4, mask: string = '*'): string {
  if (!str || str.length <= start + end) return str
  const startPart = str.slice(0, start)
  const endPart = str.slice(-end)
  const maskPart = mask.repeat(str.length - start - end)
  return startPart + maskPart + endPart
}

/**
 * 手机号脱敏
 */
export function maskPhone(phone: string): string {
  return maskString(phone, 3, 4)
}

/**
 * 邮箱脱敏
 */
export function maskEmail(email: string): string {
  if (!email) return ''
  const [name, domain] = email.split('@')
  if (!name || !domain) return email
  return maskString(name, 1, 1) + '@' + domain
}

/**
 * 身份证号脱敏
 */
export function maskIdCard(idCard: string): string {
  return maskString(idCard, 6, 4)
}

/**
 * 判断是否是PC端
 */
export function isPC(): boolean {
  const userAgent = navigator.userAgent.toLowerCase()
  const mobileKeywords = ['android', 'iphone', 'ipad', 'ipod', 'mobile', 'windows phone']
  return !mobileKeywords.some(keyword => userAgent.includes(keyword))
}

/**
 * 获取浏览器信息
 */
export function getBrowserInfo(): { name: string; version: string } {
  const ua = navigator.userAgent
  const match = ua.match(/(msie|firefox|chrome|opera|edge|safari)\/([\d.]+)/)
  if (match) {
    return { name: match[1], version: match[2] }
  }
  return { name: 'unknown', version: '0' }
}

/**
 * 本地存储工具类
 */
export const storage = {
  set<T>(key: string, value: T, expire?: number): void {
    const data = {
      value,
      expire: expire ? Date.now() + expire : null
    }
    localStorage.setItem(key, JSON.stringify(data))
  },

  get<T>(key: string): T | null {
    const str = localStorage.getItem(key)
    if (!str) return null

    try {
      const data = JSON.parse(str)
      if (data.expire && Date.now() > data.expire) {
        localStorage.removeItem(key)
        return null
      }
      return data.value as T
    } catch {
      return null
    }
  },

  remove(key: string): void {
    localStorage.removeItem(key)
  },

  clear(): void {
    localStorage.clear()
  },

  has(key: string): boolean {
    return localStorage.getItem(key) !== null
  }
}

/**
 * 会话存储工具类
 */
export const session = {
  set<T>(key: string, value: T): void {
    sessionStorage.setItem(key, JSON.stringify({ value }))
  },

  get<T>(key: string): T | null {
    const str = sessionStorage.getItem(key)
    if (!str) return null
    try {
      return JSON.parse(str).value as T
    } catch {
      return null
    }
  },

  remove(key: string): void {
    sessionStorage.removeItem(key)
  },

  clear(): void {
    sessionStorage.clear()
  }
}
