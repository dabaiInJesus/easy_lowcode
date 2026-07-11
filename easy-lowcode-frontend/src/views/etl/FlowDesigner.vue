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
  return id ? Number(id) : undefined
})
</script>

<style scoped>
.flow-designer {
  height: 100vh;
  overflow: hidden;
}
</style>
