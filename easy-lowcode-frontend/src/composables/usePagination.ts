import { ref, reactive } from 'vue'
import type { Ref } from 'vue'
import type { PageResult } from '@/types/common'

interface UsePaginationOptions<T> {
  fetchFn: (params: { current: number; size: number }) => Promise<PageResult<T>>
  initialPageSize?: number
}

interface UsePaginationReturn<T> {
  loading: Ref<boolean>
  dataList: Ref<T[]>
  total: Ref<number>
  currentPage: Ref<number>
  pageSize: Ref<number>
  search: () => Promise<void>
  reset: () => void
  handleSizeChange: (size: number) => void
  handleCurrentChange: (page: number) => void
}

/**
 * Composable for pagination state and handlers
 *
 * Manages pagination state (current page, page size), loading state,
 * and provides search/reset/size-change/page-change handlers.
 *
 * @param options - Configuration object with fetch function and optional initial page size
 * @returns Reactive pagination state and handler functions
 *
 * @example
 * const { loading, dataList, total, currentPage, pageSize, search, handleSizeChange, handleCurrentChange } = usePagination({
 *   fetchFn: (params) => getTableResourcePage(params.current, params.size),
 *   initialPageSize: 20,
 * })
 */
export function usePagination<T>(options: UsePaginationOptions<T>): UsePaginationReturn<T> {
  const { fetchFn, initialPageSize = 20 } = options

  const loading = ref(false) as Ref<boolean>
  const dataList = ref<T[]>([]) as Ref<T[]>
  const total = ref(0) as Ref<number>
  const currentPage = ref(1) as Ref<number>
  const pageSize = ref(initialPageSize) as Ref<number>

  async function search(): Promise<void> {
    loading.value = true
    try {
      const result = await fetchFn({
        current: currentPage.value,
        size: pageSize.value,
      })
      dataList.value = result.records
      total.value = result.total
    } finally {
      loading.value = false
    }
  }

  function reset(): void {
    currentPage.value = 1
    pageSize.value = initialPageSize
    search()
  }

  function handleSizeChange(size: number): void {
    pageSize.value = size
    currentPage.value = 1
    search()
  }

  function handleCurrentChange(page: number): void {
    currentPage.value = page
    search()
  }

  return {
    loading,
    dataList,
    total,
    currentPage,
    pageSize,
    search,
    reset,
    handleSizeChange,
    handleCurrentChange,
  }
}

/**
 * Simple reactive pagination state without fetch logic
 * Useful when you need manual control over the fetch process
 */
export function usePaginationState(initialPageSize = 20) {
  return reactive({
    currentPage: 1,
    pageSize: initialPageSize,
    total: 0,
    loading: false,
  })
}
