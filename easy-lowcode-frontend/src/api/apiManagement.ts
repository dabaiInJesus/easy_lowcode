import request from '@/utils/request'

export interface ApiManagement {
  id?: string | number
  apiName: string
  apiPath: string
  apiMethod: string
  apiType: string
  sourceId?: number
  description?: string
  requestConfig?: string
  responseConfig?: string
  status?: number
  version?: string
  authRequired?: number
  rateLimit?: number
  sortOrder?: number
  createTime?: string
  updateTime?: string
}

/**
 * 分页查询API列表
 */
export function getApiPage(params: {
  current: number
  size: number
  apiName?: string
  apiType?: string
  status?: number
}): Promise<any> {
  return request({
    url: '/collector/api-management/page',
    method: 'get',
    params,
  })
}

/**
 * 获取API详情
 */
export function getApiById(id: number | string): Promise<ApiManagement> {
  return request({
    url: `/collector/api-management/${id}`,
    method: 'get',
  })
}

/**
 * 注册外部接口API
 */
export function registerExternalApi(data: ApiManagement): Promise<void> {
  return request({
    url: '/collector/api-management/register-external',
    method: 'post',
    data,
  })
}

/**
 * 更新API信息
 */
export function updateApi(id: number | string, data: ApiManagement): Promise<void> {
  return request({
    url: `/collector/api-management/${id}`,
    method: 'put',
    data,
  })
}

/**
 * 删除API
 */
export function deleteApi(id: number | string): Promise<void> {
  return request({
    url: `/collector/api-management/${id}`,
    method: 'delete',
  })
}

/**
 * 批量删除API
 */
export function batchDeleteApi(ids: (number | string)[]): Promise<void> {
  return request({
    url: '/collector/api-management/batch',
    method: 'delete',
    data: ids,
  })
}

/**
 * 启用/禁用API
 */
export function updateApiStatus(id: number | string, status: number): Promise<void> {
  return request({
    url: `/collector/api-management/${id}/status`,
    method: 'put',
    params: { status },
  })
}
