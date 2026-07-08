<template>
  <div class="single-resource-search">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">单资源查询</span>
          <el-button v-if="form.resourceCode" type="primary" @click="handleSearch" :loading="loading">查询</el-button>
        </div>
      </template>

      <!-- 配置面板 -->
      <el-form :inline="true" label-width="80px" @submit.prevent="handleSearch">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="资源">
              <el-select v-model="form.resourceCode" filterable style="width:100%" @change="onResourceChange" placeholder="选择资源">
                <el-option v-for="r in resourceList" :key="r.resourceCode" :label="`${r.tableComment || r.tableName} (${r.resourceCode})`" :value="r.resourceCode" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="模板">
              <el-select v-model="searchParams.templateName" clearable placeholder="自动生成" style="width:100%" @change="onTemplateChange">
                <el-option v-for="t in templates" :key="t.name" :label="t.label || t.name" :value="t.name" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="6">
            <el-form-item label="关键词">
              <el-input v-model="searchParams.keyword" clearable placeholder="模糊搜索" @keyup.enter="handleSearch" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>

      <!-- 模板参数（仅在选择了模板时显示） -->
      <el-card v-if="selectedTemplate && selectedTemplate.parameters.length > 0" shadow="never" style="margin-bottom:12px">
        <template #header><span style="font-weight:600;font-size:14px">模板参数</span></template>
        <el-form :inline="true" label-width="100px">
          <el-form-item v-for="p in selectedTemplate.parameters" :key="p.name" :label="p.label || p.name">
            <el-input v-model="templateParamValues[p.name]" :placeholder="p.defaultValue || p.name" clearable style="width:160px" />
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 过滤条件面板 -->
      <el-collapse v-if="filterFields.length > 0" style="margin-bottom:12px">
        <el-collapse-item title="过滤条件">
          <el-row :gutter="16">
            <el-col v-for="f in filterFields" :key="f.columnName" :span="6">
              <el-form-item :label="f.fieldLabel || f.columnComment || f.columnName" label-width="90px">
                <el-input v-if="f.dataType === 'string'" v-model="filterValues[f.columnName]" :placeholder="(f.exactQuery ? '精确' : '模糊') + '搜索'" clearable size="small" @change="handleSearch" />
                <el-input-number v-else-if="f.dataType === 'number'" v-model="filterValues[f.columnName]" :placeholder="f.columnName" clearable size="small" style="width:100%" @change="handleSearch" />
                <el-date-picker v-else-if="f.dataType === 'date'" v-model="filterValues[f.columnName]" type="date" placeholder="选择日期" clearable size="small" style="width:100%" @change="handleSearch" />
                <el-select v-else v-model="filterValues[f.columnName]" clearable placeholder="选择" size="small" style="width:100%" @change="handleSearch">
                  <el-option label="是" :value="true" />
                  <el-option label="否" :value="false" />
                </el-select>
              </el-form-item>
            </el-col>
          </el-row>
        </el-collapse-item>
      </el-collapse>

    </el-card>

    <!-- 数据表格 -->
    <el-card style="margin-top:12px">
      <el-table :data="result.records" border stripe v-loading="loading" style="width:100%" max-height="600"
        @sort-change="onSortChange" :default-sort="defaultSort">
        <el-table-column v-for="col in tableColumns" :key="col.prop" :prop="col.prop" :label="col.label"
          :width="col.width" :align="col.align" :fixed="col.fixed" :sortable="col.sortable ? 'custom' : false"
          show-overflow-tooltip :min-width="col.width || 120">
          <template #default="{ row }">
            <span v-if="col.prop === '_sourceResource'" style="color:#409eff">{{ row[col.prop] }}</span>
            <span v-else-if="col.prop === '_unifiedKey'" style="color:#67c23a">{{ row[col.prop] }}</span>
            <span v-else>{{ row[col.prop] }}</span>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="form.resourceCode ? '暂无数据' : '请先选择资源' " />
        </template>
      </el-table>

      <el-pagination
        v-if="result.total > 0"
        v-model:current-page="searchParams.page"
        v-model:page-size="searchParams.pageSize"
        :total="result.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        style="margin-top:16px;justify-content:flex-end"
        @size-change="handleSearch"
        @current-change="handleSearch"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getTableResourceList, getResourceFields, getResourceTemplates, singleSearch } from '@/api/resourceSearch'
import type { FieldConfig, DisplayFieldSetting } from '@/types/tableResource'

const loading = ref(false)
const resourceList = ref<any[]>([])
const templates = ref<any[]>([])
const fieldConfigs = ref<FieldConfig[]>([])
const displaySettings = ref<Record<string, DisplayFieldSetting>>({})

const form = reactive({ resourceCode: '' })
const searchParams = reactive({
  keyword: '',
  page: 1,
  pageSize: 20,
  templateName: '',
})
const filterValues = reactive<Record<string, any>>({})
const templateParamValues = reactive<Record<string, any>>({})
const result = ref<{ records: Record<string, any>[]; total: number }>({ records: [], total: 0 })
const sortField = ref('')
const sortDir = ref<'ASC' | 'DESC'>('DESC')

const defaultSort = computed(() => {
  if (!sortField.value) return {}
  return { prop: sortField.value, order: sortDir.value === 'ASC' ? 'ascending' : 'descending' }
})

const selectedTemplate = computed(() => {
  if (!searchParams.templateName) return null
  return templates.value.find(t => t.name === searchParams.templateName) || null
})

const filterFields = computed(() => {
  return fieldConfigs.value.filter(f => f.searchable)
})

const tableColumns = computed(() => {
  const cols: { prop: string; label: string; width?: number; align?: string; fixed?: string; sortable: boolean }[] = []
  const ds = displaySettings.value
  if (fieldConfigs.value.length > 0 && Object.keys(ds).length > 0) {
    fieldConfigs.value.forEach(f => {
      const setting = ds[f.columnName]
      if (setting && !setting.visible) return
      cols.push({
        prop: f.columnName,
        label: setting?.label || f.fieldLabel || f.columnComment || f.columnName,
        width: setting?.width,
        align: setting?.align || 'left',
        fixed: setting?.fixed,
        sortable: setting?.sortable !== false && f.sortable,
      })
    })
  } else {
    if (result.value.records.length > 0) {
      Object.keys(result.value.records[0]).forEach(key => {
        if (key.startsWith('_')) return
        cols.push({ prop: key, label: key, sortable: true })
      })
    }
  }
  return cols
})

async function loadResources() {
  try {
    const data = await getTableResourceList() as any
    resourceList.value = data?.records || []
  } catch { resourceList.value = [] }
}

async function onResourceChange() {
  searchParams.templateName = ''
  searchParams.keyword = ''
  searchParams.page = 1
  Object.keys(filterValues).forEach(k => delete filterValues[k])
  Object.keys(templateParamValues).forEach(k => delete templateParamValues[k])
  templates.value = []
  fieldConfigs.value = []
  displaySettings.value = {}
  result.value = { records: [], total: 0 }

  if (!form.resourceCode) return
  try {
    const [fieldsData, tmpls] = await Promise.all([
      getResourceFields(form.resourceCode),
      getResourceTemplates(form.resourceCode),
    ])
    fieldConfigs.value = fieldsData.fields || []
    displaySettings.value = fieldsData.configJson?.displaySettings?.fields || {}
    templates.value = Array.isArray(tmpls) ? tmpls : []
    if (templates.value.length > 0) {
      const defaultTmpl = templates.value.find((t: any) => t.isDefault)
      if (defaultTmpl) searchParams.templateName = defaultTmpl.name
    }
    handleSearch()
  } catch (e: any) {
    ElMessage.error(e.message || '加载资源配置失败')
  }
}

function onTemplateChange() {
  templateParamValues.value = {}
  if (selectedTemplate.value) {
    selectedTemplate.value.parameters.forEach((p: any) => {
      if (p.defaultValue) templateParamValues[p.name] = p.defaultValue
    })
  }
}

function onSortChange(sort: { prop?: string; order?: string }) {
  if (sort.prop && sort.order) {
    sortField.value = sort.prop
    sortDir.value = sort.order === 'ascending' ? 'ASC' : 'DESC'
  } else {
    sortField.value = ''
    sortDir.value = 'DESC'
  }
  handleSearch()
}

async function handleSearch() {
  if (!form.resourceCode) { ElMessage.warning('请选择资源'); return }
  loading.value = true
  try {
    const filters: Record<string, any> = {}
    for (const [k, v] of Object.entries(filterValues)) {
      if (v !== null && v !== undefined && v !== '') {
        const fc = fieldConfigs.value.find(f => f.columnName === k)
        if (fc?.exactQuery) {
          filters[k] = v
        } else {
          filters[`${k}_like`] = v
        }
      }
    }
    const params: Record<string, any> = {
      page: searchParams.page,
      pageSize: searchParams.pageSize,
      filters,
    }
    if (searchParams.keyword) params.keyword = searchParams.keyword
    if (sortField.value) params.orderField = sortField.value
    if (sortDir.value) params.orderDirection = sortDir.value
    if (searchParams.templateName) params.templateName = searchParams.templateName
    if (Object.keys(templateParamValues).length > 0) {
      params.templateParams = { ...templateParamValues }
    }
    const data = await singleSearch(form.resourceCode, params)
    result.value = data || { records: [], total: 0 }
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
    result.value = { records: [], total: 0 }
  } finally { loading.value = false }
}

onMounted(() => { loadResources() })
</script>

<style scoped>
.single-resource-search { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 16px; font-weight: 600; }
:deep(.el-collapse-item__content) { padding-bottom: 8px; }
</style>
