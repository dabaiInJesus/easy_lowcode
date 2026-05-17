<template>
  <el-form label-width="100px">
    <el-form-item label="过滤模式">
      <el-select v-model="localConfig.mode">
        <el-option label="白名单（仅保留指定字段）" value="WHITELIST" />
        <el-option label="黑名单（排除指定字段）" value="BLACKLIST" />
      </el-select>
    </el-form-item>
    <el-form-item label="字段列表">
      <div v-for="(f, idx) in localConfig.fields" :key="idx" style="display:flex;gap:8px;margin-bottom:6px">
        <el-input v-model="localConfig.fields[idx]" placeholder="字段名" style="width:200px" />
        <el-button size="small" type="danger" @click="localConfig.fields.splice(idx, 1)">×</el-button>
      </div>
      <el-button size="small" @click="localConfig.fields.push('')">+ 添加字段</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{ modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', val: Record<string, any>): void }>()

const localConfig = reactive({
  mode: props.modelValue?.mode || 'WHITELIST',
  fields: [...(props.modelValue?.fields || [])],
})

watch(localConfig, () => emit('update:modelValue', {
  mode: localConfig.mode,
  fields: [...localConfig.fields],
}), { deep: true })
</script>
