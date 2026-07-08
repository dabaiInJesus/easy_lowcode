import request from '@/utils/request'
import type { FieldConfig, ConfigJson } from '@/types/tableResource'

export interface SearchParams {
  page: number
  pageSize: number
  keyword?: string
  filters?: Record<string, any>
  orderField?: string
  orderDirection?: 'ASC' | 'DESC'
  selectFields?: string[]
  templateName?: string
  templateParams?: Record<string, any>
}

export interface SearchResult {
  records: Record<string, any>[]
  total: number
  page: number
  pageSize: number
}

export interface ResourceFieldInfo {
  resourceCode: string
  fields: FieldConfig[]
  configJson?: ConfigJson
}

export function getTableResourceList(): Promise<{ records: any[] }> {
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

export function getResourceTemplates(resourceCode: string): Promise<{ name: string; label: string; parameters: any[] }[]> {
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

export function singleGetById(resourceCode: string, id: number): Promise<Record<string, any>> {
  return request({
    url: `/resource/search/single/${resourceCode}/${id}`,
    method: 'get',
  })
}
