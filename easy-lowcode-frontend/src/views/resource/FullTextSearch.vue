<template>
  <div class="fulltext-search">
    <el-card>
      <template #header>
        <span class="card-title">全文检索</span>
      </template>

      <el-tabs v-model="activeTab" type="border-card">
        <!-- Tab 1: 文档搜索 -->
        <el-tab-pane label="文档搜索" name="search">
          <el-form :inline="true" @submit.prevent="handleSearch">
            <el-form-item label="关键词">
              <el-input v-model="searchForm.keyword" placeholder="输入搜索关键词" clearable style="width:300px" @keyup.enter="handleSearch" />
            </el-form-item>
            <el-form-item label="资源">
              <el-select v-model="searchForm.resourceCode" clearable placeholder="全部资源" filterable style="width:200px">
                <el-option v-for="r in resourceList" :key="r.resourceCode" :label="`${r.tableComment || r.tableName} (${r.resourceCode})`" :value="r.resourceCode" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="handleSearch" :loading="searchLoading">检索</el-button>
            </el-form-item>
          </el-form>

          <el-empty v-if="!searchResult.records?.length && !searchLoading" description="输入关键词开始搜索" />
          <template v-else>
            <div v-for="doc in searchResult.records" :key="doc.id" class="search-result-item">
              <div class="doc-header">
                <el-tag :type="getFileTypeTag(doc.fileType)" size="small">{{ getFileTypeLabel(doc.fileType) }}</el-tag>
                <span class="doc-name">{{ doc.fileName }}</span>
                <span class="doc-size">{{ formatSize(doc.fileSize) }}</span>
                <el-button v-if="doc.fileUrl" text type="primary" size="small" @click="downloadFile(doc)">下载</el-button>
              </div>
              <div class="doc-snippet" v-html="highlightKeyword(doc.snippet || doc.contentText || '')" />
              <div class="doc-meta">
                <span v-if="doc.resourceCode">关联资源: {{ doc.resourceCode }}</span>
                <span v-if="doc.fileType">类型: {{ doc.fileType }}</span>
              </div>
            </div>

            <el-pagination
              v-if="searchResult.total > 0"
              v-model:current-page="searchForm.page"
              v-model:page-size="searchForm.pageSize"
              :total="searchResult.total"
              :page-sizes="[10, 20, 50]"
              layout="total, sizes, prev, pager, next, jumper"
              style="margin-top:16px;justify-content:flex-end"
              @size-change="handleSearch"
              @current-change="handleSearch"
            />
          </template>
        </el-tab-pane>

        <!-- Tab 2: 上传文档 -->
        <el-tab-pane label="上传文档" name="upload">
          <el-upload
            drag
            multiple
            :auto-upload="false"
            :file-list="uploadFiles"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            accept=".pdf,.doc,.docx,.xls,.xlsx,.ppt,.pptx,.txt,.md,.csv,.json,.xml"
            style="margin-bottom:16px"
          >
            <el-icon class="el-icon--upload" :size="48"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处，或<em>点击选择文件</em></div>
            <template #tip>
              <div class="el-upload__tip">
                支持 PDF、Word、Excel、PPT、TXT、Markdown、CSV、JSON、XML 格式
              </div>
            </template>
          </el-upload>

          <el-form :inline="true">
            <el-form-item label="关联资源">
              <el-select v-model="uploadResourceCode" clearable placeholder="不关联" filterable style="width:200px">
                <el-option v-for="r in resourceList" :key="r.resourceCode" :label="`${r.tableComment || r.tableName} (${r.resourceCode})`" :value="r.resourceCode" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="startUpload" :loading="uploading" :disabled="uploadFiles.length === 0">
                开始上传 ({{ uploadFiles.length }})
              </el-button>
            </el-form-item>
          </el-form>

          <el-table v-if="uploadResults.length > 0" :data="uploadResults" border stripe size="small" max-height="300">
            <el-table-column prop="fileName" label="文件名" min-width="200" />
            <el-table-column prop="status" label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="row.success ? 'success' : 'danger'">{{ row.success ? '成功' : '失败' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="message" label="消息" min-width="200" />
          </el-table>
        </el-tab-pane>

        <!-- Tab 3: 文档管理 -->
        <el-tab-pane label="文档管理" name="manage">
          <el-form :inline="true">
            <el-form-item label="文件名">
              <el-input v-model="manageForm.keyword" placeholder="搜索文件名" clearable style="width:240px" @keyup.enter="loadDocuments" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="loadDocuments">查询</el-button>
            </el-form-item>
          </el-form>

          <el-table :data="documents.records" border stripe v-loading="docLoading" max-height="500">
            <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
            <el-table-column prop="fileType" label="类型" width="100">
              <template #default="{ row }">
                <el-tag :type="getFileTypeTag(row.fileType)" size="small">{{ getFileTypeLabel(row.fileType) }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="fileSize" label="大小" width="100">
              <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
            </el-table-column>
            <el-table-column prop="indexed" label="索引状态" width="120">
              <template #default="{ row }">
                <el-tag :type="row.indexed === 1 ? 'success' : row.indexed === 2 ? 'danger' : 'warning'">
                  {{ row.indexed === 1 ? '已索引' : row.indexed === 2 ? '失败' : '待索引' }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="indexError" label="错误信息" min-width="200" show-overflow-tooltip />
            <el-table-column prop="createTime" label="上传时间" width="160" />
            <el-table-column label="操作" width="160" fixed="right">
              <template #default="{ row }">
                <el-button size="small" type="success" @click="reindexDoc(row.id)" :disabled="row.indexed === 1">重新索引</el-button>
                <el-button size="small" type="danger" @click="deleteDoc(row.id)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>

          <el-pagination
            v-if="documents.total > 0"
            v-model:current-page="manageForm.page"
            v-model:page-size="manageForm.pageSize"
            :total="documents.total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            style="margin-top:16px;justify-content:flex-end"
            @size-change="loadDocuments"
            @current-change="loadDocuments"
          />
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import { searchFulltext, uploadFile, getDocumentPage, reindexDocument, deleteDocument } from '@/api/fulltext'
import request from '@/utils/request'

const activeTab = ref('search')

// Search tab
const searchLoading = ref(false)
const searchForm = reactive({ keyword: '', page: 1, pageSize: 20, resourceCode: '' })
const searchResult = ref<{ records: any[]; total: number }>({ records: [], total: 0 })

// Upload tab
const uploadFiles = ref<any[]>([])
const uploadResourceCode = ref('')
const uploading = ref(false)
const uploadResults = ref<{ fileName: string; success: boolean; message: string }[]>([])

// Manage tab
const docLoading = ref(false)
const manageForm = reactive({ keyword: '', page: 1, pageSize: 20 })
const documents = ref<{ records: any[]; total: number }>({ records: [], total: 0 })

// Shared
const resourceList = ref<any[]>([])

async function loadResources() {
  try {
    const data = await request({ url: '/collector/table-resource/page', method: 'get', params: { current: 1, size: 200 } }) as any
    resourceList.value = data?.records || []
  } catch { resourceList.value = [] }
}

async function handleSearch() {
  if (!searchForm.keyword) { ElMessage.warning('请输入搜索关键词'); return }
  searchLoading.value = true
  try {
    const data = await searchFulltext(searchForm.keyword, searchForm.page, searchForm.pageSize, searchForm.resourceCode || undefined)
    searchResult.value = data || { records: [], total: 0 }
  } catch (e: any) {
    ElMessage.error(e.message || '搜索失败')
    searchResult.value = { records: [], total: 0 }
  } finally { searchLoading.value = false }
}

function onFileChange(uploadFile: any) {
  uploadFiles.value = [...uploadFiles.value, uploadFile]
}

function onFileRemove(uploadFile: any) {
  uploadFiles.value = uploadFiles.value.filter(f => f.uid !== uploadFile.uid)
}

async function startUpload() {
  if (uploadFiles.value.length === 0) return
  uploading.value = true
  uploadResults.value = []
  for (const f of uploadFiles.value) {
    try {
      await uploadFile(f.raw, uploadResourceCode.value || undefined)
      uploadResults.value.push({ fileName: f.name, success: true, message: '上传并索引成功' })
    } catch (e: any) {
      uploadResults.value.push({ fileName: f.name, success: false, message: e.message || '上传失败' })
    }
  }
  uploading.value = false
  uploadFiles.value = []
  ElMessage.success(`上传完成，共 ${uploadResults.value.length} 个文件`)
}

async function loadDocuments() {
  docLoading.value = true
  try {
    const data = await getDocumentPage(manageForm.page, manageForm.pageSize, manageForm.keyword || undefined)
    documents.value = data || { records: [], total: 0 }
  } catch { documents.value = { records: [], total: 0 } }
  finally { docLoading.value = false }
}

async function reindexDoc(id: number) {
  try {
    await reindexDocument(id)
    ElMessage.success('重新索引成功')
    loadDocuments()
  } catch (e: any) {
    ElMessage.error(e.message || '重新索引失败')
  }
}

function deleteDoc(id: number) {
  ElMessageBox.confirm('确定删除该文档？搜索引擎中的索引也将被删除。', '确认', { type: 'warning' })
    .then(async () => {
      await deleteDocument(id)
      ElMessage.success('已删除')
      loadDocuments()
    }).catch(() => {})
}

function getFileTypeTag(fileType: string): string {
  if (!fileType) return 'info'
  if (fileType.includes('pdf')) return 'danger'
  if (fileType.includes('word') || fileType.includes('document')) return 'primary'
  if (fileType.includes('sheet') || fileType.includes('excel')) return 'success'
  if (fileType.includes('presentation') || fileType.includes('powerpoint')) return 'warning'
  if (fileType.includes('text') || fileType.includes('markdown')) return 'info'
  return 'info'
}

function getFileTypeLabel(fileType: string): string {
  if (!fileType) return '未知'
  if (fileType.includes('pdf')) return 'PDF'
  if (fileType.includes('word') || fileType.includes('document')) return 'Word'
  if (fileType.includes('sheet') || fileType.includes('excel')) return 'Excel'
  if (fileType.includes('presentation') || fileType.includes('powerpoint')) return 'PPT'
  if (fileType.includes('text')) return 'TXT'
  if (fileType.includes('markdown')) return 'MD'
  if (fileType.includes('csv')) return 'CSV'
  if (fileType.includes('json')) return 'JSON'
  if (fileType.includes('xml')) return 'XML'
  return fileType.split('/').pop() || '未知'
}

function formatSize(bytes: number): string {
  if (!bytes) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let size = bytes
  while (size >= 1024 && i < units.length - 1) { size /= 1024; i++ }
  return `${size.toFixed(1)} ${units[i]}`
}

function highlightKeyword(text: string): string {
  if (!text || !searchForm.keyword) return text || ''
  const kw = searchForm.keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  return text.replace(new RegExp(`(${kw})`, 'gi'), '<mark style="background:#ffd54f;padding:0 2px">$1</mark>')
}

function downloadFile(doc: any) {
  if (doc.fileUrl) {
    window.open(doc.fileUrl, '_blank')
  }
}

onMounted(() => {
  loadResources()
  loadDocuments()
})
</script>

<style scoped>
.fulltext-search { padding: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
.search-result-item {
  padding: 12px;
  border: 1px solid #eee;
  border-radius: 6px;
  margin-bottom: 8px;
  transition: all 0.2s;
}
.search-result-item:hover { border-color: #409eff; box-shadow: 0 2px 8px rgba(64,158,255,0.1); }
.doc-header { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.doc-name { font-weight: 600; color: #303133; flex: 1; }
.doc-size { color: #999; font-size: 12px; }
.doc-snippet {
  font-size: 13px;
  color: #666;
  line-height: 1.6;
  padding: 8px;
  background: #f9f9f9;
  border-radius: 4px;
  max-height: 80px;
  overflow: hidden;
}
.doc-meta { margin-top: 6px; font-size: 12px; color: #999; display: flex; gap: 16px; }
:deep(.el-upload-dragger) { width: 100%; }
</style>
