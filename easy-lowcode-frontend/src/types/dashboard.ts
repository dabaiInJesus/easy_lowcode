/**
 * Dashboard entity matching Dashboard.java
 */
export interface Dashboard {
  id?: string
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
  createTime?: string
  updateTime?: string
}

/**
 * Dashboard chart entity matching DashboardChart.java
 */
export interface DashboardChart {
  id?: string
  dashboardId: string
  title: string
  chartType: string
  datasourceId?: string
  tableResourceId?: string
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
  createTime?: string
  updateTime?: string
}
