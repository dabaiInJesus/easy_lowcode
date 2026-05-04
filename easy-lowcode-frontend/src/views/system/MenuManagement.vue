<template>
  <div class="menu-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增菜单
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe row-key="id" default-expand-all>
        <el-table-column prop="menuName" label="菜单名称" min-width="150" />
        <el-table-column prop="path" label="路径" min-width="150" />
        <el-table-column prop="component" label="组件" min-width="150" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 1" type="primary">目录</el-tag>
            <el-tag v-else-if="row.menuType === 2" type="success">菜单</el-tag>
            <el-tag v-else type="info">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="visible" label="显示" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.visible === 1" type="success">显示</el-tag>
            <el-tag v-else type="danger">隐藏</el-tag>
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
import { getMenuList } from '@/api/auth'
import { ElMessage, ElMessageBox } from 'element-plus'

interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuType: number
  path: string
  component: string
  icon: string
  sort: number
  visible: number
  perms?: string
  children?: MenuItem[]
}

const tableData = ref<MenuItem[]>([])
const loading = ref(false)

// 加载菜单列表
const loadMenus = async () => {
  loading.value = true
  try {
    const res: any = await getMenuList()
    // 响应拦截器已经解包，res 就是数组
    tableData.value = res || []
  } catch (error) {
    ElMessage.error('加载菜单列表失败')
  } finally {
    loading.value = false
  }
}

const handleAdd = () => {
  ElMessage.info('新增菜单功能待实现')
}

const handleEdit = (row: MenuItem) => {
  ElMessage.info(`编辑菜单: ${row.menuName}`)
}

const handleDelete = async (row: MenuItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除菜单 "${row.menuName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    ElMessage.success('删除成功')
    loadMenus()
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped>
.menu-management { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
