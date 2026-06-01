<template>
  <div>
    <p style="color:#666;font-size:13px;margin-bottom:12px">配置键值映射关系</p>
    <div v-for="(val, key, idx) in localConfig.mappings" :key="idx" style="display:flex;gap:8px;margin-bottom:8px">
      <el-input v-model="localConfig.mappings[key]" placeholder="原始名称" style="width:140px" />
      <span style="line-height:32px">→</span>
      <el-input v-model="localConfig.mappings[key]" placeholder="映射名称" style="width:140px" />
      <el-button @click="removeEntry(key)" type="danger" size="small">×</el-button>
    </div>
    <el-button size="small" @click="addEntry">+ 添加映射</el-button>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{ modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', val: Record<string, any>): void }>()

const localConfig = reactive<Record<string, any>>(
  props.modelValue?.mappings
    ? { mappings: { ...props.modelValue.mappings } }
    : { mappings: {} }
)

function addEntry() {
  localConfig.mappings[''] = ''
}

function removeEntry(key: string) {
  delete localConfig.mappings[key]
}

watch(localConfig, () => emit('update:modelValue', { mappings: { ...localConfig.mappings } }), { deep: true })
</script>

<style scoped>
.config-wrapper {
  padding: 8px;
}
.mapping-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}
.mapping-row .el-input {
  flex: 1;
}
.mapping-arrow {
  color: #999;
  font-size: 16px;
}
</style>