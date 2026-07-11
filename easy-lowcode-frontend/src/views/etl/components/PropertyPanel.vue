<template>
  <div class="property-panel">
    <div class="panel-header">
      <span class="panel-title">节点配置</span>
      <el-button text @click="$emit('close')">
        <el-icon><Close /></el-icon>
      </el-button>
    </div>

    <div class="panel-body" v-if="node">
      <!-- 节点基本信息 -->
      <el-form label-position="top" size="small">
        <el-form-item label="节点名称">
          <el-input v-model="nodeName" @change="updateConfig" />
        </el-form-item>

        <el-form-item label="节点类型">
          <el-tag :color="node.data?.color" effect="dark" size="small">
            {{ node.data?.label }}
          </el-tag>
        </el-form-item>
      </el-form>

      <!-- 动态配置表单 -->
      <el-divider content-position="left">连接配置</el-divider>

      <!-- 数据源节点配置 -->
      <template v-if="node.data?.nodeType === 'SOURCE'">
        <SourceNodeConfig
          :node-sub-type="node.data?.nodeSubType"
          :config="node.data?.config || {}"
          @update="onConfigUpdate"
        />
      </template>

      <!-- 转换节点配置 -->
      <template v-if="node.data?.nodeType === 'TRANSFORM'">
        <TransformNodeConfig
          :node-sub-type="node.data?.nodeSubType"
          :config="node.data?.config || {}"
          @update="onConfigUpdate"
        />
      </template>

      <!-- 目标节点配置 -->
      <template v-if="node.data?.nodeType === 'TARGET'">
        <TargetNodeConfig
          :node-sub-type="node.data?.nodeSubType"
          :config="node.data?.config || {}"
          @update="onConfigUpdate"
        />
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { Close } from '@element-plus/icons-vue'
import SourceNodeConfig from './forms/SourceNodeConfig.vue'
import TransformNodeConfig from './forms/TransformNodeConfig.vue'
import TargetNodeConfig from './forms/TargetNodeConfig.vue'
import type { Node } from '@vue-flow/core'

interface Props {
  node: Node | null
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', nodeId: string, config: Record<string, any>): void
  (e: 'close'): void
}>()

const nodeName = ref('')

watch(() => props.node, (newNode) => {
  if (newNode) {
    nodeName.value = (newNode.data?.label as string) || ''
  }
}, { immediate: true })

function updateConfig() {
  if (props.node) {
    props.node.data = { ...props.node.data, label: nodeName.value }
  }
}

function onConfigUpdate(config: Record<string, any>) {
  if (props.node) {
    emit('update', props.node.id, config)
  }
}
</script>

<style scoped>
.property-panel {
  width: 320px;
  background: #fff;
  border-left: 1px solid #e8e8e8;
  overflow-y: auto;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
}

.panel-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
}

.panel-body {
  padding: 16px;
}
</style>
