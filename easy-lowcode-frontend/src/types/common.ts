/**
 * Base entity fields that all backend entities inherit from BaseEntity.java
 */
export interface BaseEntity {
  id: string
  createTime: string
  updateTime: string
  createBy: string
  updateBy: string
  deleted: number
}

/**
 * Pagination result wrapper matching backend MyBatis-Plus IPage structure
 */
export interface PageResult<T = unknown> {
  records: T[]
  total: number
  current: number
  size: number
  pages?: number
}

/**
 * Standard API response wrapper
 */
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
}

/**
 * Pagination request parameters
 */
export interface PageParams {
  current: number
  size: number
  keyword?: string
}
