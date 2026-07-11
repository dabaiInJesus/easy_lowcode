<template>
  <div class="workflow-node" :style="{ borderColor: color }">
    <div class="node-header" :style="{ background: color }">
      <span class="node-icon">{{ icon }}</span>
      <span class="node-label">{{ data?.label || '节点' }}</span>
      <el-button text size="small" class="delete-btn" @click.stop="$emit('delete')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    <div class="node-body">
      <div class="node-type">{{ data?.nodeType }}</div>
    </div>
    <Handle v-if="data?.nodeType !== 'start'" type="target" :position="Position.Left" />
    <Handle v-if="data?.nodeType !== 'end'" type="source" :position="Position.Right" />
  </div>
</template>

<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import { Close } from '@element-plus/icons-vue'

defineProps<{
  id: string
  data: any
  color: string
  icon: string
}>()

defineEmits(['delete'])
</script>

<style scoped>
.workflow-node { min-width: 140px; border: 2px solid #1890ff; border-radius: 8px; background: #fff; box-shadow: 0 2px 8px rgba(0,0,0,0.1); }
.node-header { display: flex; align-items: center; gap: 6px; padding: 8px 12px; border-radius: 6px 6px 0 0; color: #fff; font-size: 13px; font-weight: 500; }
.node-icon { font-size: 14px; }
.node-label { flex: 1; }
.delete-btn { color: rgba(255,255,255,0.8); }
.node-body { padding: 6px 12px; font-size: 11px; color: #999; }
</style>
