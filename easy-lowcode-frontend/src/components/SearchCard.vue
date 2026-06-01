<template>
  <el-card class="search-card">
    <el-form :inline="true" :model="modelValue" @submit.prevent>
      <slot />
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { watch } from 'vue'

interface Props {
  modelValue?: Record<string, any>
}

const props = withDefaults(defineProps<Props>(), {
  modelValue: () => ({})
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: Record<string, any>): void
  (e: 'search'): void
  (e: 'reset'): void
}>()

const handleSearch = () => emit('search')
const handleReset = () => emit('reset')
</script>

<style scoped>
.search-card {
  margin-bottom: 20px;
}
</style>