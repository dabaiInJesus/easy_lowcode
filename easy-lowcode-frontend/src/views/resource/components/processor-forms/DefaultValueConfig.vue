<template>
  <el-form label-width="100px">
    <el-form-item label="字段名">
      <el-input v-model="localConfig.field" placeholder="如: status" />
    </el-form-item>
    <el-form-item label="默认值">
      <el-input v-model="localConfig.value" placeholder="如: 1" />
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{ modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', val: Record<string, any>): void }>()

const localConfig = reactive({ ...props.modelValue })
watch(localConfig, () => emit('update:modelValue', { ...localConfig }), { deep: true })
</script>


<style scoped>
.el-form {
  max-width: 100%;
}
</style>