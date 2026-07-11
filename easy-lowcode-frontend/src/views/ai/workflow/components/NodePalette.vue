<template>
  <div class="node-palette">
    <div class="palette-title">节点面板</div>
    <div v-for="(category, key) in categories" :key="key" class="palette-section">
      <div class="section-title">{{ category.label }}</div>
      <div class="node-list">
        <div
          v-for="node in category.nodes"
          :key="node.type"
          class="palette-node"
          :style="{ borderLeftColor: node.color }"
          @click="$emit('add-node', node.type, node.label)"
        >
          <span class="node-icon">{{ node.icon }}</span>
          <span class="node-label">{{ node.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { WORKFLOW_NODE_TYPES, CATEGORY_LABELS } from '../workflowNodeTypes'

defineEmits<{ (e: 'add-node', type: string, label: string): void }>()

const categories = computed(() => {
  const result: Record<string, { label: string; nodes: typeof WORKFLOW_NODE_TYPES }> = {}
  for (const node of WORKFLOW_NODE_TYPES) {
    if (!result[node.category]) {
      result[node.category] = { label: CATEGORY_LABELS[node.category] || node.category, nodes: [] }
    }
    result[node.category].nodes.push(node)
  }
  return result
})
</script>

<style scoped>
.node-palette { width: 200px; background: #fff; border-right: 1px solid #e8e8e8; overflow-y: auto; padding: 12px; }
.palette-title { font-size: 14px; font-weight: 600; color: #333; margin-bottom: 16px; }
.palette-section { margin-bottom: 16px; }
.section-title { font-size: 12px; color: #999; margin-bottom: 8px; }
.node-list { display: flex; flex-direction: column; gap: 6px; }
.palette-node { display: flex; align-items: center; gap: 8px; padding: 8px 12px; background: #fafafa; border: 1px solid #e8e8e8; border-left: 3px solid #1890ff; border-radius: 4px; cursor: pointer; transition: all 0.2s; font-size: 13px; }
.palette-node:hover { background: #e6f7ff; border-color: #91d5ff; }
.node-icon { font-size: 14px; }
.node-label { color: #333; }
</style>
