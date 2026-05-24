<template>
  <el-tag :type="status === 1 ? 'success' : 'danger'" v-bind="$attrs">
    {{ statusText }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{
  status: number | string
  enabledText?: string
  disabledText?: string
}>()

const statusText = computed(() => {
  if (props.status === 1 || props.status === '1') {
    return props.enabledText || '启用'
  }
  return props.disabledText || '禁用'
})
</script>