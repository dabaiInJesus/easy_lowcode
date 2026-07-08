import request from '@/utils/request'

export interface FulltextDocument {
  id: number
  fileName: string
  fileType: string
  fileSize: number
  storageType: string
  storagePath: string
  contentText: string
  resourceCode: string
  searchEngine: string
  indexed: number
  indexError: string
  createTime: string
  updateTime: string
}

export function uploadFile(file: File, resourceCode?: string): Promise<FulltextDocument> {
  const formData = new FormData()
  formData.append('file', file)
  if (resourceCode) formData.append('resourceCode', resourceCode)
  return request({
    url: '/collector/fulltext/upload',
    method: 'post',
    data: formData,
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000,
  })
}

export function searchFulltext(keyword: string, page: number, pageSize: number, resourceCode?: string): Promise<{
  records: Record<string, any>[]
  total: number
  page: number
  pageSize: number
}> {
  return request({
    url: '/collector/fulltext/search',
    method: 'get',
    params: { keyword, page, pageSize, resourceCode },
  })
}

export function getDocumentPage(current: number, size: number, keyword?: string): Promise<any> {
  return request({
    url: '/collector/fulltext/page',
    method: 'get',
    params: { current, size, keyword },
  })
}

export function getDocumentById(id: number): Promise<FulltextDocument> {
  return request({ url: `/collector/fulltext/${id}`, method: 'get' })
}

export function reindexDocument(id: number): Promise<void> {
  return request({ url: `/collector/fulltext/${id}/reindex`, method: 'post' })
}

export function deleteDocument(id: number): Promise<void> {
  return request({ url: `/collector/fulltext/${id}`, method: 'delete' })
}
