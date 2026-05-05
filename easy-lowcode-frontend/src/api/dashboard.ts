import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

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

export function getDashboardPage(current: number, size: number, keyword?: string, status?: number): Promise<ApiResponse<any>> {
  return request({ url: '/dashboard/page', method: 'get', params: { current, size, keyword, status } })
}

export function getDashboardList(status?: number): Promise<ApiResponse<Dashboard[]>> {
  return request({ url: '/dashboard/list', method: 'get', params: { status } })
}

export function getDashboardById(id: number): Promise<ApiResponse<Dashboard>> {
  return request({ url: `/dashboard/${id}`, method: 'get' })
}

export function createDashboard(data: Partial<Dashboard>): Promise<ApiResponse<void>> {
  return request({ url: '/dashboard', method: 'post', data })
}

export function updateDashboard(data: Partial<Dashboard>): Promise<ApiResponse<void>> {
  return request({ url: '/dashboard', method: 'put', data })
}

export function deleteDashboard(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/dashboard/${id}`, method: 'delete' })
}

export function publishDashboard(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/dashboard/${id}/publish`, method: 'post' })
}

export function copyDashboard(id: number): Promise<ApiResponse<number>> {
  return request({ url: `/dashboard/${id}/copy`, method: 'post' })
}

export function offlineDashboard(id: number): Promise<ApiResponse<void>> {
  return request({ url: `/dashboard/${id}/offline`, method: 'post' })
}

export function getDashboardCharts(dashboardId: number): Promise<ApiResponse<DashboardChart[]>> {
  return request({ url: `/dashboard/${dashboardId}/charts`, method: 'get' })
}

export function addChart(data: Partial<DashboardChart>): Promise<ApiResponse<void>> {
  return request({ url: '/dashboard/chart', method: 'post', data })
}

export function updateChart(data: Partial<DashboardChart>): Promise<ApiResponse<void>> {
  return request({ url: '/dashboard/chart', method: 'put', data })
}

export function removeChart(chartId: number): Promise<ApiResponse<void>> {
  return request({ url: `/dashboard/chart/${chartId}`, method: 'delete' })
}

export function updateChartPositions(charts: Partial<DashboardChart>[]): Promise<ApiResponse<void>> {
  return request({ url: '/dashboard/charts/positions', method: 'put', data: charts })
}

export function queryChartData(chartId: number, params?: Record<string, any>): Promise<ApiResponse<any[]>> {
  return request({ url: `/dashboard/chart/${chartId}/data`, method: 'get', params })
}

export function previewDashboard(id: number): Promise<ApiResponse<any>> {
  return request({ url: `/dashboard/${id}/preview`, method: 'get' })
}
