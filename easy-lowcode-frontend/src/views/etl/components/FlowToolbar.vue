<template>
  <div class="flow-toolbar">
    <div class="toolbar-left">
      <span class="flow-name">{{ flow?.flowName || '未命名流程' }}</span>
      <el-tag v-if="flow?.flowStatus" :type="statusType" size="small">
        {{ statusLabel }}
      </el-tag>
    </div>
    <div class="toolbar-right">
      <el-button size="small" @click="$emit('save')" :loading="saving">
        <el-icon><Check /></el-icon> 保存
      </el-button>
      <el-button size="small" type="success" @click="$emit('execute')"
        :disabled="flow?.flowStatus === 'RUNNING'">
        <el-icon><VideoPlay /></el-icon> 执行
      </el-button>
      <el-button size="small" type="danger" @click="$emit('stop')"
        :disabled="flow?.flowStatus !== 'RUNNING'">
        <el-icon><VideoPause /></el-icon> 停止
      </el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { Check, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import type { FlowDefinition } from '@/types/flow'

interface Props {
  flow?: FlowDefinition | null
  saving?: boolean
}

const props = defineProps<Props>()
defineEmits(['save', 'execute', 'stop'])

const statusType = computed(() => {
  switch (props.flow?.flowStatus) {
    case 'RUNNING': return 'warning'
    case 'COMPLETED': return 'success'
    case 'FAILED': return 'danger'
    default: return 'info'
  }
})

const statusLabel = computed(() => {
  const map: Record<string, string> = {
    DRAFT: '草稿',
    READY: '就绪',
    RUNNING: '运行中',
    COMPLETED: '已完成',
    FAILED: '失败',
  }
  return map[props.flow?.flowStatus || ''] || props.flow?.flowStatus
})
</script>

<style scoped>
.flow-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 16px;
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}

.toolbar-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.flow-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

.toolbar-right {
  display: flex;
  gap: 8px;
}
</style>
