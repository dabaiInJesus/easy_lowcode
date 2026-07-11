<template>
  <div class="workflow-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>AI 工作流</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新建工作流</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="workflowName" label="工作流名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="workflowCode" label="编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
              {{ row.status === 'PUBLISHED' ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleDesign(row)">设计</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
          :page-sizes="[10, 20, 50]" :total="pagination.total"
          layout="total, sizes, prev, pager, next, jumper" @size-change="fetchData" @current-change="fetchData" />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" title="新建工作流" width="500px">
      <el-form :model="formData" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="formData.workflowName" placeholder="请输入工作流名称" />
        </el-form-item>
        <el-form-item label="编码" required>
          <el-input v-model="formData.workflowCode" placeholder="英文编码（唯一）" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="formData.description" type="textarea" :rows="3" />
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
import { Plus } from '@element-plus/icons-vue'
import { getWorkflowPage, createWorkflow, deleteWorkflow } from '@/api/ai-workflow'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const submitting = ref(false)
const formData = reactive({ workflowName: '', workflowCode: '', description: '' })

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const res = await getWorkflowPage(pagination.current, pagination.size)
    tableData.value = res.records
    pagination.total = res.total
  } finally { loading.value = false }
}

function handleAdd() {
  formData.workflowName = ''
  formData.workflowCode = ''
  formData.description = ''
  dialogVisible.value = true
}

async function handleCreate() {
  if (!formData.workflowName || !formData.workflowCode) { ElMessage.warning('请填写必填项'); return }
  submitting.value = true
  try {
    await createWorkflow(formData)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

function handleDesign(row: any) {
  router.push(`/ai/workflow/design/${row.id}`)
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除工作流「${row.workflowName}」？`, '确认删除', { type: 'warning' })
  await deleteWorkflow(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.workflow-management { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pagination-wrapper { display: flex; justify-content: flex-end; margin-top: 16px; }
</style>
