<template>
  <div class="fulltext-search">
    <el-card>
      <template #header>
        <span class="card-title">全文检索</span>
      </template>

      <el-form :inline="true" @submit.prevent="handleSearch">
        <el-form-item label="资源">
          <el-select v-model="form.resourceCode" placeholder="选择资源" filterable style="width: 280px">
            <el-option v-for="r in resourceList" :key="r.resourceCode" :label="`${r.tableComment || r.tableName} (${r.resourceCode})`" :value="r.resourceCode" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" placeholder="输入搜索关键词" clearable style="width: 240px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">检索</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="result.records" border stripe v-loading="loading" style="margin-top: 16px" max-height="500">
        <el-table-column v-for="col in columns" :key="col" :prop="col" :label="col" min-width="140" show-overflow-tooltip />
      </el-table>

      <el-pagination
        v-if="result.total > 0"
        v-model:current-page="form.page"
        v-model:page-size="form.pageSize"
        :total="result.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top: 16px; justify-content: flex-end"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import request from '@/utils/request'

const loading = ref(false)
const resourceList = ref<any[]>([])
const form = reactive({ resourceCode: '', keyword: '', page: 1, pageSize: 20 })
const result = ref<{ records: Record<string, any>[]; total: number }>({ records: [], total: 0 })
const columns = computed(() => result.value.records.length > 0 ? Object.keys(result.value.records[0]) : [])

async function loadResources() {
  try {
    const data = await request({ url: '/collector/table-resource/page', method: 'get', params: { current: 1, size: 200 } }) as any
    resourceList.value = data?.records || []
  } catch { resourceList.value = [] }
}

async function handleSearch() {
  if (!form.resourceCode) { ElMessage.warning('请选择资源'); return }
  if (!form.keyword) { ElMessage.warning('请输入搜索关键词'); return }
  loading.value = true
  try {
    const data = await request({
      url: `/resource/search/fulltext/${form.resourceCode}`,
      method: 'get',
      params: { keyword: form.keyword, page: form.page, pageSize: form.pageSize }
    }) as any
    result.value = data || { records: [], total: 0 }
  } catch (e: any) {
    ElMessage.error(e.message || '检索失败')
  } finally { loading.value = false }
}

onMounted(() => { loadResources() })
</script>

<style scoped>
.fulltext-search { padding: 16px; }
.card-title { font-size: 16px; font-weight: 600; }
</style>
