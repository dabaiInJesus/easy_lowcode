<template>
  <div class="workflow-designer">
    <WorkflowCanvas :workflow-id="workflowId" />
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import WorkflowCanvas from './WorkflowCanvas.vue'

const route = useRoute()
const workflowId = computed(() => {
  const id = route.params.id
  return id ? Number(id) : undefined
})
</script>

<style scoped>
.workflow-designer { height: 100vh; overflow: hidden; }
</style>
