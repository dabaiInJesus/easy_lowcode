<template>
  <div class="flow-canvas-container">
    <!-- 左侧节点面板 -->
    <NodePalette @add-node="addNode" />

    <!-- 中间画布 -->
    <div class="canvas-wrapper">
      <FlowToolbar
        :flow="flow"
        :saving="saving"
        @save="handleSave"
        @execute="handleExecute"
        @stop="handleStop"
      />
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        :default-viewport="{ zoom: 1, x: 0, y: 0 }"
        :snap-to-grid="true"
        :snap-grid="[20, 20]"
        fit-view-on-init
        class="flow-canvas"
        @node-click="onNodeClick"
        @connect="onConnect"
      >
        <Background />
        <Controls />
        <MiniMap />

        <!-- 自定义节点模板 -->
        <template #node-source="nodeProps">
          <SourceNode v-bind="nodeProps" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-transform="nodeProps">
          <TransformNode v-bind="nodeProps" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-target="nodeProps">
          <TargetNode v-bind="nodeProps" @delete="deleteNode(nodeProps.id)" />
        </template>
      </VueFlow>
    </div>

    <!-- 右侧属性面板 -->
    <PropertyPanel
      v-if="selectedNode"
      :node="selectedNode"
      @update="updateNodeConfig"
      @close="selectedNode = null"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { VueFlow, type Node, type Edge, type Connection } from '@vue-flow/core'
// vue-flow 样式：缺失时节点不按 absolute 定位渲染（块级撑满画布宽度）、连线/控件/小地图全部错乱
import '@vue-flow/core/dist/style.css'
import '@vue-flow/core/dist/theme-default.css'
import '@vue-flow/controls/dist/style.css'
import '@vue-flow/minimap/dist/style.css'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'
import NodePalette from './NodePalette.vue'
import FlowToolbar from './FlowToolbar.vue'
import PropertyPanel from './PropertyPanel.vue'
import SourceNode from './nodes/SourceNode.vue'
import TransformNode from './nodes/TransformNode.vue'
import TargetNode from './nodes/TargetNode.vue'
import { generateNodeId, getNodeType } from './nodeTypes'
import { getFlowDetail, updateFlow, executeFlow, stopFlow } from '@/api/flow'
import type { FlowDefinition } from '@/types/flow'

interface Props {
  flowId?: number | string  // 雪花 ID 需以字符串传递（超 JS Number 安全范围）
}

const props = defineProps<Props>()

const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const selectedNode = ref<Node | null>(null)
const saving = ref(false)
const flow = ref<FlowDefinition | null>(null)

// 加载流程详情
onMounted(async () => {
  if (props.flowId) {
    try {
      const detail = await getFlowDetail(props.flowId)
      flow.value = detail
      if (detail.nodesJson) {
        nodes.value = JSON.parse(detail.nodesJson)
      }
      if (detail.edgesJson) {
        edges.value = JSON.parse(detail.edgesJson)
      }
    } catch (e) {
      ElMessage.error('加载流程失败')
    }
  }
})

// 添加节点
function addNode(nodeType: string, category: string, label: string) {
  const typeDef = getNodeType(nodeType, category)
  if (!typeDef) return

  const newNode: Node = {
    id: generateNodeId(),
    type: category.toLowerCase(),
    position: { x: 250 + nodes.value.length * 50, y: 100 + nodes.value.length * 30 },
    data: {
      nodeType: category,
      nodeSubType: nodeType,
      label: label,
      color: typeDef.color,
      config: {},
    },
  }
  nodes.value.push(newNode)
}

// 选中节点
function onNodeClick(event: any) {
  selectedNode.value = event.node
}

// 连线
function onConnect(connection: Connection) {
  const newEdge: Edge = {
    id: `e-${connection.source}-${connection.target}`,
    source: connection.source!,
    target: connection.target!,
    animated: true,
    style: { stroke: '#1890ff' },
  }
  edges.value.push(newEdge)
}

// 删除节点
function deleteNode(nodeId: string) {
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
  if (selectedNode.value?.id === nodeId) {
    selectedNode.value = null
  }
}

// 更新节点配置
function updateNodeConfig(nodeId: string, config: Record<string, any>) {
  const node = nodes.value.find(n => n.id === nodeId)
  if (node) {
    node.data = { ...node.data, config }
  }
}

// 保存流程
async function handleSave() {
  if (!flow.value) {
    ElMessage.warning('请先创建流程')
    return
  }
  saving.value = true
  try {
    flow.value.nodesJson = JSON.stringify(nodes.value)
    flow.value.edgesJson = JSON.stringify(edges.value)
    await updateFlow(flow.value)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

// 执行流程
async function handleExecute() {
  if (!flow.value?.id) {
    ElMessage.warning('请先保存流程')
    return
  }
  try {
    await executeFlow(flow.value.id)
    ElMessage.success('执行已启动')
  } catch (e) {
    ElMessage.error('执行失败')
  }
}

// 停止流程
async function handleStop() {
  if (!flow.value?.id) return
  try {
    await stopFlow(flow.value.id)
    ElMessage.success('已停止')
  } catch (e) {
    ElMessage.error('停止失败')
  }
}
</script>

<style scoped>
.flow-canvas-container {
  display: flex;
  height: calc(100vh - 120px);
  background: #f5f5f5;
}

.canvas-wrapper {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.flow-canvas {
  flex: 1;
  background: #fafafa;
}

:deep(.vue-flow__minimap) {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}

:deep(.vue-flow__controls) {
  border: 1px solid #e8e8e8;
  border-radius: 4px;
}
</style>
