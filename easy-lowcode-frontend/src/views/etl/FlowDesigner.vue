<template>
  <div class="flow-designer">
    <FlowCanvas :flow-id="flowId" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import FlowCanvas from './components/FlowCanvas.vue'

const route = useRoute()
const flowId = computed(() => {
  const id = route.params.id
  // 雪花 ID 超过 JS Number 安全整数范围（2^53），必须以字符串传递，否则精度丢失后后端查不到（"流程不存在"）
  return id ? String(id) : undefined
})
</script>

<style scoped>
.flow-designer {
  height: 100vh;
  overflow: hidden;
}
</style>
