<template>
  <div class="multi-resource-search">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">多资源统一查询</span>
          <el-button v-if="searchMode === 'unified'" type="primary" @click="handleUnifiedSearch" :loading="loading">搜索</el-button>
        </div>
      </template>

      <!-- 模式切换 -->
      <el-radio-group v-model="searchMode" style="margin-bottom:16px">
        <el-radio-button value="unified">统一Key搜索</el-radio-button>
        <el-radio-button value="multi">多资源关键词搜索</el-radio-button>
      </el-radio-group>

      <!-- 统一Key搜索模式 -->
      <template v-if="searchMode === 'unified'">
        <el-form :inline="true" @submit.prevent="handleUnifiedSearch">
          <el-form-item label="统一Key">
            <el-select v-model="unifiedForm.key" filterable placeholder="选择统一Key" style="width:200px" @change="loadKeyMappings">
              <el-option v-for="k in unifiedKeys" :key="k.unifiedKey" :label="`${k.displayName || k.unifiedKey} (${k.unifiedKey})`" :value="k.unifiedKey" />
            </el-select>
          </el-form-item>
          <el-form-item label="查询值">
            <el-input v-model="unifiedForm.value" placeholder="输入查询值" clearable style="width:240px" @keyup.enter="handleUnifiedSearch" />
          </el-form-item>
          <el-form-item v-if="selectedKeyMappings.length > 0">
            <span style="font-size:12px;color:#999">将在 {{ selectedKeyMappings.length }} 个资源的 {{ selectedKeyMappings.map(m => m.fieldName).join(', ') }} 字段中精确匹配</span>
          </el-form-item>
        </el-form>
      </template>

      <!-- 多资源关键词搜索模式 -->
      <template v-else>
        <el-form :inline="true" @submit.prevent="handleMultiSearch">
          <el-form-item label="资源">
            <el-select v-model="multiForm.resourceCodes" multiple filterable placeholder="选择多个资源" style="width:420px">
              <el-option v-for="r in resourceList" :key="r.resourceCode" :label="`${r.tableComment || r.tableName} (${r.resourceCode})`" :value="r.resourceCode" />
            </el-select>
          </el-form-item>
          <el-form-item label="关键词">
            <el-input v-model="multiForm.keyword" placeholder="搜索关键词" clearable style="width:200px" @keyup.enter="handleMultiSearch" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleMultiSearch" :loading="loading">搜索</el-button>
          </el-form-item>
        </el-form>
      </template>
    </el-card>

    <!-- 结果表格 -->
    <el-card style="margin-top:12px">
      <el-table :data="result.records" border stripe v-loading="loading" style="width:100%" max-height="600">
        <el-table-column v-for="col in columns" :key="col" :prop="col" :label="col" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">
            <span v-if="col === '_sourceResource'" style="color:#409eff">{{ row[col] }}</span>
            <span v-else-if="col === '_unifiedKey'" style="color:#67c23a">{{ row[col] }}</span>
            <span v-else-if="col === '_unifiedValue'" style="color:#909399">{{ row[col] }}</span>
            <span v-else>{{ row[col] }}</span>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-if="result.total > 0"
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="result.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top:16px;justify-content:flex-end"
        @size-change="handlePageChange"
        @current-change="handlePageChange"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'
import { getDistinctKeys, getMappingsByKey } from '@/api/unifiedKey'
import type { UnifiedKeyMapping } from '@/api/unifiedKey'

const loading = ref(false)
const resourceList = ref<any[]>([])
const searchMode = ref<'unified' | 'multi'>('unified')
const page = ref(1)
const pageSize = ref(20)

// Unified key search
const unifiedKeys = ref<UnifiedKeyMapping[]>([])
const selectedKeyMappings = ref<UnifiedKeyMapping[]>([])
const unifiedForm = reactive({ key: '', value: '' })

// Multi resource search
const multiForm = reactive({ resourceCodes: [] as string[], keyword: '' })

const result = ref<{ records: Record<string, any>[]; total: number }>({ records: [], total: 0 })

const columns = computed(() => {
  if (result.value.records.length > 0) {
    return Object.keys(result.value.records[0])
  }
  return []
})

async function loadResources() {
  try {
    const data = await request({ url: '/collector/table-resource/page', method: 'get', params: { current: 1, size: 200 } }) as any
    resourceList.value = data?.records || []
  } catch { resourceList.value = [] }
}

async function loadUnifiedKeys() {
  try {
    const data = await getDistinctKeys()
    unifiedKeys.value = Array.isArray(data) ? data : []
  } catch { unifiedKeys.value = [] }
}

async function loadKeyMappings() {
  if (!unifiedForm.key) return
  try {
    selectedKeyMappings.value = await getMappingsByKey(unifiedForm.key) || []
  } catch { selectedKeyMappings.value = [] }
}

async function handleUnifiedSearch() {
  if (!unifiedForm.key) { ElMessage.warning('请选择统一Key'); return }
  if (!unifiedForm.value) { ElMessage.warning('请输入查询值'); return }
  loading.value = true
  try {
    const data = await request({
      url: '/resource/search/unified',
      method: 'post',
      data: { unifiedKey: unifiedForm.key, value: unifiedForm.value, params: { page: page.value, pageSize: pageSize.value } },
    }) as any
    result.value = data || { records: [], total: 0 }
  } catch (e: any) {
    ElMessage.error(e.message || '搜索失败')
    result.value = { records: [], total: 0 }
  } finally { loading.value = false }
}

async function handleMultiSearch() {
  if (!multiForm.resourceCodes.length) { ElMessage.warning('请选择至少一个资源'); return }
  loading.value = true
  try {
    const params: Record<string, any> = { page: page.value, pageSize: pageSize.value }
    if (multiForm.keyword) params.keyword = multiForm.keyword
    const data = await request({
      url: '/resource/search/multi',
      method: 'post',
      data: { resourceCodes: multiForm.resourceCodes, params },
    }) as any
    result.value = data || { records: [], total: 0 }
  } catch (e: any) {
    ElMessage.error(e.message || '搜索失败')
    result.value = { records: [], total: 0 }
  } finally { loading.value = false }
}

function handlePageChange() {
  if (searchMode.value === 'unified') {
    handleUnifiedSearch()
  } else {
    handleMultiSearch()
  }
}

onMounted(() => {
  loadResources()
  loadUnifiedKeys()
})
</script>

<style scoped>
.multi-resource-search { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 16px; font-weight: 600; }
</style>
