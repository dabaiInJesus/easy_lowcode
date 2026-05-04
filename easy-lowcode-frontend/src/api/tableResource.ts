import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface TableResource {
  id?: number
  datasourceId: number
  tableName: string
  tableComment?: string
  resourceCode: string
  apiPath: string
  methods?: string
  status?: number
  configJson?: string
  datasourceName?: string
}

/**
 * 分页查询表资源列表
 */
export function getTableResourcePage(
  current: number,
  size: number,
  datasourceId?: number,
  keyword?: string
): Promise<ApiResponse<any>> {
  return request({
    url: '/collector/table-resource/page',
    method: 'get',
    params: { current, size, datasourceId, keyword },
  })
}

/**
 * 获取表资源详情
 */
export function getTableResourceById(id: number): Promise<ApiResponse<TableResource>> {
  return request({
    url: `/collector/table-resource/${id}`,
    method: 'get',
  })
}

/**
 * 注册表资源
 */
export function registerTableResource(data: Partial<TableResource>): Promise<ApiResponse<void>> {
  return request({
    url: '/collector/table-resource',
    method: 'post',
    data,
  })
}

/**
 * 更新表资源
 */
export function updateTableResource(data: Partial<TableResource>): Promise<ApiResponse<void>> {
  return request({
    url: '/collector/table-resource',
    method: 'put',
    data,
  })
}

/**
 * 删除表资源
 */
export function deleteTableResource(id: number): Promise<ApiResponse<void>> {
  return request({
    url: `/collector/table-resource/${id}`,
    method: 'delete',
  })
}

/**
 * 生成API接口
 */
export function generateApi(id: number): Promise<ApiResponse<void>> {
  return request({
    url: `/collector/table-resource/${id}/generate-api`,
    method: 'post',
  })
}
