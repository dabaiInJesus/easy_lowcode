import { ref, shallowReactive } from 'vue'
import type { Ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

type DialogMode = 'add' | 'edit' | 'view' | null

interface UseCrudDialogOptions<T> {
  fetchList?: () => Promise<void>
  createFn?: (data: Partial<T>) => Promise<void>
  updateFn?: (data: Partial<T>) => Promise<void>
  deleteFn?: (id: string | number) => Promise<void>
  initialFormData?: Partial<T>
  successMessage?: string
}

interface UseCrudDialogReturn<T> {
  dialogVisible: Ref<boolean>
  dialogTitle: Ref<string>
  dialogMode: Ref<DialogMode>
  formData: Partial<T>
  submitting: Ref<boolean>
  openAdd: () => void
  openEdit: (row: T) => void
  openView: (row: T) => void
  closeDialog: () => void
  resetForm: () => void
  handleSubmit: () => Promise<void>
  handleDelete: (id: string | number, rowName?: string) => Promise<void>
}

export function useCrudDialog<T extends { id?: string | number }>(
  options: UseCrudDialogOptions<T> = {}
): UseCrudDialogReturn<T> {
  const {
    fetchList,
    createFn,
    updateFn,
    deleteFn,
    initialFormData = {} as Partial<T>,
    successMessage = '操作成功',
  } = options

  const dialogVisible = ref(false) as Ref<boolean>
  const dialogTitle = ref('') as Ref<string>
  const dialogMode = ref<DialogMode>(null) as Ref<DialogMode>
  const submitting = ref(false) as Ref<boolean>
  const formData = shallowReactive<Partial<T>>({ ...initialFormData })

  function openAdd(): void {
    dialogMode.value = 'add'
    dialogTitle.value = '新增'
    resetForm()
    dialogVisible.value = true
  }

  function openEdit(row: T): void {
    dialogMode.value = 'edit'
    dialogTitle.value = '编辑'
    Object.assign(formData, row)
    dialogVisible.value = true
  }

  function openView(row: T): void {
    dialogMode.value = 'view'
    dialogTitle.value = '详情'
    Object.assign(formData, row)
    dialogVisible.value = true
  }

  function closeDialog(): void {
    dialogVisible.value = false
    resetForm()
  }

  function resetForm(): void {
    const keys = Object.keys(formData) as (keyof T)[]
    keys.forEach((key) => {
      delete formData[key]
    })
    Object.assign(formData, { ...initialFormData })
  }

  async function handleSubmit(): Promise<void> {
    if (!dialogMode.value || dialogMode.value === 'view') {
      closeDialog()
      return
    }

    submitting.value = true
    try {
      if (dialogMode.value === 'add' && createFn) {
        await createFn({ ...formData })
      } else if (dialogMode.value === 'edit' && updateFn) {
        await updateFn({ ...formData })
      }
      ElMessage.success(successMessage)
      closeDialog()
      await fetchList?.()
    } finally {
      submitting.value = false
    }
  }

  async function handleDelete(id: string | number, rowName?: string): Promise<void> {
    if (!deleteFn) return

    const displayName = rowName || `ID: ${id}`
    try {
      await ElMessageBox.confirm(
        `确定删除 "${displayName}" 吗？此操作不可撤销。`,
        '确认删除',
        {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning',
        }
      )
      await deleteFn(id)
      ElMessage.success('删除成功')
      await fetchList?.()
    } catch {
      // User cancelled
    }
  }

  return {
    dialogVisible,
    dialogTitle,
    dialogMode,
    formData,
    submitting,
    openAdd,
    openEdit,
    openView,
    closeDialog,
    resetForm,
    handleSubmit,
    handleDelete,
  }
}
