<template>
  <div class="chat-container">
    <el-card class="chat-card">
      <div class="chat-header">
        <h3>AI 智能对话</h3>
        <div class="header-actions">
          <el-select v-model="selectedProvider" placeholder="选择AI供应商" size="small" style="width:160px" @change="loadProviders">
            <el-option v-for="p in providers" :key="p.name" :label="p.label" :value="p.name" />
          </el-select>
          <el-button size="small" @click="clearChat">清空对话</el-button>
        </div>
      </div>
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <el-icon :size="48"><ChatLineRound /></el-icon>
          <p>您好！我是 AI 助手，有什么可以帮助您的？</p>
          <div class="suggestion-list">
            <el-tag v-for="s in suggestions" :key="s" @click="sendMessage(s)" class="suggestion-tag" effect="plain">{{ s }}</el-tag>
          </div>
        </div>
        <div v-for="(msg, i) in messages" :key="i" :class="['message', msg.role]">
          <div class="message-content">{{ msg.content }}</div>
        </div>
        <div v-if="loading" class="message assistant">
          <div class="message-content thinking"><span class="dot-pulse" /></div>
        </div>
      </div>
      <div class="chat-input">
        <el-input v-model="inputText" type="textarea" :rows="3" placeholder="输入您的问题..." @keydown.enter.exact.prevent="sendMessage()" />
        <el-button type="primary" @click="sendMessage()" :loading="loading">发送</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatLineRound } from '@element-plus/icons-vue'
import { chat, getProviders } from '@/api/ai'

const messages = ref<{role:string;content:string}[]>([])
const inputText = ref('')
const loading = ref(false)
const selectedProvider = ref('')
const providers = ref<{name:string;label:string;enabled:boolean}[]>([])
const messagesRef = ref<HTMLElement>()

const suggestions = ['帮我生成一段SQL查询语句', '解释什么是低代码平台', '写一个Python排序算法']

const loadProviders = async () => {
  try {
    providers.value = await getProviders()
  } catch { /* ignore */ }
}

const clearChat = () => {
  messages.value = []
  inputText.value = ''
}

const sendMessage = async (text?: string) => {
  const content = text || inputText.value
  if (!content.trim()) return
  inputText.value = ''
  messages.value.push({ role: 'user', content })
  loading.value = true
  try {
    const res = await chat({ message: content, history: messages.value.slice(-10), provider: selectedProvider.value || undefined })
    messages.value.push({ role: 'assistant', content: res || '未获取到回复' })
    await nextTick()
    messagesRef.value?.scrollTo({ top: messagesRef.value.scrollHeight, behavior: 'smooth' })
  } catch (e: any) {
    ElMessage.error(e.message || '请求失败')
    messages.value.push({ role: 'assistant', content: '抱歉，请求出错了，请稍后重试。' })
  } finally {
    loading.value = false
  }
}

onMounted(loadProviders)
</script>

<style scoped>
.chat-container { padding: 20px; max-width: 800px; margin: 0 auto; }
.chat-card { min-height: 600px; display: flex; flex-direction: column; }
.chat-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16px; }
.chat-header h3 { margin: 0; }
.header-actions { display: flex; gap: 8px; }
.chat-messages { flex: 1; overflow-y: auto; padding: 16px 0; min-height: 400px; max-height: 500px; }
.chat-empty { text-align: center; padding: 60px 20px; color: #909399; }
.chat-empty p { margin: 12px 0; }
.suggestion-list { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; }
.suggestion-tag { cursor: pointer; }
.message { margin-bottom: 16px; }
.message.user { text-align: right; }
.message.user .message-content { display: inline-block; background: #409eff; color: #fff; padding: 10px 16px; border-radius: 12px 12px 4px 12px; max-width: 70%; text-align: left; white-space: pre-wrap; }
.message.assistant .message-content { display: inline-block; background: #f0f2f5; padding: 10px 16px; border-radius: 12px 12px 12px 4px; max-width: 70%; text-align: left; white-space: pre-wrap; }
.chat-input { display: flex; gap: 12px; align-items: flex-end; margin-top: 16px; }
.chat-input .el-textarea { flex: 1; }
.thinking { min-width: 60px; }
.dot-pulse::after { content: '...'; animation: pulse 1.5s infinite; }
@keyframes pulse { 0%,100% { opacity: 0.3; } 50% { opacity: 1; } }
</style>
