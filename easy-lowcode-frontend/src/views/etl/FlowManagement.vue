<template>
  <div class="flow-management">
    <!-- 搜索栏 -->
    <el-card shadow="never" class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="流程名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable>
            <el-option label="草稿" value="DRAFT" />
            <el-option label="运行中" value="RUNNING" />
            <el-option label="已完成" value="COMPLETED" />
            <el-option label="失败" value="FAILED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card shadow="never" class="table-card">
      <template #header>
        <div class="card-header">
          <span>流程列表</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon> 新建流程
          </el-button>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="flowName" label="流程名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="flowCode" label="流程编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.flowStatus)" size="small">
              {{ getStatusLabel(row.flowStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调度方式" width="100" align="center">
          <template #default="{ row }">
            {{ row.scheduleType === 'MANUAL' ? '手动' : row.scheduleType }}
          </template>
        </el-table-column>
        <el-table-column label="最后执行" width="180">
          <template #default="{ row }">
            <div v-if="row.lastExecTime">
              <div>{{ row.lastExecTime }}</div>
              <el-tag :type="getStatusType(row.lastExecStatus)" size="small">
                {{ getStatusLabel(row.lastExecStatus) }}
              </el-tag>
            </div>
            <span v-else class="text-muted">未执行</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleDesign(row)">
              <el-icon><Edit /></el-icon> 设计
            </el-button>
            <el-button text type="success" size="small" @click="handleExecute(row)"
              :disabled="row.flowStatus === 'RUNNING'">
              <el-icon><VideoPlay /></el-icon> 执行
            </el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">
              <el-icon><Delete /></el-icon> 删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="pagination.current"
          v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]"
          :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 新建流程对话框 -->
    <el-dialog v-model="dialogVisible" title="新建流程" width="500px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="流程名称" required>
          <el-input v-model="formData.flowName" placeholder="请输入流程名称" />
        </el-form-item>
        <el-form-item label="流程编码" required>
          <el-input v-model="formData.flowCode" placeholder="请输入流程编码（唯一）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" placeholder="流程描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Edit, Delete, VideoPlay } from '@element-plus/icons-vue'
import { getFlowPage, createFlow, deleteFlow, executeFlow } from '@/api/flow'

const router = useRouter()

const loading = ref(false)
const tableData = ref<any[]>([])
const searchForm = reactive({ keyword: '', status: '' })
const pagination = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const submitting = ref(false)
const formData = reactive({ flowName: '', flowCode: '', description: '' })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getFlowPage(pagination.current, pagination.size, searchForm.keyword, searchForm.status)
    tableData.value = res.records
    pagination.total = res.total
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pagination.current = 1
  fetchData()
}

function handleReset() {
  searchForm.keyword = ''
  searchForm.status = ''
  pagination.current = 1
  fetchData()
}

function handleAdd() {
  formData.flowName = ''
  formData.flowCode = ''
  formData.description = ''
  dialogVisible.value = true
}

async function handleCreate() {
  if (!formData.flowName || !formData.flowCode) {
    ElMessage.warning('请填写必填项')
    return
  }
  submitting.value = true
  try {
    await createFlow(formData)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchData()
  } finally {
    submitting.value = false
  }
}

function handleDesign(row: any) {
  router.push(`/etl/flow/design/${row.id}`)
}

async function handleExecute(row: any) {
  try {
    await executeFlow(row.id)
    ElMessage.success('执行已启动')
    fetchData()
  } catch (e) {
    // 错误已由拦截器处理
  }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除流程「${row.flowName}」？`, '确认删除', { type: 'warning' })
  await deleteFlow(row.id)
  ElMessage.success('删除成功')
  fetchData()
}

function getStatusType(status: string) {
  const map: Record<string, string> = { DRAFT: 'info', RUNNING: 'warning', COMPLETED: 'success', FAILED: 'danger' }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = { DRAFT: '草稿', RUNNING: '运行中', COMPLETED: '已完成', FAILED: '失败' }
  return map[status] || status
}
</script>

<style scoped>
.flow-management {
  padding: 16px;
}

.search-card {
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.pagination-wrapper {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.text-muted {
  color: #999;
  font-size: 12px;
}
</style>
