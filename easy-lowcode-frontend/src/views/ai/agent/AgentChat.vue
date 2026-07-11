<template>
  <div class="agent-chat">
    <el-card class="chat-card">
      <div class="chat-header">
        <div class="header-left">
          <el-button text @click="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
          </el-button>
          <h3>{{ agent?.agentName || 'AI 智能体' }}</h3>
          <el-tag v-if="agent" size="small" type="info">{{ agent.provider }} / {{ agent.model }}</el-tag>
        </div>
        <div class="header-right">
          <el-button size="small" @click="clearChat">清空对话</el-button>
        </div>
      </div>

      <!-- 对话消息区 -->
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <el-icon :size="48"><User /></el-icon>
          <p>{{ agent?.openingStatement || '您好！我是 AI 智能体，有什么可以帮助您的？' }}</p>
          <div class="suggestion-list" v-if="suggestions.length > 0">
            <el-tag v-for="s in suggestions" :key="s" @click="sendMessage(s)" class="suggestion-tag" effect="plain">
              {{ s }}
            </el-tag>
          </div>
        </div>

        <!-- 消息列表 -->
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
          <!-- 思考过程 -->
          <div v-if="msg.type === 'thought'" class="thought-block">
            <div class="thought-label">
              <el-icon><Loading v-if="msg.loading" /></el-icon>
              <span>思考过程</span>
            </div>
            <div class="thought-content">{{ msg.content }}</div>
          </div>

          <!-- 工具调用 -->
          <div v-else-if="msg.type === 'tool_start'" class="tool-block">
            <div class="tool-label">
              <el-icon><SetUp /></el-icon>
              <span>调用工具: {{ msg.toolName }}</span>
            </div>
            <div class="tool-params">{{ formatJson(msg.toolInput) }}</div>
          </div>

          <!-- 工具结果 -->
          <div v-else-if="msg.type === 'tool_result'" class="tool-result-block">
            <div class="tool-label">
              <el-icon><CircleCheck /></el-icon>
              <span>工具结果: {{ msg.toolName }}</span>
            </div>
            <div class="tool-result">{{ formatJson(msg.toolOutput) }}</div>
          </div>

          <!-- 普通消息 -->
          <div v-else class="message-content">{{ msg.content }}</div>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="message assistant">
          <div class="message-content thinking"><span class="dot-pulse" /></div>
        </div>
      </div>

      <!-- 输入区 -->
      <div class="chat-input">
        <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入您的问题..."
          @keydown.enter.exact.prevent="sendMessage()" :disabled="loading" />
        <el-button type="primary" @click="sendMessage()" :loading="loading" :disabled="!inputText.trim()">
          发送
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, User, Loading, SetUp, CircleCheck } from '@element-plus/icons-vue'
import { getAgentHistory, clearAgentSession } from '@/api/ai'

const route = useRoute()
const agentCode = computed(() => route.params.agentCode as string)

const agent = ref<any>(null)
const messages = ref<any[]>([])
const inputText = ref('')
const loading = ref(false)
const messagesRef = ref<HTMLElement>()

const suggestions = computed(() => {
  if (!agent.value?.suggestedQuestions) return []
  return agent.value.suggestedQuestions.split('\n').filter((s: string) => s.trim())
})

onMounted(async () => {
  // 加载 Agent 信息
  try {
    const list = await import('@/api/ai').then(m => m.getAgentList())
    agent.value = list.find((a: any) => a.agentCode === agentCode.value)
  } catch { /* 静默 */ }

  // 加载历史消息
  try {
    const history = await getAgentHistory(agentCode.value)
    if (history && history.length > 0) {
      messages.value = history.map((h: any) => ({
        role: h.role,
        content: h.content,
        type: h.role === 'tool' ? 'tool_result' : 'message',
      }))
    }
  } catch { /* 静默 */ }

  scrollToBottom()
})

function scrollToBottom() {
  nextTick(() => {
    if (messagesRef.value) {
      messagesRef.value.scrollTop = messagesRef.value.scrollHeight
    }
  })
}

function formatJson(obj: any): string {
  if (!obj) return ''
  try {
    return JSON.stringify(typeof obj === 'string' ? JSON.parse(obj) : obj, null, 2)
  } catch {
    return String(obj)
  }
}

async function sendMessage(text?: string) {
  const msg = text || inputText.value.trim()
  if (!msg || loading.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content: msg, type: 'message' })
  inputText.value = ''
  loading.value = true
  scrollToBottom()

  try {
    // 使用 SSE 连接
    const response = await fetch(`/api/ai/agent/${agentCode.value}/chat`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${sessionStorage.getItem('token') || ''}`,
      },
      body: JSON.stringify({ message: msg, sessionId: Date.now().toString() }),
    })

    const reader = response.body?.getReader()
    const decoder = new TextDecoder()
    let buffer = ''

    if (reader) {
      while (true) {
        const { done, value } = await reader.read()
        if (done) break

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data:')) {
            try {
              const eventData = JSON.parse(line.substring(5).trim())
              handleSSEEvent(eventData)
            } catch { /* 忽略解析错误 */ }
          }
        }
      }
    }
  } catch (e: any) {
    ElMessage.error('对话失败: ' + (e.message || '未知错误'))
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

function handleSSEEvent(event: any) {
  const type = event.type
  const data = event.data

  switch (type) {
    case 'thought':
      messages.value.push({
        role: 'assistant',
        content: data.content,
        type: 'thought',
        iteration: data.iteration,
      })
      break
    case 'tool_start':
      messages.value.push({
        role: 'tool',
        toolName: data.toolName,
        toolInput: data.params,
        type: 'tool_start',
      })
      break
    case 'tool_result':
      messages.value.push({
        role: 'tool',
        toolName: data.toolName,
        toolOutput: data.result,
        type: 'tool_result',
      })
      break
    case 'response':
      messages.value.push({
        role: 'assistant',
        content: data.content,
        type: 'message',
        iteration: data.iteration,
      })
      break
    case 'error':
      ElMessage.error(data.error || '执行出错')
      break
  }
  scrollToBottom()
}

async function clearChat() {
  try {
    await clearAgentSession(agentCode.value)
    messages.value = []
    ElMessage.success('对话已清空')
  } catch { /* 错误已由拦截器处理 */ }
}
</script>

<style scoped>
.agent-chat { padding: 20px; height: calc(100vh - 120px); display: flex; flex-direction: column; }
.chat-card { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
.chat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; flex-shrink: 0; }
.header-left { display: flex; align-items: center; gap: 8px; }
.header-left h3 { margin: 0; font-size: 16px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px 0; }
.chat-empty { text-align: center; padding: 60px 20px; color: #909399; }
.chat-empty p { margin: 12px 0; }
.suggestion-list { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.suggestion-tag { cursor: pointer; }

.message { margin-bottom: 16px; }
.message.user { text-align: right; }
.message.user .message-content { display: inline-block; background: #409eff; color: #fff; padding: 10px 16px; border-radius: 12px 12px 4px 12px; max-width: 70%; text-align: left; white-space: pre-wrap; }
.message.assistant .message-content { display: inline-block; background: #f0f2f5; padding: 10px 16px; border-radius: 12px 12px 12px 4px; max-width: 70%; text-align: left; white-space: pre-wrap; }

.thought-block { background: #fff7e6; border: 1px solid #ffd591; border-radius: 8px; padding: 12px; margin-bottom: 8px; }
.thought-label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #fa8c16; margin-bottom: 6px; }
.thought-content { font-size: 13px; color: #595959; white-space: pre-wrap; }

.tool-block { background: #e6f7ff; border: 1px solid #91d5ff; border-radius: 8px; padding: 12px; margin-bottom: 8px; }
.tool-result-block { background: #f6ffed; border: 1px solid #b7eb8f; border-radius: 8px; padding: 12px; margin-bottom: 8px; }
.tool-label { display: flex; align-items: center; gap: 6px; font-size: 12px; color: #1890ff; margin-bottom: 6px; }
.tool-result-block .tool-label { color: #52c41a; }
.tool-params, .tool-result { font-size: 12px; font-family: monospace; background: #fafafa; padding: 8px; border-radius: 4px; white-space: pre-wrap; max-height: 200px; overflow-y: auto; }

.chat-input { display: flex; gap: 12px; align-items: flex-end; margin-top: 16px; flex-shrink: 0; }
.chat-input .el-textarea { flex: 1; }
.thinking { min-width: 60px; }
.dot-pulse::after { content: '...'; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100% { opacity: 0.3; } 50% { opacity: 1; } }
</style>
