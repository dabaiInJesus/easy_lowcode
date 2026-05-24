import request from '@/utils/request'
import type { PageResult, PageParams } from '@/types/common'

/**
 * Generic CRUD API factory function
 *
 * Creates a set of standard CRUD operations for a given resource path.
 * All returned types are wrapped in the standard ApiResponse structure
 * which is automatically unwrapped by the request interceptor.
 *
 * @param basePath - The base URL path for the resource (e.g., '/auth/user')
 * @returns Object containing listPage, getList, getDetail, create, update, remove methods
 *
 * @example
 * const userApi = createCrudApi<User>('/auth/user')
 * userApi.listPage({ current: 1, size: 10 })
 * userApi.getDetail('123')
 * userApi.create({ username: 'test' })
 */
export function createCrudApi<T extends { id?: string }>(basePath: string) {
  return {
    /**
     * Paginated list query
     */
    listPage(params: PageParams & Record<string, unknown>): Promise<PageResult<T>> {
      return request({
        url: `${basePath}/page`,
        method: 'get',
        params,
      })
    },

    /**
     * Non-paginated list query
     */
    getList(params?: Record<string, unknown>): Promise<T[]> {
      return request({
        url: `${basePath}/list`,
        method: 'get',
        params,
      })
    },

    /**
     * Get single entity by ID
     */
    getDetail(id: string | number): Promise<T> {
      return request({
        url: `${basePath}/${id}`,
        method: 'get',
      })
    },

    /**
     * Create new entity
     */
    create(data: Partial<T>): Promise<void> {
      return request({
        url: basePath,
        method: 'post',
        data,
      })
    },

    /**
     * Update existing entity
     */
    update(data: Partial<T>): Promise<void> {
      return request({
        url: basePath,
        method: 'put',
        data,
      })
    },

    /**
     * Delete entity by ID
     */
    remove(id: string | number): Promise<void> {
      return request({
        url: `${basePath}/${id}`,
        method: 'delete',
      })
    },

    /**
     * Batch delete entities
     */
    batchRemove(ids: (string | number)[]): Promise<void> {
      return request({
        url: `${basePath}/batch`,
        method: 'delete',
        data: ids,
      })
    },

    /**
     * Update entity status
     */
    updateStatus(id: string | number, status: number): Promise<void> {
      return request({
        url: `${basePath}/${id}/status`,
        method: 'put',
        params: { status },
      })
    },
  }
}
