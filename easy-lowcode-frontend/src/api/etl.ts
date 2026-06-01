import request from '@/utils/request'

export interface EtlTask {
  id?: number
  taskName: string
  taskCode: string
  sourceDatasourceId: number
  sourceTable?: string
  sourceSql?: string
  readMode?: string
  targetDatasourceId: number
  targetTable: string
  writeMode?: string
  fieldMapping?: string
  transformRules?: string
  scheduleType?: string
  cronExpression?: string
  intervalSeconds?: number
  batchSize?: number
  skipError?: number
  status?: number
  remark?: string
  sourceDatasourceName?: string
  targetDatasourceName?: string
  lastExecStatus?: string
  lastExecTime?: string
}

export function getEtlTaskPage(current: number, size: number, keyword?: string): Promise<any> {
  return request({ url: '/etl/task/page', method: 'get', params: { current, size, keyword } })
}

export function getEtlTaskById(id: number): Promise<EtlTask> {
  return request({ url: `/etl/task/${id}`, method: 'get' })
}

export function createEtlTask(data: Partial<EtlTask>): Promise<void> {
  return request({ url: '/etl/task', method: 'post', data })
}

export function updateEtlTask(data: Partial<EtlTask>): Promise<void> {
  return request({ url: '/etl/task', method: 'put', data })
}

export function deleteEtlTask(id: number): Promise<void> {
  return request({ url: `/etl/task/${id}`, method: 'delete' })
}

export function executeEtlTask(id: number): Promise<number> {
  return request({ url: `/etl/task/${id}/execute`, method: 'post' })
}

export function stopEtlTask(id: number): Promise<void> {
  return request({ url: `/etl/task/${id}/stop`, method: 'post' })
}

export function getEtlTaskHistory(id: number): Promise<any[]> {
  return request({ url: `/etl/task/${id}/history`, method: 'get' })
}

export function getEtlTaskSourceColumns(id: number): Promise<any[]> {
  return request({ url: `/etl/task/${id}/source-columns`, method: 'get' })
}

export function getEtlTaskTargetColumns(id: number): Promise<any[]> {
  return request({ url: `/etl/task/${id}/target-columns`, method: 'get' })
}

export function scanTableColumns(datasourceId: number, tableName: string): Promise<any[]> {
  return request({ url: '/etl/task/scan-columns', method: 'get', params: { datasourceId, tableName } })
}

export function previewEtlSourceData(id: number, limit = 10): Promise<any[]> {
  return request({ url: `/etl/task/${id}/preview`, method: 'get', params: { limit } })
}

export function toggleEtlTaskSchedule(id: number, enabled: boolean): Promise<void> {
  return request({ url: `/etl/task/${id}/schedule`, method: 'put', params: { enabled } })
}

export function getEtlDatasources(): Promise<any[]> {
  return request({ url: '/etl/task/datasources', method: 'get' })
}
