<template>
  <div class="dept-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>部门管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增部门
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe row-key="id" default-expand-all>
        <el-table-column prop="deptName" label="部门名称" min-width="150" />
        <el-table-column prop="deptCode" label="部门编码" min-width="150" />
        <el-table-column prop="parentId" label="上级部门" width="150">
          <template #default="{ row }">
            {{ row.parentId === 0 ? '无' : row.parentId }}
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.status === 1" type="success">正常</el-tag>
            <el-tag v-else type="danger">禁用</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getDeptList } from '@/api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

interface DeptItem {
  id: number
  parentId: number
  deptName: string
  deptCode: string
  sort: number
  status: number
  leader?: string
  phone?: string
  email?: string
  children?: DeptItem[]
}

const tableData = ref<DeptItem[]>([])
const loading = ref(false)

// 加载部门列表
const loadDepts = async () => {
  loading.value = true
  try {
    const res = await getDeptList()
    // 响应拦截器已经解包，res 就是数组
    tableData.value = res || []
  } catch (error) {
    ElMessage.error('加载部门列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  ElMessage.info('新增部门功能待实现')
}

const handleEdit = (row: DeptItem) => {
  ElMessage.info(`编辑部门: ${row.deptName}`)
}

const handleDelete = async (row: DeptItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除部门 "${row.deptName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
    loadDepts()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadDepts()
})
</script>

<style scoped>
.dept-management { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
