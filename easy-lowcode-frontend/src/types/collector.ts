/**
 * Data source configuration matching DataSourceConfig.java
 */
export interface DataSourceConfig {
  id?: string
  name: string
  code: string
  dbType: string
  url: string
  username: string
  password: string
  driverClassName?: string
  status?: number
  remark?: string
  extraConfig?: string
  createTime?: string
  updateTime?: string
}

/**
 * Table resource matching TableResource.java
 */
export interface TableResource {
  id?: string
  datasourceId: string
  tableName: string
  tableComment?: string
  resourceCode: string
  apiPath: string
  methods?: string
  status?: number
  configJson?: string
  datasourceName?: string
  createTime?: string
  updateTime?: string
}

/**
 * API management matching ApiManagement.java
 */
export interface ApiManagement {
  id?: string
  apiName: string
  apiPath: string
  apiMethod: string
  apiType: string
  sourceId?: string
  description?: string
  requestConfig?: string
  responseConfig?: string
  status?: number
  version?: string
  authRequired?: boolean
  rateLimit?: number
  sortOrder?: number
  datasourceName?: string
  tableName?: string
  createTime?: string
  updateTime?: string
}
