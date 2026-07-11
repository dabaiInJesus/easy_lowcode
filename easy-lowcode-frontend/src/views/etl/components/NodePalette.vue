<template>
  <div class="node-palette">
    <div class="palette-title">节点面板</div>

    <!-- 数据源 -->
    <div class="palette-section">
      <div class="section-title">数据源</div>
      <div class="node-list">
        <div
          v-for="node in sourceNodes"
          :key="node.type"
          class="palette-node"
          :style="{ borderLeftColor: node.color }"
          draggable="true"
          @dragstart="onDragStart(node)"
        >
          <span class="node-label">{{ node.label }}</span>
        </div>
      </div>
    </div>

    <!-- 转换 -->
    <div class="palette-section">
      <div class="section-title">转换</div>
      <div class="node-list">
        <div
          v-for="node in transformNodes"
          :key="node.type"
          class="palette-node"
          :style="{ borderLeftColor: node.color }"
          draggable="true"
          @dragstart="onDragStart(node)"
        >
          <span class="node-label">{{ node.label }}</span>
        </div>
      </div>
    </div>

    <!-- 目标 -->
    <div class="palette-section">
      <div class="section-title">目标</div>
      <div class="node-list">
        <div
          v-for="node in targetNodes"
          :key="node.type"
          class="palette-node"
          :style="{ borderLeftColor: node.color }"
          draggable="true"
          @dragstart="onDragStart(node)"
        >
          <span class="node-label">{{ node.label }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { getNodesByCategory } from './nodeTypes'
import type { NodeTypeDefinition } from '@/types/flow'

const emit = defineEmits<{
  (e: 'add-node', type: string, category: string, label: string): void
}>()

const sourceNodes = getNodesByCategory('SOURCE')
const transformNodes = getNodesByCategory('TRANSFORM')
const targetNodes = getNodesByCategory('TARGET')

function onDragStart(node: NodeTypeDefinition) {
  // 点击添加（简化版，后续可实现拖拽）
  emit('add-node', node.type, node.category, node.label)
}
</script>

<style scoped>
.node-palette {
  width: 200px;
  background: #fff;
  border-right: 1px solid #e8e8e8;
  overflow-y: auto;
  padding: 12px;
}

.palette-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.palette-section {
  margin-bottom: 16px;
}

.section-title {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
  text-transform: uppercase;
}

.node-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.palette-node {
  padding: 8px 12px;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  border-left: 3px solid #1890ff;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
}

.palette-node:hover {
  background: #e6f7ff;
  border-color: #91d5ff;
}

.node-label {
  color: #333;
}
</style>
