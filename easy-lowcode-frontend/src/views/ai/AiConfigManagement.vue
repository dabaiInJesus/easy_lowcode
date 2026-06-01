<template>
  <div class="ai-config-management">
    <el-card class="search-card">
      <el-button type="primary" @click="handleCreate">新增配置</el-button>
    </el-card>
    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="displayName" label="配置名称" min-width="160" />
        <el-table-column prop="provider" label="供应商" width="120">
          <template #default="{ row }">
            {{ getProviderLabel(row.provider) }}
          </template>
        </el-table-column>
        <el-table-column prop="model" label="模型" width="160" />
        <el-table-column prop="baseUrl" label="API地址" min-width="250" show-overflow-tooltip />
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
        <el-form-item label="配置名称" prop="displayName">
          <el-input v-model="formData.displayName" placeholder="如：阿里云通义千问" />
        </el-form-item>
        <el-form-item label="供应商" prop="provider">
          <el-select v-model="formData.provider" placeholder="选择供应商" style="width:100%" @change="handleProviderChange">
            <el-option v-for="item in providerOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型" prop="model">
          <el-select
            v-model="formData.model"
            placeholder="选择或搜索模型"
            style="width:100%"
            filterable
            allow-create
            default-first-option
            :filter-method="filterModels"
          >
            <el-option
              v-for="model in filteredModels"
              :key="model.value"
              :label="model.label"
              :value="model.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="API地址" prop="baseUrl">
          <el-input v-model="formData.baseUrl" placeholder="API 端点地址" />
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
        <el-button @click="handleFormTest" :loading="formTestLoading">测试</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, watch, nextTick } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getAiConfigPage, createAiConfig, updateAiConfig, deleteAiConfig, testAiConnection, getAiConfigById } from '@/api/ai'
import { useUserStore } from '@/stores'
import request from '@/utils/request'

interface AiConfig {
  id?: number
  provider: string
  displayName: string
  apiKey?: string
  baseUrl?: string
  model?: string
  status?: number
  remark?: string
}

interface ModelOption {
  label: string
  value: string
}

interface ProviderOption {
  label: string
  value: string
  defaultUrl: string
  models: string[]
}

const providerConfig: Record<string, ProviderOption> = {
  openai: {
    label: 'OpenAI',
    value: 'openai',
    defaultUrl: 'https://api.openai.com/v1',
    models: ['gpt-4.5', 'gpt-4.1', 'o3-pro', 'o3', 'o4-mini', 'gpt-4o', 'gpt-4o-mini', 'gpt-3.5-turbo']
  },
  dashscope: {
    label: '通义千问',
    value: 'dashscope',
    defaultUrl: 'https://dashscope.aliyuncs.com/api/v1',
    models: ['qwen3.5-max', 'qwen3-max-thinking', 'qwen3-coder-next', 'qwen3-plus', 'qwen3', 'qwen2.5-plus', 'qwen2.5-turbo']
  },
  deepseek: {
    label: 'DeepSeek',
    value: 'deepseek',
    defaultUrl: 'https://api.deepseek.com/v1',
    models: ['deepseek-v4-pro', 'deepseek-v4-flash', 'deepseek-reasoner', 'deepseek-chat', 'deepseek-coder-v2']
  },
  wenxin: {
    label: '百度文心一言',
    value: 'wenxin',
    defaultUrl: 'https://aip.baidubce.com/rpc/2.0/ai_custom/v1/wenxinworkshop',
    models: ['ernie-5.0', 'ernie-5.0-thinking-preview', 'ernie-4.5-turbo-128k', 'ernie-4.0-pro', 'ernie-4.0-turbo', 'ernie-lite-8k']
  },
  hunyuan: {
    label: '腾讯混元',
    value: 'hunyuan',
    defaultUrl: 'https://api.hunyuan.cloud.tencent.com/v1',
    models: ['hy3', 'hunyuan-2.0-think', 'hunyuan-2.0-instruct', 'hunyuan-pro', 'hunyuan-flash']
  },
  zhipu: {
    label: '智谱AI',
    value: 'zhipu',
    defaultUrl: 'https://open.bigmodel.cn/api/paas/v4',
    models: ['glm-5', 'glm-5-turbo', 'glm-5-flash', 'glm-4.7', 'glm-4-plus', 'glm-4-flash', 'glm-4-32k']
  },
  moonshot: {
    label: 'Moonshot',
    value: 'moonshot',
    defaultUrl: 'https://api.moonshot.cn/v1',
    models: ['kimi-k2.6', 'kimi-k2.5', 'kimi-k2', 'moonshot-v1']
  },
  ollama: {
    label: 'Ollama',
    value: 'ollama',
    defaultUrl: 'http://localhost:11434',
    models: ['llama3.3', 'llama3.2', 'llama3.1', 'qwen2.5', 'qwen2', 'mistral', 'mixtral', 'codellama', 'phi4', 'deepseek-coder-v2', 'nomic-embed-text']
  },
  minimax: {
    label: 'Minimax',
    value: 'minimax',
    defaultUrl: 'https://api.minimax.chat/v1',
    models: ['MiniMax-M2.7', 'MiniMax-M2.5', 'MiniMax-M2.1', 'abab6.5s-chat', 'abab6-chat']
  },
}

const providerOptions = computed(() => {
  return Object.values(providerConfig).map(p => ({ label: p.label, value: p.value }))
})

const currentModels = computed(() => {
  const config = providerConfig[formData.provider]
  if (!config) return []
  return config.models.map(m => ({ label: m, value: m }))
})

const filteredModels = ref<ModelOption[]>([])

const formData = reactive<Partial<AiConfig>>({
  displayName: '',
  provider: 'openai',
  model: '',
  baseUrl: 'https://api.openai.com/v1',
  apiKey: '',
  status: 1,
  remark: ''
})

watch(() => formData.provider, () => {
  filteredModels.value = currentModels.value
}, { immediate: true })

const filterModels = (query: string) => {
  if (!query) {
    filteredModels.value = currentModels.value
    return
  }
  const config = providerConfig[formData.provider]
  if (!config) return
  filteredModels.value = config.models
    .filter(m => m.toLowerCase().includes(query.toLowerCase()))
    .map(m => ({ label: m, value: m }))
  if (!filteredModels.value.find(m => m.value === query)) {
    filteredModels.value.unshift({ label: query, value: query })
  }
}

const getProviderLabel = (code: string) => {
  return providerConfig[code]?.label || code
}

const handleProviderChange = (provider: string) => {
  const config = providerConfig[provider]
  if (config) {
    formData.baseUrl = config.defaultUrl
    formData.model = config.models[0]
    filteredModels.value = currentModels.value
  }
}

const tableData = ref<AiConfig[]>([])
const loading = ref(false)
const testLoading = ref<number | null>(null)
const formTestLoading = ref(false)

const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const submitLoading = ref(false)

const formRules: FormRules = {
  displayName: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  provider: [{ required: true, message: '请选择供应商', trigger: 'change' }],
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getAiConfigPage(1, 100)
    tableData.value = res.records || []
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleCreate = () => {
  isEdit.value = false
  const config = providerConfig['openai']
  Object.assign(formData, {
    displayName: '',
    provider: 'openai',
    model: config.models[0],
    baseUrl: config.defaultUrl,
    apiKey: '',
    status: 1,
    remark: ''
  })
  nextTick(() => {
    filteredModels.value = currentModels.value
  })
  dialogVisible.value = true
}

const handleEdit = async (row: AiConfig) => {
  isEdit.value = true
  try {
    const fullConfig = await getAiConfigById(row.id!)
    Object.assign(formData, fullConfig)
    nextTick(() => {
      filteredModels.value = providerConfig[formData.provider]?.models.map(m => ({ label: m, value: m })) || []
    })
  } catch (e: any) {
    ElMessage.error('获取配置详情失败')
  }
  dialogVisible.value = true
}

const handleDelete = async (row: AiConfig) => {
  try {
    await ElMessageBox.confirm('确定删除该配置？', '提示', { type: 'warning' })
    await deleteAiConfig(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch {
    // cancel
  }
}

const handleTest = async (row: AiConfig) => {
  console.log('handleTest called, row:', row)
  testLoading.value = row.id!
  try {
    console.log('calling getAiConfigById...')
    const fullConfig = await getAiConfigById(row.id!)
    console.log('got config:', fullConfig)
    
    const testUrl = '/ai/test'
    console.log('sending request to:', testUrl)
    console.log('request data:', {
      provider: fullConfig.provider,
      apiKey: fullConfig.apiKey,
      apiUrl: fullConfig.baseUrl,
      model: fullConfig.model
    })
    
    const res = await request({
      url: testUrl,
      method: 'POST',
      data: {
        provider: fullConfig.provider,
        apiKey: fullConfig.apiKey,
        apiUrl: fullConfig.baseUrl,
        model: fullConfig.model
      }
    })
    console.log('request result:', res)
    ElMessage.success('连接成功')
  } catch (e: any) {
    console.log('=== Catch block entered ===')
    console.log('e:', e)
    console.log('e.constructor.name:', e.constructor?.name)
    console.log('e.message:', e.message)
    console.log('e.response:', e.response)
    console.log('e.response?.data:', e.response?.data)
    console.log('e.config:', e.config)
    console.log('e.config?.url:', e.config?.url)
    console.log('e.config?.baseURL:', e.config?.baseURL)
    console.log('e.isAxiosError:', e.isAxiosError)
    console.log('e.toString():', e.toString())
    console.log('JSON.stringify(e):', JSON.stringify(e, Object.getOwnPropertyNames(e)))
    
    ElMessage.error(e.message || '连接失败')
  } finally {
    testLoading.value = null
  }
}

const handleFormTest = async () => {
  formTestLoading.value = true
  try {
    const ok = await testAiConnection({
      provider: formData.provider,
      apiKey: formData.apiKey,
      apiUrl: formData.baseUrl,
      model: formData.model
    })
    ElMessage.success(ok ? '连接成功' : '连接失败')
  } catch (e: any) {
    ElMessage.error(e.message || '连接失败')
  } finally {
    formTestLoading.value = false
  }
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
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    displayName: '',
    provider: 'openai',
    model: '',
    baseUrl: 'https://api.openai.com/v1',
    apiKey: '',
    status: 1,
    remark: ''
  })
}

onMounted(loadData)
</script>

<style scoped>
.ai-config-management {
  padding: 20px;
}
.search-card {
  margin-bottom: 20px;
}
.table-card {
  margin-bottom: 20px;
}
</style>