<template>
  <el-input
    v-model="jsonStr"
    type="textarea"
    :rows="8"
    placeholder="输入 JSON 配置..."
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'

const props = defineProps<{ modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', val: Record<string, any>): void }>()

const jsonStr = ref(JSON.stringify(props.modelValue || {}, null, 2))

watch(jsonStr, (nv) => {
  try {
    const parsed = JSON.parse(nv)
    emit('update:modelValue', parsed)
  } catch { }
})
</script>
