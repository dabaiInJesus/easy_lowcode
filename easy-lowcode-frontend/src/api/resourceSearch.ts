import request from '@/utils/request'
import type { FieldConfig, ConfigJson } from '@/types/tableResource'
import type { PageResult } from '@/types/common'

export interface SearchParams {
  page: number
  pageSize: number
  keyword?: string
  filters?: Record<string, unknown>
  orderField?: string
  orderDirection?: 'ASC' | 'DESC'
  selectFields?: string[]
  templateName?: string
  templateParams?: Record<string, unknown>
}

export interface SearchResult {
  records: Record<string, unknown>[]
  total: number
  page: number
  pageSize: number
}

export interface ResourceFieldInfo {
  resourceCode: string
  fields: FieldConfig[]
  configJson?: ConfigJson
}

export interface TemplateParameter {
  name: string
  label: string
  type?: string
  required?: boolean
  defaultValue?: unknown
}

export interface ResourceTemplate {
  name: string
  label: string
  parameters: TemplateParameter[]
}

export function getTableResourceList(): Promise<PageResult<{ id: string; resourceCode: string; tableName: string }>> {
  return request({
    url: '/collector/table-resource/page',
    method: 'get',
    params: { current: 1, size: 200 },
  })
}

export function getResourceFields(resourceCode: string): Promise<ResourceFieldInfo> {
  return request({
    url: `/resource/search/fields/${resourceCode}`,
    method: 'get',
  })
}

export function getResourceTemplates(resourceCode: string): Promise<ResourceTemplate[]> {
  return request({
    url: `/resource/${resourceCode}/templates`,
    method: 'get',
  })
}

export function singleSearch(resourceCode: string, params: SearchParams): Promise<SearchResult> {
  return request({
    url: `/resource/search/single/${resourceCode}`,
    method: 'post',
    data: params,
  })
}

export function singleGetById(resourceCode: string, id: number): Promise<Record<string, unknown>> {
  return request({
    url: `/resource/search/single/${resourceCode}/${id}`,
    method: 'get',
  })
}
