import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface DataSourceConfig {
  id?: number
  name: string
  code: string
  dbType: string
  dbName?: string
  url: string
  username: string
  password: string
  driverClassName?: string
  status?: number
  remark?: string
}

/**
 * 分页查询数据源列表
 */
export function getDataSourcePage(current: number, size: number, keyword?: string): Promise<PageResult<DataSourceConfig>> {
  return request({
    url: '/collector/datasource/page',
    method: 'get',
    params: { current, size, keyword },
  })
}

/**
 * 获取数据源详情
 */
export function getDataSourceById(id: number): Promise<DataSourceConfig> {
  return request({
    url: `/collector/datasource/${id}`,
    method: 'get',
  })
}

/**
 * 创建数据源
 */
export function createDataSource(data: Partial<DataSourceConfig>): Promise<void> {
  return request({
    url: '/collector/datasource',
    method: 'post',
    data,
  })
}

/**
 * 更新数据源
 */
export function updateDataSource(data: Partial<DataSourceConfig>): Promise<void> {
  return request({
    url: '/collector/datasource',
    method: 'put',
    data,
  })
}

/**
 * 删除数据源
 */
export function deleteDataSource(id: number): Promise<void> {
  return request({
    url: `/collector/datasource/${id}`,
    method: 'delete',
  })
}

/**
 * 测试连接
 */
export function testConnection(data: Partial<DataSourceConfig>): Promise<boolean> {
  return request({
    url: '/collector/datasource/test-connection',
    method: 'post',
    data,
  })
}

export interface TableInfo {
  name: string
  tableName?: string
  type: string
  comment?: string
}

export interface ColumnInfo {
  name: string
  type: string
  nullable?: boolean
  comment?: string
  primaryKey?: boolean
}

/**
 * 扫描表列表
 */
export function scanTables(datasourceId: number): Promise<TableInfo[]> {
  return request({
    url: `/collector/datasource/${datasourceId}/tables`,
    method: 'get',
  })
}

/**
 * 获取表结构
 */
export function getTableColumns(datasourceId: number, tableName: string): Promise<ColumnInfo[]> {
  return request({
    url: `/collector/datasource/${datasourceId}/table/${tableName}/columns`,
    method: 'get',
  })
}

/**
 * 获取所有活跃数据源（用于下拉选择器）
 */
export function getActiveDataSources(): Promise<DataSourceConfig[]> {
  return request<{ records: DataSourceConfig[] }>({
    url: '/collector/datasource/page',
    method: 'get',
    params: { current: 1, size: 200 },
  }).then(res => res.records || [])
}
