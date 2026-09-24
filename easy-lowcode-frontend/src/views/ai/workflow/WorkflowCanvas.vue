<template>
  <div class="workflow-canvas-container">
    <!-- 左侧节点面板 -->
    <NodePalette @add-node="addNode" />

    <!-- 中间画布 -->
    <div class="canvas-wrapper">
      <FlowToolbar
        :workflow="workflow"
        :saving="saving"
        :executing="executing"
        @save="handleSave"
        @execute="handleExecute"
        @publish="handlePublish"
      />
      <VueFlow
        v-model:nodes="nodes"
        v-model:edges="edges"
        :default-viewport="{ zoom: 1, x: 0, y: 0 }"
        :snap-to-grid="true"
        :snap-grid="[20, 20]"
        fit-view-on-init
        class="workflow-canvas"
        @node-click="onNodeClick"
        @connect="onConnect"
      >
        <Background />
        <Controls />
        <MiniMap />

        <!-- 自定义节点模板 -->
        <template #node-start="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#52c41a" icon="▶" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-llm="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#1890ff" icon="🤖" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-code="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#13c2c2" icon="⌨" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-http="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#eb2f96" icon="🌐" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-condition="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#faad14" icon="◆" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-variable="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#722ed1" icon="📝" @delete="deleteNode(nodeProps.id)" />
        </template>
        <template #node-end="nodeProps">
          <WorkflowNode v-bind="nodeProps" color="#ff4d4f" icon="⏹" @delete="deleteNode(nodeProps.id)" />
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
import { VueFlow, type Node, type Connection } from '@vue-flow/core'
import { Background } from '@vue-flow/background'
import { Controls } from '@vue-flow/controls'
import { MiniMap } from '@vue-flow/minimap'
import { ElMessage } from 'element-plus'
import NodePalette from './components/NodePalette.vue'
import FlowToolbar from './components/FlowToolbar.vue'
import PropertyPanel from './components/PropertyPanel.vue'
import WorkflowNode from './components/WorkflowNode.vue'
import { generateWorkflowNodeId, getWorkflowNodeType } from './workflowNodeTypes'
import { getWorkflowDetail, updateWorkflow, publishWorkflow } from '@/api/ai-workflow'
import type { AiWorkflowDef } from '@/types/ai-workflow'

interface Props {
  workflowId?: number | string
}

const props = defineProps<Props>()

const nodes = ref<any[]>([])
const edges = ref<any[]>([])
const selectedNode = ref<Node | null>(null)
const saving = ref(false)
const executing = ref(false)
const workflow = ref<AiWorkflowDef | null>(null)

onMounted(async () => {
  if (props.workflowId) {
    try {
      const detail = await getWorkflowDetail(props.workflowId)
      workflow.value = detail
      if (detail.nodesJson) nodes.value = JSON.parse(detail.nodesJson)
      if (detail.edgesJson) edges.value = JSON.parse(detail.edgesJson)
    } catch (e) {
      ElMessage.error('加载工作流失败')
    }
  }
})

function addNode(type: string, label: string) {
  const typeDef = getWorkflowNodeType(type)
  if (!typeDef) return
  const newNode: any = {
    id: generateWorkflowNodeId(),
    type: type,
    position: { x: 250 + nodes.value.length * 50, y: 100 + nodes.value.length * 30 },
    data: { label: label, nodeType: type, config: {} },
  }
  nodes.value.push(newNode)
}

function onNodeClick(event: any) {
  selectedNode.value = event.node
}

function onConnect(connection: Connection) {
  const newEdge: any = {
    id: `e-${connection.source}-${connection.target}`,
    source: connection.source!,
    target: connection.target!,
    animated: true,
    style: { stroke: '#1890ff' },
  }
  edges.value.push(newEdge)
}

function deleteNode(nodeId: string) {
  nodes.value = nodes.value.filter(n => n.id !== nodeId)
  edges.value = edges.value.filter(e => e.source !== nodeId && e.target !== nodeId)
  if (selectedNode.value?.id === nodeId) selectedNode.value = null
}

function updateNodeConfig(nodeId: string, config: Record<string, any>) {
  const node = nodes.value.find(n => n.id === nodeId)
  if (node) node.data = { ...node.data, config }
}

async function handleSave() {
  if (!workflow.value) { ElMessage.warning('请先创建工作流'); return }
  saving.value = true
  try {
    workflow.value.nodesJson = JSON.stringify(nodes.value)
    workflow.value.edgesJson = JSON.stringify(edges.value)
    await updateWorkflow(workflow.value)
    ElMessage.success('保存成功')
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}

async function handlePublish() {
  if (!workflow.value?.id) { ElMessage.warning('请先保存工作流'); return }
  try {
    await publishWorkflow(workflow.value.id)
    workflow.value.status = 'PUBLISHED'
    ElMessage.success('发布成功')
  } catch (e) {
    ElMessage.error('发布失败')
  }
}

function handleExecute() {
  if (!workflow.value?.id) { ElMessage.warning('请先保存工作流'); return }
  executing.value = true
  // TODO: 打开执行面板
  ElMessage.info('执行功能开发中')
  executing.value = false
}
</script>

<style scoped>
.workflow-canvas-container {
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
.workflow-canvas {
  flex: 1;
  background: #fafafa;
}
:deep(.vue-flow__minimap) { border: 1px solid #e8e8e8; border-radius: 4px; }
:deep(.vue-flow__controls) { border: 1px solid #e8e8e8; border-radius: 4px; }
</style>
