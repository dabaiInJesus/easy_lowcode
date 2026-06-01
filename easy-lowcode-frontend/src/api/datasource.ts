import request from '@/utils/request'

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
export function getDataSourcePage(current: number, size: number, keyword?: string): Promise<any> {
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

/**
 * 扫描表列表
 */
export function scanTables(datasourceId: number): Promise<any[]> {
  return request({
    url: `/collector/datasource/${datasourceId}/tables`,
    method: 'get',
  })
}

/**
 * 获取表结构
 */
export function getTableColumns(datasourceId: number, tableName: string): Promise<any[]> {
  return request({
    url: `/collector/datasource/${datasourceId}/table/${tableName}/columns`,
    method: 'get',
  })
}
