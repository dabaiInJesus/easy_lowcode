import { ref } from 'vue'
import { ElMessage } from 'element-plus'

/**
 * 消息通知 Hook
 */
export function useNotification() {
  const success = (message: string) => {
    ElMessage.success(message)
  }

  const error = (message: string) => {
    ElMessage.error(message)
  }

  const warning = (message: string) => {
    ElMessage.warning(message)
  }

  const info = (message: string) => {
    ElMessage.info(message)
  }

  return {
    success,
    error,
    warning,
    info
  }
}

/**
 * 确认对话框 Hook
 */
export function useConfirm() {
  const confirm = async (
    message: string,
    title = '提示'
  ): Promise<boolean> => {
    const { ElMessageBox } = await import('element-plus')
    try {
      await ElMessageBox.confirm(message, title, {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning'
      })
      return true
    } catch {
      return false
    }
  }

  const alert = async (message: string, title = '提示'): Promise<void> => {
    const { ElMessageBox } = await import('element-plus')
    await ElMessageBox.alert(message, title, {
      confirmButtonText: '确定'
    })
  }

  return {
    confirm,
    alert
  }
}

/**
 * 文件下载 Hook
 */
export function useDownload() {
  const download = async (
    url: string,
    filename?: string,
    method = 'get'
  ) => {
    const { default: axios } = await import('axios')
    const response = await axios({
      url,
      method,
      responseType: 'blob'
    })

    const blob = new Blob([response.data])
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = filename || getFilenameFromContentDisposition(response.headers['content-disposition'])
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(link.href)
  }

  return {
    download
  }
}

function getFilenameFromContentDisposition(contentDisposition: string): string {
  if (!contentDisposition) return '下载文件'
  const match = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/)
  if (match) {
    return decodeURIComponent(match[1].replace(/['"]/g, ''))
  }
  return '下载文件'
}

/**
 * 防抖 Hook
 */
export function useDebounce<T extends (...args: any[]) => any>(
  fn: T,
  delay = 300
): T {
  let timer: ReturnType<typeof setTimeout> | null = null

  const debouncedFn = ((...args: Parameters<T>) => {
    if (timer) clearTimeout(timer)
    timer = setTimeout(() => {
      fn(...args)
    }, delay)
  }) as T

  return debouncedFn
}

/**
 * 节流 Hook
 */
export function useThrottle<T extends (...args: any[]) => any>(
  fn: T,
  delay = 300
): T {
  let lastTime = 0

  const throttledFn = ((...args: Parameters<T>) => {
    const now = Date.now()
    if (now - lastTime >= delay) {
      lastTime = now
      fn(...args)
    }
  }) as T

  return throttledFn
}