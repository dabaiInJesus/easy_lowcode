<template>
  <el-form label-width="120px">
    <el-form-item label="默认日期格式">
      <el-select v-model="localConfig.defaultFormat">
        <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss" />
        <el-option label="yyyy-MM-dd" value="yyyy-MM-dd" />
        <el-option label="yyyy/MM/dd" value="yyyy/MM/dd" />
        <el-option label="HH:mm:ss" value="HH:mm:ss" />
      </el-select>
    </el-form-item>
    <el-form-item label="按字段单独配置">
      <div v-for="(fmt, field, idx) in localConfig.fieldFormats" :key="idx" style="display:flex;gap:8px;margin-bottom:6px">
        <el-input v-model="localConfig.fieldFormats[field]" :placeholder="field" style="width:200px" disabled />
        <el-input v-model="localConfig.fieldFormats[field]" placeholder="格式" style="width:160px" />
        <el-button size="small" type="danger" @click="delete localConfig.fieldFormats[field]">×</el-button>
      </div>
      <el-button size="small" @click="addFieldFormat">+ 添加字段</el-button>
    </el-form-item>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{ modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', val: Record<string, any>): void }>()

const localConfig = reactive({
  defaultFormat: props.modelValue?.defaultFormat || 'yyyy-MM-dd HH:mm:ss',
  fieldFormats: { ...(props.modelValue?.fieldFormats || {}) },
})

function addFieldFormat() {
  const key = `field_${Object.keys(localConfig.fieldFormats).length + 1}`
  localConfig.fieldFormats[key] = 'yyyy-MM-dd'
}

watch(localConfig, () => emit('update:modelValue', {
  defaultFormat: localConfig.defaultFormat,
  fieldFormats: { ...localConfig.fieldFormats },
}), { deep: true })
</script>
