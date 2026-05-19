<template>
  <div class="ai-config-management">
    <el-card class="search-card">
      <el-button type="primary" @click="handleCreate">新增配置</el-button>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="configName" label="配置名称" min-width="160" />
        <el-table-column prop="provider" label="供应商" width="120" />
        <el-table-column prop="model" label="模型" width="160" />
        <el-table-column prop="apiUrl" label="API地址" min-width="250" show-overflow-tooltip />
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
            <el-button size="small" @click="handleTest(row)" :loading="testLoading === row.id">测试</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑AI配置' : '新增AI配置'" width="600px" @close="handleDialogClose">
      <el-form ref="formRef" :model="formData" :rules="formRules" label-width="120px">
        <el-form-item label="配置名称" prop="configName">
          <el-input v-model="formData.configName" placeholder="如：阿里云通义千问" />
        </el-form-item>
        <el-form-item label="供应商" prop="provider">
          <el-select v-model="formData.provider" placeholder="选择供应商" style="width:100%">
            <el-option label="OpenAI" value="openai" />
            <el-option label="通义千问" value="tongyi" />
            <el-option label="DeepSeek" value="deepseek" />
            <el-option label="百度千帆" value="qianfan" />
            <el-option label="智谱AI" value="zhipu" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型" prop="model">
          <el-input v-model="formData.model" placeholder="如：gpt-4o、qwen-max" />
        </el-form-item>
        <el-form-item label="API地址" prop="apiUrl">
          <el-input v-model="formData.apiUrl" placeholder="API 端点地址" />
        </el-form-item>
        <el-form-item label="API密钥" prop="apiKey">
          <el-input v-model="formData.apiKey" type="password" show-password placeholder="API Key" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="formData.remark" type="textarea" :rows="2" />
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
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getAiConfigPage, createAiConfig, updateAiConfig, deleteAiConfig, testAiConnection, type AiConfig } from '@/api/ai'

const tableData = ref<AiConfig[]>([])
const loading = ref(false)
const testLoading = ref<number | null>(null)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const formData = reactive<Partial<AiConfig>>({ configName: '', provider: 'openai', model: '', apiUrl: '', apiKey: '', status: 1, remark: '' })

const formRules: FormRules = {
  configName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择供应商', trigger: 'change' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAiConfigPage(1, 100)
    tableData.value = res.records || []
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const handleCreate = () => {
  isEdit.value = false
  Object.assign(formData, { configName: '', provider: 'openai', model: '', apiUrl: '', apiKey: '', status: 1, remark: '' })
  dialogVisible.value = true
}

const handleEdit = (row: AiConfig) => {
  isEdit.value = true
  Object.assign(formData, row)
  dialogVisible.value = true
}

const handleDelete = async (row: AiConfig) => {
  try {
    await ElMessageBox.confirm('确定删除该配置？', '提示', { type: 'warning' })
    await deleteAiConfig(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

const handleTest = async (row: AiConfig) => {
  testLoading.value = row.id!
  try {
    const ok = await testAiConnection({ provider: row.provider, apiKey: row.apiKey, apiUrl: row.apiUrl, model: row.model })
    ElMessage.success(ok ? '连接成功' : '连接失败')
  } catch (e: any) { ElMessage.error(e.message || '连接失败') }
  finally { testLoading.value = null }
}

const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateAiConfig(formData as AiConfig)
        ElMessage.success('更新成功')
      } else {
        await createAiConfig(formData as AiConfig)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e: any) { ElMessage.error(e.message || '操作失败') }
    finally { submitLoading.value = false }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
}

onMounted(loadData)
</script>

<style scoped>
.ai-config-management { padding: 20px; }
.search-card { margin-bottom: 20px; }
.table-card { margin-bottom: 20px; }
</style>
