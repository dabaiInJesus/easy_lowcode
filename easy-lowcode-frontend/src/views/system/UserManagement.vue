<template>
  <div class="user-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>用户管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增用户
          </el-button>
        </div>
      </template>

      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="用户名/姓名"
            clearable
            @clear="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>
            搜索
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>
            重置
          </el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="realName" label="真实姓名" width="120" />
        <el-table-column prop="phone" label="手机号" width="120" />
        <el-table-column prop="email" label="邮箱" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">
              编辑
            </el-button>
            <el-button link type="warning" size="small" @click="handleResetPassword(row)">
              重置密码
            </el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        ref="formRef"
        :model="formData"
        :rules="formRules"
        label-width="100px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="密码" prop="password" v-if="!formData.id">
          <el-input
            v-model="formData.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="formData.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="真实姓名" prop="realName">
          <el-input v-model="formData.realName" placeholder="请输入真实姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码对话框 -->
    <el-dialog
      v-model="resetPasswordVisible"
      title="重置密码"
      width="400px"
    >
      <el-form :model="resetPasswordForm" label-width="100px">
        <el-form-item label="新密码">
          <el-input
            v-model="resetPasswordForm.newPassword"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPasswordVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPasswordSubmit">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { Plus, Search, Refresh } from '@element-plus/icons-vue'
import { getUserPage, createUser, updateUser, deleteUser, resetPassword } from '@/api/auth'

interface User {
  id?: number
  username: string
  password?: string
  nickname: string
  realName?: string
  phone?: string
  email?: string
  status: number
  createTime?: string
}

const loading = ref(false)
const tableData = ref<User[]>([])
const searchForm = reactive({
  keyword: '',
})
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive<User>({
  username: '',
  password: '',
  nickname: '',
  realName: '',
  phone: '',
  email: '',
  status: 1,
})

const formRules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' },
  ],
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
  ],
}

const resetPasswordVisible = ref(false)
const resetPasswordForm = reactive({
  userId: 0,
  newPassword: '',
})

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getUserPage(pagination.current, pagination.size, searchForm.keyword)
    console.log('用户列表响应:', res)
    // 响应拦截器已经解包，res 就是 PageResult
    if (res && res.records) {
      tableData.value = res.records
      pagination.total = res.total
    } else {
      console.error('响应数据格式错误:', res)
      ElMessage.error('加载数据失败：响应格式错误')
    }
  } catch (error) {
    console.error('加载数据失败:', error)
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.keyword = ''
  handleSearch()
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增用户'
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: User) => {
  dialogTitle.value = '编辑用户'
  Object.assign(formData, row)
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: User) => {
  await ElMessageBox.confirm('确定要删除该用户吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  })
  
  try {
    await deleteUser(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除失败:', error)
  }
}

// 重置密码
const handleResetPassword = (row: User) => {
  resetPasswordForm.userId = row.id!
  resetPasswordForm.newPassword = ''
  resetPasswordVisible.value = true
}

const handleResetPasswordSubmit = async () => {
  if (!resetPasswordForm.newPassword) {
    ElMessage.warning('请输入新密码')
    return
  }
  
  try {
    await resetPassword(resetPasswordForm.userId, resetPasswordForm.newPassword)
    ElMessage.success('密码重置成功')
    resetPasswordVisible.value = false
  } catch (error) {
    console.error('重置密码失败:', error)
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      // 准备提交数据
      const submitData = { ...formData }
      
      // 编辑时不发送密码字段
      if (submitData.id) {
        delete submitData.password
      }
      
      if (formData.id) {
        await updateUser(submitData)
        ElMessage.success('更新成功')
      } else {
        await createUser(submitData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error) {
      console.error('提交失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

// 对话框关闭
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: undefined,
    username: '',
    password: '',
    nickname: '',
    realName: '',
    phone: '',
    email: '',
    status: 1,
  })
}

// 分页
const handleSizeChange = () => {
  loadData()
}

const handleCurrentChange = () => {
  loadData()
}

// 格式化时间
const formatTime = (time: string | undefined) => {
  if (!time) return '-'
  const date = new Date(time)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hours = String(date.getHours()).padStart(2, '0')
  const minutes = String(date.getMinutes()).padStart(2, '0')
  const seconds = String(date.getSeconds()).padStart(2, '0')
  return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.user-management {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}
</style>
