<template>
  <div class="dashboard-management">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="大屏名称/标题" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="草稿" :value="0" />
            <el-option label="已发布" :value="1" />
            <el-option label="已下线" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleCreate">新建大屏</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 卡片式展示 -->
    <el-row :gutter="20" v-loading="loading">
      <el-col v-for="item in tableData" :key="item.id" :xs="24" :sm="12" :md="8" :lg="6" style="margin-bottom:20px">
        <el-card class="dashboard-card" :body-style="{ padding: '0' }" shadow="hover">
          <div class="card-preview" :style="{ backgroundColor: item.backgroundColor || '#0a1628' }">
            <div class="card-title">{{ item.title || item.name }}</div>
            <div class="card-chart-count">{{ item.chartCount || 0 }} 个图表</div>
          </div>
          <div class="card-body">
            <div class="card-info">
              <span class="card-name">{{ item.name }}</span>
              <el-tag v-if="item.status === 0" size="small" type="info">草稿</el-tag>
              <el-tag v-else-if="item.status === 1" size="small" type="success">已发布</el-tag>
              <el-tag v-else size="small" type="danger">已下线</el-tag>
            </div>
            <div class="card-actions">
              <el-button size="small" type="primary" @click="handleDesign(item)">设计</el-button>
              <el-button size="small" @click="handlePreview(item)">预览</el-button>
              <el-button v-if="item.status === 0" size="small" @click="handlePublish(item)">发布</el-button>
              <el-button size="small" @click="handleCopy(item)">复制</el-button>
              <el-button size="small" type="danger" @click="handleDelete(item)">删除</el-button>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
      :total="pagination.total" :page-sizes="[12,24,36,48]" layout="total, sizes, prev, pager, next, jumper"
      @size-change="loadData" @current-change="loadData" class="pagination" />

    <!-- 创建/编辑大屏对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑大屏' : '新建大屏'" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="大屏名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入大屏名称" />
        </el-form-item>
        <el-form-item label="大屏编码" prop="code">
          <el-input v-model="formData.code" placeholder="唯一标识" />
        </el-form-item>
        <el-form-item label="大屏标题">
          <el-input v-model="formData.title" placeholder="展示标题，不填则使用名称" />
        </el-form-item>
        <el-form-item label="大屏描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="描述" />
        </el-form-item>
        <el-form-item label="宽(px)">
          <el-input-number v-model="formData.width" :min="800" :max="3840" :step="100" />
        </el-form-item>
        <el-form-item label="高(px)">
          <el-input-number v-model="formData.height" :min="600" :max="2160" :step="100" />
        </el-form-item>
        <el-form-item label="背景色">
          <el-color-picker v-model="formData.backgroundColor" />
        </el-form-item>
        <el-form-item label="自动刷新">
          <el-input-number v-model="formData.refreshInterval" :min="0" :max="3600" /> 秒（0=不刷新）
        </el-form-item>
        <el-form-item label="分类">
          <el-input v-model="formData.category" placeholder="如：运营、监控" />
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="formData.tags" placeholder="逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getDashboardPage, createDashboard, updateDashboard, deleteDashboard, publishDashboard, copyDashboard, type Dashboard } from '@/api/dashboard'

const router = useRouter()
const searchForm = reactive({ keyword: '', status: undefined as number | undefined })
const tableData = ref<Dashboard[]>([])
const loading = ref(false)
const pagination = reactive({ current: 1, size: 12, total: 0 })
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formData = reactive<Partial<Dashboard>>({
  name: '', code: '', title: '', description: '', width: 1920, height: 1080,
  backgroundColor: '#0a1628', refreshInterval: 0, category: '', tags: '', status: 0
})
const formRules: FormRules = {
  name: [{ required: true, message: '请输入大屏名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入大屏编码', trigger: 'blur' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getDashboardPage(pagination.current, pagination.size, searchForm.keyword, searchForm.status)
    tableData.value = res.records
    pagination.total = res.total
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; searchForm.status = undefined; handleSearch() }

const handleCreate = () => {
  isEdit.value = false; dialogVisible.value = true
  Object.assign(formData, { name: '', code: '', title: '', description: '', width: 1920, height: 1080, backgroundColor: '#0a1628', refreshInterval: 0, category: '', tags: '', status: 0 })
}


const handleDesign = (row: Dashboard) => {
  window.open(`/dashboard/design/${row.id}`, '_blank')
}

const handlePreview = (row: Dashboard) => {
  window.open(`/dashboard/view/${row.id}`, '_blank')
}

const handlePublish = async (row: Dashboard) => {
  try {
    await publishDashboard(row.id!)
    ElMessage.success('发布成功')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '发布失败') }
}

const handleCopy = async (row: Dashboard) => {
  try {
    await copyDashboard(row.id!)
    ElMessage.success('复制成功')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '复制失败') }
}

const handleDelete = async (row: Dashboard) => {
  try {
    await ElMessageBox.confirm('确定删除该大屏及其所有图表？', '提示', { type: 'warning' })
    await deleteDashboard(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) { await updateDashboard(formData); ElMessage.success('更新成功') }
      else { await createDashboard(formData); ElMessage.success('创建成功') }
      dialogVisible.value = false; loadData()
    } catch (e: any) { ElMessage.error(e.message || '操作失败') }
    finally { submitLoading.value = false }
  })
}

const handleDialogClose = () => { formRef.value?.resetFields() }

onMounted(() => { loadData() })
</script>

<style scoped>
.dashboard-management { padding: 20px; }
.search-card { margin-bottom: 20px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
.dashboard-card { cursor: pointer; transition: transform .2s; }
.dashboard-card:hover { transform: translateY(-4px); }
.card-preview { height: 140px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #fff; position: relative; }
.card-title { font-size: 18px; font-weight: 600; text-shadow: 0 2px 4px rgba(0,0,0,.5); }
.card-chart-count { font-size: 12px; opacity: .7; margin-top: 8px; }
.card-body { padding: 12px 16px; }
.card-info { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.card-name { font-weight: 500; font-size: 14px; }
.card-actions { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
