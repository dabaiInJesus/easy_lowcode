<template>
  <div class="custom-node target-node" :style="{ borderColor: data?.color }">
    <div class="node-header" :style="{ background: data?.color }">
      <span class="node-icon">▣</span>
      <span class="node-label">{{ data?.label || '目标' }}</span>
      <el-button text size="small" class="delete-btn" @click.stop="$emit('delete')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>
    <div class="node-body">
      <div class="node-type">TARGET</div>
      <div class="node-subtype">{{ data?.nodeSubType }}</div>
    </div>
    <!-- 连接点 -->
    <Handle type="target" :position="Position.Left" />
  </div>
</template>

<script setup lang="ts">
import { Handle, Position } from '@vue-flow/core'
import { Close } from '@element-plus/icons-vue'

defineProps<{
  id: string
  data: any
}>()

defineEmits(['delete'])
</script>

<style scoped>
.custom-node {
  min-width: 140px;
  border: 2px solid #52c41a;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
}

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 6px 6px 0 0;
  color: #fff;
  font-size: 13px;
  font-weight: 500;
}

.node-icon {
  font-size: 12px;
}

.node-label {
  flex: 1;
}

.delete-btn {
  color: rgba(255, 255, 255, 0.8);
}

.node-body {
  padding: 8px 12px;
  font-size: 12px;
  color: #666;
}

.node-type {
  color: #999;
  font-size: 10px;
  text-transform: uppercase;
}

.node-subtype {
  color: #333;
  font-weight: 500;
}
</style>
