<template>
  <div class="agent-management">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>AI 智能体</span>
          <el-button type="primary" @click="handleAdd"><el-icon><Plus /></el-icon> 新建智能体</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="agentName" label="名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="agentCode" label="编码" min-width="120" show-overflow-tooltip />
        <el-table-column label="模型" width="150">
          <template #default="{ row }">
            <el-tag size="small">{{ row.provider }} / {{ row.model }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="工具" width="100" align="center">
          <template #default="{ row }">
            <el-tag v-if="row.toolsConfig" type="success" size="small">已配置</el-tag>
            <el-tag v-else type="info" size="small">无</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.publishStatus === 1 ? '已发布' : '草稿' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="usageCount" label="使用次数" width="100" align="center" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="handleChat(row)">对话</el-button>
            <el-button text type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button text type="success" size="small" @click="handlePublish(row)"
              :disabled="row.publishStatus === 1">发布</el-button>
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑智能体' : '新建智能体'" width="700px" top="5vh">
      <el-tabs v-model="activeTab">
        <el-tab-pane label="基本信息" name="basic">
          <el-form :model="formData" label-width="100px" style="margin-top: 16px">
            <el-form-item label="名称" required>
              <el-input v-model="formData.agentName" placeholder="智能体名称" />
            </el-form-item>
            <el-form-item label="编码" required>
              <el-input v-model="formData.agentCode" placeholder="英文编码（唯一）" :disabled="isEdit" />
            </el-form-item>
            <el-form-item label="描述">
              <el-input v-model="formData.description" type="textarea" :rows="2" />
            </el-form-item>
            <el-form-item label="AI 供应商" required>
              <el-select v-model="formData.provider" placeholder="选择供应商">
                <el-option label="OpenAI" value="openai" />
                <el-option label="DashScope" value="dashscope" />
                <el-option label="DeepSeek" value="deepseek" />
                <el-option label="MiniMax" value="minimax" />
                <el-option label="Ollama" value="ollama" />
              </el-select>
            </el-form-item>
            <el-form-item label="模型" required>
              <el-input v-model="formData.model" placeholder="gpt-4 / qwen-turbo" />
            </el-form-item>
            <el-form-item label="温度">
              <el-slider v-model="formData.temperature" :min="0" :max="2" :step="0.1" show-input />
            </el-form-item>
            <el-form-item label="最大 Token">
              <el-input-number v-model="formData.maxTokens" :min="100" :max="8000" :step="100" />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="提示词" name="prompt">
          <el-form label-width="100px" style="margin-top: 16px">
            <el-form-item label="系统提示词">
              <el-input v-model="formData.instructions" type="textarea" :rows="10"
                placeholder="定义智能体的角色、能力和行为规则..." />
            </el-form-item>
            <el-form-item label="开场白">
              <el-input v-model="formData.openingStatement" type="textarea" :rows="3"
                placeholder="用户首次对话时显示的欢迎语" />
            </el-form-item>
            <el-form-item label="建议问题">
              <el-input v-model="formData.suggestedQuestions" type="textarea" :rows="3"
                placeholder='每行一个问题，如：\n帮我查询用户数据\n解释系统架构' />
            </el-form-item>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="工具配置" name="tools">
          <div style="margin-top: 16px">
            <p style="color: #909399; margin-bottom: 12px">选择智能体可以使用的工具：</p>
            <el-checkbox-group v-model="selectedTools">
              <div v-for="tool in availableTools" :key="tool.name" class="tool-item">
                <el-checkbox :label="tool.name">
                  <span style="font-weight: 500">{{ tool.name }}</span>
                  <span style="color: #909399; margin-left: 8px">{{ tool.description }}</span>
                </el-checkbox>
              </div>
            </el-checkbox-group>
          </div>
        </el-tab-pane>

        <el-tab-pane label="高级配置" name="advanced">
          <el-form label-width="120px" style="margin-top: 16px">
            <el-form-item label="最大执行轮次">
              <el-input-number v-model="formData.maxIterations" :min="1" :max="50" />
              <span style="color: #909399; margin-left: 8px">ReAct 循环最大次数</span>
            </el-form-item>
            <el-form-item label="启用自主规划">
              <el-switch v-model="formData.enablePlanning" :active-value="1" :inactive-value="0" />
              <span style="color: #909399; margin-left: 8px">允许 Agent 自主规划执行步骤</span>
            </el-form-item>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitting">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getAgentPage, createAgent, updateAgent, deleteAgent, publishAgent, getAgentTools } from '@/api/ai-agent'

const router = useRouter()
const loading = ref(false)
const tableData = ref<any[]>([])
const pagination = reactive({ current: 1, size: 10, total: 0 })
const dialogVisible = ref(false)
const submitting = ref(false)
const isEdit = ref(false)
const activeTab = ref('basic')
const selectedTools = ref<string[]>([])
const availableTools = ref<any[]>([])

const formData = reactive({
  id: 0,
  agentName: '',
  agentCode: '',
  description: '',
  provider: 'openai',
  model: 'gpt-4',
  temperature: 0.7,
  maxTokens: 2000,
  instructions: '',
  openingStatement: '',
  suggestedQuestions: '',
  toolsConfig: '',
  maxIterations: 10,
  enablePlanning: 0,
})

onMounted(() => {
  fetchData()
  loadTools()
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getAgentPage(pagination.current, pagination.size)
    tableData.value = res.records
    pagination.total = res.total
  } finally { loading.value = false }
}

async function loadTools() {
  try {
    availableTools.value = await getAgentTools()
  } catch { /* 静默失败 */ }
}

function handleAdd() {
  isEdit.value = false
  activeTab.value = 'basic'
  Object.assign(formData, {
    id: 0, agentName: '', agentCode: '', description: '', provider: 'openai',
    model: 'gpt-4', temperature: 0.7, maxTokens: 2000, instructions: '',
    openingStatement: '', suggestedQuestions: '', toolsConfig: '', maxIterations: 10, enablePlanning: 0,
  })
  selectedTools.value = []
  dialogVisible.value = true
}

function handleEdit(row: any) {
  isEdit.value = true
  activeTab.value = 'basic'
  Object.assign(formData, row)
  selectedTools.value = row.toolsConfig ? JSON.parse(row.toolsConfig) : []
  dialogVisible.value = true
}

function handleChat(row: any) {
  router.push(`/ai/agent/chat/${row.agentCode}`)
}

async function handleSubmit() {
  if (!formData.agentName || !formData.agentCode) { ElMessage.warning('请填写必填项'); return }
  submitting.value = true
  try {
    formData.toolsConfig = JSON.stringify(selectedTools.value)
    if (isEdit.value) {
      await updateAgent(formData)
      ElMessage.success('更新成功')
    } else {
      await createAgent(formData)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    fetchData()
  } finally { submitting.value = false }
}

async function handlePublish(row: any) {
  try {
    await publishAgent(row.id)
    ElMessage.success('发布成功')
    fetchData()
  } catch { /* 错误已由拦截器处理 */ }
}

async function handleDelete(row: any) {
  await ElMessageBox.confirm(`确定删除智能体「${row.agentName}」？`, '确认删除', { type: 'warning' })
  await deleteAgent(row.id)
  ElMessage.success('删除成功')
  fetchData()
}
</script>

<style scoped>
.agent-management { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tool-item { padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.tool-item:last-child { border-bottom: none; }
</style>
