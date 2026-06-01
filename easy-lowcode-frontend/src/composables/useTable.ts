import { ref, reactive, computed } from 'vue'
import type { TableColumnCtx } from 'element-plus'

/**
 * 表格分页 Hook
 * 适用于大多数列表页面的分页需求
 */
export function usePagination<T = any>(options?: {
  defaultSize?: number
  defaultCurrent?: number
}) {
  const pagination = reactive({
    current: options?.defaultCurrent || 1,
    size: options?.defaultSize || 10,
    total: 0
  })

  const loading = ref(false)

  const handleSizeChange = (val: number) => {
    pagination.size = val
    pagination.current = 1
  }

  const handleCurrentChange = (val: number) => {
    pagination.current = val
  }

  const resetPagination = () => {
    pagination.current = 1
    pagination.total = 0
  }

  const setTotal = (total: number) => {
    pagination.total = total
  }

  return {
    pagination,
    loading,
    handleSizeChange,
    handleCurrentChange,
    resetPagination,
    setTotal
  }
}

/**
 * 表格选择 Hook
 * 用于多选表格
 */
export function useTableSelection<T = any>() {
  const selectedRows = ref<T[]>([])
  const selectedIds = computed(() => selectedRows.value.map(r => (r as any).id))

  const handleSelectionChange = (rows: T[]) => {
    selectedRows.value = rows
  }

  const clearSelection = () => {
    selectedRows.value = []
  }

  const isSelected = (row: T) => {
    return selectedIds.value.includes((row as any).id)
  }

  return {
    selectedRows,
    selectedIds,
    handleSelectionChange,
    clearSelection,
    isSelected
  }
}

/**
 * 搜索表单 Hook
 */
export function useSearchForm<F extends Record<string, any>>(initialForm?: F) {
  const form = reactive<F>((initialForm || {}) as F)
  const searchForm = reactive<F>({} as F)

  const handleSearch = (searchFn: (form: F) => void) => {
    Object.assign(searchForm, form)
    searchFn(searchForm)
  }

  const handleReset = (resetFn?: () => void) => {
    Object.keys(form).forEach(key => {
      (form as any)[key] = initialForm ? (initialForm as any)[key] : ''
    })
    Object.assign(searchForm, form)
    resetFn?.()
  }

  return {
    form,
    searchForm,
    handleSearch,
    handleReset
  }
}

/**
 * 弹窗表单 Hook
 */
export function useDialogForm<T extends Record<string, any>>(initialData?: Partial<T>) {
  const dialogVisible = ref(false)
  const isEdit = ref(false)
  const formData = reactive<Partial<T>>(initialFormData(initialData))

  const openCreate = (defaultData?: Partial<T>) => {
    isEdit.value = false
    Object.keys(formData).forEach(key => {
      (formData as any)[key] = undefined
    })
    if (defaultData) {
      Object.assign(formData, defaultData)
    }
    dialogVisible.value = true
  }

  const openEdit = (row: T) => {
    isEdit.value = true
    Object.assign(formData, row)
    dialogVisible.value = true
  }

  const closeDialog = () => {
    dialogVisible.value = false
  }

  return {
    dialogVisible,
    isEdit,
    formData,
    openCreate,
    openEdit,
    closeDialog
  }
}

function initialFormData<T extends Record<string, any>>(initialData?: Partial<T>): Partial<T> {
  if (!initialData) return {} as Partial<T>
  const result: Partial<T> = {}
  for (const key in initialData) {
    result[key] = initialData[key]
  }
  return result
}

/**
 * 确认删除 Hook
 */
export function useConfirmDelete(deleteFn: (id: number) => Promise<void>) {
  const deleteLoading = ref<number | null>(null)

  const handleDelete = async (row: { id: number; name?: string }, nameField = 'name') => {
    const { ElMessageBox, ElMessage } = await import('element-plus')
    
    try {
      await ElMessageBox.confirm(
        `确定删除 ${(row as any)[nameField] || '此项'} 吗？`,
        '提示',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        }
      )
      
      deleteLoading.value = row.id
      await deleteFn(row.id)
      ElMessage.success('删除成功')
    } catch (error: any) {
      if (error !== 'cancel') {
        ElMessage.error(error.message || '删除失败')
      }
    } finally {
      deleteLoading.value = null
    }
  }

  return {
    deleteLoading,
    handleDelete
  }
}

/**
 * 加载状态 Hook
 */
export function useLoading() {
  const loading = ref(false)
  const error = ref<string | null>(null)

  const wrap = async <T>(fn: () => Promise<T>): Promise<T | undefined> => {
    loading.value = true
    error.value = null
    try {
      return await fn()
    } catch (e: any) {
      error.value = e.message || '操作失败'
      return undefined
    } finally {
      loading.value = false
    }
  }

  return {
    loading,
    error,
    wrap
  }
}