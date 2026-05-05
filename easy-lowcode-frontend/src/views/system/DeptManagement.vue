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
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="deptCode" label="部门编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="deptName" label="部门名称" min-width="150" />
        <el-table-column prop="leader" label="负责人" width="120" />
        <el-table-column prop="phone" label="联系电话" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
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
        <el-form-item label="上级部门" prop="parentId">
          <el-tree-select
            v-model="formData.parentId"
            :data="treeSelectData"
            :props="{ label: 'deptName', value: 'id', children: 'children' }"
            placeholder="请选择上级部门"
            check-strictly
            clearable
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="部门名称" prop="deptName">
          <el-input
            v-model="formData.deptName"
            placeholder="请输入部门名称"
            @input="handleDeptNameInput"
          />
        </el-form-item>
        <el-form-item label="部门编码" prop="deptCode">
          <el-input
            v-model="formData.deptCode"
            placeholder="请输入部门编码（英文）"
          />
          <div class="form-tip">建议使用英文，如：tech_dept、hr_dept</div>
        </el-form-item>
        <el-form-item label="负责人" prop="leader">
          <el-input v-model="formData.leader" placeholder="请输入负责人姓名" />
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="formData.phone" placeholder="请输入联系电话" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="formData.email" placeholder="请输入邮箱地址" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="formData.sort" :min="0" :max="999" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">正常</el-radio>
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
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { getDeptList, createDept, updateDept, deleteDept } from '@/api/auth'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

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
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const formData = ref({
  id: 0,
  parentId: 0,
  deptName: '',
  deptCode: '',
  leader: '',
  phone: '',
  email: '',
  sort: 1,
  status: 1
})

// 表单验证规则
const formRules: FormRules = {
  deptName: [
    { required: true, message: '请输入部门名称', trigger: 'blur' }
  ],
  deptCode: [
    { required: true, message: '请输入部门编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '部门编码必须以字母开头，只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  phone: [
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }
  ]
}

// 对话框标题
const dialogTitle = computed(() => isEdit.value ? '编辑部门' : '新增部门')

// 树形选择器数据
const treeSelectData = computed(() => {
  // 添加一个根节点选项
  return [{
    id: 0,
    deptName: '顶级部门',
    children: tableData.value
  }]
})

// 加载部门列表
const loadDepts = async () => {
  loading.value = true
  try {
    const res: any = await getDeptList()
    tableData.value = res || []
  } catch (error) {
    ElMessage.error('加载部门列表失败')
  } finally {
    loading.value = false
  }
}

// 新增部门
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑部门
const handleEdit = (row: DeptItem) => {
  isEdit.value = true
  formData.value = {
    id: row.id,
    parentId: row.parentId,
    deptName: row.deptName,
    deptCode: row.deptCode,
    leader: row.leader || '',
    phone: row.phone || '',
    email: row.email || '',
    sort: row.sort,
    status: row.status
  }
  dialogVisible.value = true
}

// 删除部门
const handleDelete = async (row: DeptItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除部门 "${row.deptName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    await deleteDept(row.id)
    ElMessage.success('删除成功')
    loadDepts()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateDept(formData.value)
        ElMessage.success('更新成功')
      } else {
        await createDept(formData.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadDepts()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: 0,
    parentId: 0,
    deptName: '',
    deptCode: '',
    leader: '',
    phone: '',
    email: '',
    sort: 1,
    status: 1
  }
  formRef.value?.clearValidate()
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
}

// 部门名称输入时自动生成dept_code
const handleDeptNameInput = (value: string) => {
  // 如果用户还没有手动修改过dept_code，则自动生成
  if (!isEdit.value && !formData.value.deptCode) {
    formData.value.deptCode = generateDeptCode(value)
  }
}

// 生成部门编码
const generateDeptCode = (name: string): string => {
  if (!name || name.trim() === '') {
    return ''
  }
  
  // 简单处理：移除空格和特殊字符，转为小写
  let code = name.replace(/[\s\u3000]+/g, '_')  // 替换空格为下划线
                .replace(/[^a-zA-Z0-9_\u4e00-\u9fa5]/g, '')  // 移除特殊字符
                .toLowerCase()
  
  // 如果是中文，建议用户手动输入英文
  if (/[\u4e00-\u9fa5]/.test(code)) {
    // 返回原样，让用户自己输入英文
    return code
  }
  
  // 确保以字母开头
  if (/^\d/.test(code)) {
    code = 'dept_' + code
  }
  
  return code
}

onMounted(() => {
  loadDepts()
})
</script>

<style scoped>
.dept-management { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}
</style>
