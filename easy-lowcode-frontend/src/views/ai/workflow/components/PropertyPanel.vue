<template>
  <div class="property-panel">
    <div class="panel-header">
      <span class="panel-title">节点配置</span>
      <el-button text @click="$emit('close')"><el-icon><Close /></el-icon></el-button>
    </div>
    <div class="panel-body" v-if="node">
      <el-form label-position="top" size="small">
        <el-form-item label="节点名称">
          <el-input v-model="nodeName" @change="updateConfig" />
        </el-form-item>
        <el-form-item label="节点类型">
          <el-tag :color="getNodeTypeColor(node.type)" effect="dark" size="small">
            {{ node.data?.label || node.type }}
          </el-tag>
        </el-form-item>
      </el-form>

      <!-- LLM 节点配置 -->
      <template v-if="node.type === 'llm'">
        <el-divider content-position="left">LLM 配置</el-divider>
        <el-form label-position="top" size="small">
          <el-form-item label="AI 供应商">
            <el-select v-model="nodeConfig.provider" @change="emitConfig">
              <el-option label="OpenAI" value="openai" />
              <el-option label="DashScope" value="dashscope" />
              <el-option label="DeepSeek" value="deepseek" />
              <el-option label="MiniMax" value="minimax" />
            </el-select>
          </el-form-item>
          <el-form-item label="模型">
            <el-input v-model="nodeConfig.model" placeholder="gpt-4" @change="emitConfig" />
          </el-form-item>
          <el-form-item label="Prompt 模板">
            <el-input v-model="nodeConfig.prompt" type="textarea" :rows="5"
              placeholder="使用 {{variable}} 引用变量" @change="emitConfig" />
          </el-form-item>
          <el-form-item label="最大 Token">
            <el-input-number v-model="nodeConfig.maxTokens" :min="100" :max="8000" @change="emitConfig" />
          </el-form-item>
        </el-form>
      </template>

      <!-- 代码节点配置 -->
      <template v-if="node.type === 'code'">
        <el-divider content-position="left">代码配置</el-divider>
        <el-form label-position="top" size="small">
          <el-form-item label="语言">
            <el-select v-model="nodeConfig.language" @change="emitConfig">
              <el-option label="JavaScript" value="javascript" />
            </el-select>
          </el-form-item>
          <el-form-item label="代码">
            <el-input v-model="nodeConfig.code" type="textarea" :rows="8"
              placeholder="通过 input 变量访问上游输出" @change="emitConfig" />
          </el-form-item>
        </el-form>
      </template>

      <!-- HTTP 节点配置 -->
      <template v-if="node.type === 'http'">
        <el-divider content-position="left">HTTP 配置</el-divider>
        <el-form label-position="top" size="small">
          <el-form-item label="URL">
            <el-input v-model="nodeConfig.url" placeholder="https://api.example.com" @change="emitConfig" />
          </el-form-item>
          <el-form-item label="方法">
            <el-select v-model="nodeConfig.method" @change="emitConfig">
              <el-option label="GET" value="GET" />
              <el-option label="POST" value="POST" />
              <el-option label="PUT" value="PUT" />
              <el-option label="DELETE" value="DELETE" />
            </el-select>
          </el-form-item>
          <el-form-item label="请求体 (JSON)">
            <el-input v-model="nodeConfig.bodyStr" type="textarea" :rows="4"
              placeholder='{"key": "value"}' @change="parseBody" />
          </el-form-item>
        </el-form>
      </template>

      <!-- 条件节点配置 -->
      <template v-if="node.type === 'condition'">
        <el-divider content-position="left">条件配置</el-divider>
        <el-form label-position="top" size="small">
          <el-form-item label="变量名">
            <el-input v-model="nodeConfig.variable" placeholder="变量名" @change="emitConfig" />
          </el-form-item>
          <el-form-item label="操作符">
            <el-select v-model="nodeConfig.operator" @change="emitConfig">
              <el-option label="等于" value="eq" />
              <el-option label="不等于" value="neq" />
              <el-option label="大于" value="gt" />
              <el-option label="小于" value="lt" />
              <el-option label="包含" value="contains" />
              <el-option label="为空" value="isEmpty" />
              <el-option label="不为空" value="isNotEmpty" />
              <el-option label="为真" value="isTrue" />
              <el-option label="为假" value="isFalse" />
            </el-select>
          </el-form-item>
          <el-form-item label="比较值">
            <el-input v-model="nodeConfig.value" placeholder="值" @change="emitConfig" />
          </el-form-item>
        </el-form>
      </template>

      <!-- 变量节点配置 -->
      <template v-if="node.type === 'variable'">
        <el-divider content-position="left">变量配置</el-divider>
        <el-form label-position="top" size="small">
          <el-form-item label="变量赋值">
            <div v-for="(val, key) in (nodeConfig.assignments || {})" :key="String(key)" class="var-row">
              <el-input :model-value="String(key)" size="small" disabled style="width: 100px" />
              <span style="margin: 0 4px">=</span>
              <el-input :model-value="val" size="small" placeholder="值 (用 {{var}} 引用)"
                @update:model-value="(v: string) => { nodeConfig.assignments[key] = v; emitConfig() }" />
            </div>
            <el-button size="small" @click="addAssignment">+ 添加变量</el-button>
          </el-form-item>
        </el-form>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { Close } from '@element-plus/icons-vue'

interface Props { node: any }
const props = defineProps<Props>()
const emit = defineEmits<{ (e: 'update', nodeId: string, config: Record<string, any>): void; (e: 'close'): void }>()

const nodeName = ref('')
const nodeConfig = reactive<Record<string, any>>({})

function getNodeTypeColor(type: string) {
  const colors: Record<string, string> = { start: '#52c41a', llm: '#1890ff', code: '#13c2c2', http: '#eb2f96', condition: '#faad14', variable: '#722ed1', end: '#ff4d4f' }
  return colors[type] || '#1890ff'
}

function initConfig() {
  if (props.node) {
    nodeName.value = props.node.data?.label || ''
    Object.assign(nodeConfig, props.node.data?.config || {})
  }
}

initConfig()

function updateConfig() {
  if (props.node) props.node.data = { ...props.node.data, label: nodeName.value }
}

function emitConfig() {
  if (props.node) emit('update', props.node.id, { ...nodeConfig })
}

function parseBody() {
  try {
    nodeConfig.body = nodeConfig.bodyStr ? JSON.parse(nodeConfig.bodyStr) : null
  } catch { /* ignore */ }
  emitConfig()
}

function addAssignment() {
  if (!nodeConfig.assignments) nodeConfig.assignments = {}
  const key = `var_${Object.keys(nodeConfig.assignments).length + 1}`
  nodeConfig.assignments[key] = ''
  emitConfig()
}
</script>

<style scoped>
.property-panel { width: 320px; background: #fff; border-left: 1px solid #e8e8e8; overflow-y: auto; }
.panel-header { display: flex; justify-content: space-between; align-items: center; padding: 12px 16px; border-bottom: 1px solid #e8e8e8; }
.panel-title { font-size: 14px; font-weight: 600; color: #333; }
.panel-body { padding: 16px; }
</style>
