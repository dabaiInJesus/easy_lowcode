import request from '@/utils/request'

export interface UnifiedKeyMapping {
  id?: number
  unifiedKey: string
  displayName: string
  description?: string
  resourceCode: string
  fieldName: string
  dataType?: string
  queryType?: string
  sortOrder?: number
}

export function getDistinctKeys(): Promise<UnifiedKeyMapping[]> {
  return request({ url: '/collector/unified-key-mapping/keys', method: 'get' })
}

export function getMappingsByKey(unifiedKey: string): Promise<UnifiedKeyMapping[]> {
  return request({ url: `/collector/unified-key-mapping/keys/${unifiedKey}`, method: 'get' })
}

export function getMappingPage(current: number, size: number, unifiedKey?: string): Promise<any> {
  return request({
    url: '/collector/unified-key-mapping/page',
    method: 'get',
    params: { current, size, unifiedKey },
  })
}

export function createMapping(data: UnifiedKeyMapping): Promise<void> {
  return request({ url: '/collector/unified-key-mapping', method: 'post', data })
}

export function batchCreateMappings(data: UnifiedKeyMapping[]): Promise<void> {
  return request({ url: '/collector/unified-key-mapping/batch', method: 'post', data })
}

export function updateMapping(id: number, data: UnifiedKeyMapping): Promise<void> {
  return request({ url: `/collector/unified-key-mapping/${id}`, method: 'put', data })
}

export function deleteMapping(id: number): Promise<void> {
  return request({ url: `/collector/unified-key-mapping/${id}`, method: 'delete' })
}

export function suggestMappings(unifiedKey: string, displayName?: string): Promise<any[]> {
  return request({
    url: '/collector/unified-key-mapping/suggest',
    method: 'get',
    params: { unifiedKey, displayName },
  })
}
