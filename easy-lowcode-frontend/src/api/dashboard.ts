import request from '@/utils/request'

export interface Dashboard {
  id?: number
  name: string
  code: string
  title?: string
  description?: string
  width?: number
  height?: number
  backgroundColor?: string
  backgroundImage?: string
  layoutConfig?: string
  styleConfig?: string
  refreshInterval?: number
  status?: number
  thumbnail?: string
  category?: string
  tags?: string
  sortOrder?: number
  isBuiltin?: number
  chartCount?: number
}

export interface DashboardChart {
  id?: number
  dashboardId: number
  title: string
  chartType: string
  datasourceId?: number
  tableResourceId?: number
  querySql?: string
  xField?: string
  yField?: string
  groupField?: string
  filterConfig?: string
  orderField?: string
  orderDirection?: string
  limitRecords?: number
  chartOption?: string
  posX?: number
  posY?: number
  width?: number
  height?: number
  refreshInterval?: number
  sortOrder?: number
  remark?: string
}

export function getDashboardPage(current: number, size: number, keyword?: string, status?: number): Promise<any> {
  return request({ url: '/dashboard/page', method: 'get', params: { current, size, keyword, status } })
}

export function getDashboardList(status?: number): Promise<Dashboard[]> {
  return request({ url: '/dashboard/list', method: 'get', params: { status } })
}

export function getDashboardById(id: number): Promise<Dashboard> {
  return request({ url: `/dashboard/${id}`, method: 'get' })
}

export function createDashboard(data: Partial<Dashboard>): Promise<void> {
  return request({ url: '/dashboard', method: 'post', data })
}

export function updateDashboard(data: Partial<Dashboard>): Promise<void> {
  return request({ url: '/dashboard', method: 'put', data })
}

export function deleteDashboard(id: number): Promise<void> {
  return request({ url: `/dashboard/${id}`, method: 'delete' })
}

export function publishDashboard(id: number): Promise<void> {
  return request({ url: `/dashboard/${id}/publish`, method: 'post' })
}

export function copyDashboard(id: number): Promise<number> {
  return request({ url: `/dashboard/${id}/copy`, method: 'post' })
}

export function offlineDashboard(id: number): Promise<void> {
  return request({ url: `/dashboard/${id}/offline`, method: 'post' })
}

export function getDashboardCharts(dashboardId: number): Promise<DashboardChart[]> {
  return request({ url: `/dashboard/${dashboardId}/charts`, method: 'get' })
}

export function addChart(data: Partial<DashboardChart>): Promise<void> {
  return request({ url: '/dashboard/chart', method: 'post', data })
}

export function updateChart(data: Partial<DashboardChart>): Promise<void> {
  return request({ url: '/dashboard/chart', method: 'put', data })
}

export function removeChart(chartId: number): Promise<void> {
  return request({ url: `/dashboard/chart/${chartId}`, method: 'delete' })
}

export function updateChartPositions(charts: Partial<DashboardChart>[]): Promise<void> {
  return request({ url: '/dashboard/charts/positions', method: 'put', data: charts })
}

export function queryChartData(chartId: number, params?: Record<string, any>): Promise<any[]> {
  return request({ url: `/dashboard/chart/${chartId}/data`, method: 'get', params })
}

export function previewDashboard(id: number): Promise<any> {
  return request({ url: `/dashboard/${id}/preview`, method: 'get' })
}

// ========== Text-to-SQL ==========

export interface TextToSqlRequest {
  datasourceId: number
  tableName: string
  question: string
  limit?: number
  execute?: boolean
}

export interface TextToSqlResponse {
  sql: string
  data: any[]
  rowCount: number
  recommendedChartType: string
  recommendedEchartsOption: string
  aiContent: string
  success: boolean
  errorMessage?: string
}

export function textToSql(data: TextToSqlRequest): Promise<TextToSqlResponse> {
  return request({ url: '/dataview/text-to-sql', method: 'post', data })
}

// ========== AI 图表推荐 ==========

export interface ChartRecommendRequest {
  data: any[]
  limit?: number
}

export interface ChartRecommendation {
  chartType: string
  echartsOption: string
  reason: string
}

export function recommendChart(data: ChartRecommendRequest): Promise<ChartRecommendation> {
  return request({ url: '/dataview/chart/recommend', method: 'post', data })
}

// ========== SQL 解释 ==========

export interface SqlExplainRequest {
  sql: string
  dialect?: string
  datasourceId?: number
}

export interface SqlExplainResponse {
  explanation: string
  suggestions: string[]
  rewrittenSql: string
  syntaxValid: boolean
  sampleData: any[]
  error?: string
}

export function explainSql(data: SqlExplainRequest): Promise<SqlExplainResponse> {
  return request({ url: '/dataview/sql/explain', method: 'post', data })
}

// ========== 数据源工具 ==========

export function testDataSource(data: { datasourceId?: number; dbType?: string; url?: string; username?: string; password?: string }): Promise<boolean> {
  return request({ url: '/dataview/datasource/test', method: 'post', data })
}

export function getDataSourceList(): Promise<any[]> {
  return request({ url: '/dataview/datasources', method: 'get' })
}

export function getTableColumns(datasourceId: number, table: string, schema?: string): Promise<any[]> {
  return request({ url: `/dataview/tables/${datasourceId}/columns`, method: 'get', params: { table, schema } })
}
