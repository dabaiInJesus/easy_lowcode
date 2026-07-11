<template>
  <div class="flow-toolbar">
    <div class="toolbar-left">
      <span class="flow-name">{{ workflow?.workflowName || '未命名工作流' }}</span>
      <el-tag v-if="workflow?.status" :type="workflow.status === 'PUBLISHED' ? 'success' : 'info'" size="small">
        {{ workflow.status === 'PUBLISHED' ? '已发布' : '草稿' }}
      </el-tag>
    </div>
    <div class="toolbar-right">
      <el-button size="small" @click="$emit('save')" :loading="saving">保存</el-button>
      <el-button size="small" type="warning" @click="$emit('publish')"
        :disabled="workflow?.status === 'PUBLISHED'">发布</el-button>
      <el-button size="small" type="success" @click="$emit('execute')"
        :disabled="executing || workflow?.status !== 'PUBLISHED'">执行</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { AiWorkflowDef } from '@/types/ai-workflow'

interface Props {
  workflow?: AiWorkflowDef | null
  saving?: boolean
  executing?: boolean
}

defineProps<Props>()
defineEmits(['save', 'publish', 'execute'])
</script>

<style scoped>
.flow-toolbar { display: flex; justify-content: space-between; align-items: center; padding: 8px 16px; background: #fff; border-bottom: 1px solid #e8e8e8; }
.toolbar-left { display: flex; align-items: center; gap: 12px; }
.flow-name { font-size: 14px; font-weight: 500; color: #333; }
.toolbar-right { display: flex; gap: 8px; }
</style>
