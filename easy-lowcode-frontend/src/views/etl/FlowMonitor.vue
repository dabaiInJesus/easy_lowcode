<template>
  <div class="flow-monitor">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>流程监控</span>
          <div class="header-actions">
            <el-button @click="fetchData" :loading="loading">
              <el-icon><Refresh /></el-icon> 刷新
            </el-button>
          </div>
        </div>
      </template>

      <!-- 执行记录表格 -->
      <el-table :data="executions" border stripe v-loading="loading" empty-text="暂无执行记录">
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="flowName" label="流程名称" min-width="150" show-overflow-tooltip />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="getStatusType(row.execStatus)" size="small">
              {{ getStatusLabel(row.execStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="开始时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.startTime) }}
          </template>
        </el-table-column>
        <el-table-column label="结束时间" width="180">
          <template #default="{ row }">
            {{ formatTime(row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="耗时" width="100">
          <template #default="{ row }">
            {{ calcDuration(row.startTime, row.endTime) }}
          </template>
        </el-table-column>
        <el-table-column label="数据量" width="200">
          <template #default="{ row }">
            <div class="data-counts">
              <span>读取: <b>{{ row.readCount }}</b></span>
              <span>写入: <b>{{ row.writeCount }}</b></span>
              <span>跳过: <b>{{ row.skipCount }}</b></span>
              <span v-if="row.errorCount > 0" class="error-count">错误: <b>{{ row.errorCount }}</b></span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="showDetail(row)">
              <el-icon><View /></el-icon> 详情
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 执行详情对话框 -->
    <el-dialog v-model="detailVisible" title="执行详情" width="600px">
      <el-descriptions :column="2" border v-if="currentExecution">
        <el-descriptions-item label="流程名称">{{ currentExecution.flowName }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="getStatusType(currentExecution.execStatus)">
            {{ getStatusLabel(currentExecution.execStatus) }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="开始时间">{{ formatTime(currentExecution.startTime) }}</el-descriptions-item>
        <el-descriptions-item label="结束时间">{{ formatTime(currentExecution.endTime) }}</el-descriptions-item>
        <el-descriptions-item label="读取记录数">{{ currentExecution.readCount }}</el-descriptions-item>
        <el-descriptions-item label="写入记录数">{{ currentExecution.writeCount }}</el-descriptions-item>
        <el-descriptions-item label="跳过记录数">{{ currentExecution.skipCount }}</el-descriptions-item>
        <el-descriptions-item label="错误记录数">{{ currentExecution.errorCount }}</el-descriptions-item>
        <el-descriptions-item label="错误信息" :span="2">
          <pre v-if="currentExecution.errorMessage" class="error-log">{{ currentExecution.errorMessage }}</pre>
          <span v-else>无</span>
        </el-descriptions-item>
        <el-descriptions-item label="执行详情" :span="2">
          <pre v-if="currentExecution.execDetail" class="exec-detail">{{ formatJson(currentExecution.execDetail) }}</pre>
          <span v-else>无</span>
        </el-descriptions-item>
      </el-descriptions>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { Refresh, View } from '@element-plus/icons-vue'
import { getExecutionHistory, getExecutionDetail } from '@/api/flow'
import type { FlowExecution } from '@/types/flow'

const props = defineProps<{ flowId?: number }>()

const loading = ref(false)
const executions = ref<FlowExecution[]>([])
const detailVisible = ref(false)
const currentExecution = ref<FlowExecution | null>(null)
let refreshTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => {
  fetchData()
  // 自动刷新（5秒）
  refreshTimer = setInterval(fetchData, 5000)
})

onUnmounted(() => {
  if (refreshTimer) clearInterval(refreshTimer)
})

async function fetchData() {
  if (!props.flowId) return
  loading.value = true
  try {
    executions.value = await getExecutionHistory(props.flowId, 50)
  } finally {
    loading.value = false
  }
}

async function showDetail(row: FlowExecution) {
  if (row.id) {
    currentExecution.value = await getExecutionDetail(row.id)
  } else {
    currentExecution.value = row
  }
  detailVisible.value = true
}

function getStatusType(status: string) {
  const map: Record<string, string> = { RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', STOPPED: 'info' }
  return map[status] || 'info'
}

function getStatusLabel(status: string) {
  const map: Record<string, string> = { RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败', STOPPED: '已停止' }
  return map[status] || status
}

function formatTime(time?: string) {
  if (!time) return '-'
  return new Date(time).toLocaleString()
}

function calcDuration(start?: string, end?: string) {
  if (!start || !end) return '-'
  const ms = new Date(end).getTime() - new Date(start).getTime()
  if (ms < 1000) return ms + 'ms'
  if (ms < 60000) return (ms / 1000).toFixed(1) + 's'
  return (ms / 60000).toFixed(1) + 'min'
}

function formatJson(jsonStr: string) {
  try {
    return JSON.stringify(JSON.parse(jsonStr), null, 2)
  } catch {
    return jsonStr
  }
}
</script>

<style scoped>
.flow-monitor {
  padding: 16px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.data-counts {
  display: flex;
  gap: 8px;
  font-size: 12px;
}

.data-counts span b {
  color: #1890ff;
}

.error-count b {
  color: #ff4d4f !important;
}

.error-log {
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  padding: 8px;
  font-size: 12px;
  max-height: 200px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}

.exec-detail {
  background: #f6f6f6;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  padding: 8px;
  font-size: 12px;
  max-height: 300px;
  overflow-y: auto;
  white-space: pre-wrap;
  word-break: break-all;
  margin: 0;
}
</style>
