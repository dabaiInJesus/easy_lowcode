<template>
  <div>
    <p style="color:#666;font-size:13px;margin-bottom:12px">
      配置脱敏规则，支持正则表达式替换
    </p>
    <div v-for="(rule, idx) in localConfig.rules" :key="idx"
      style="border:1px solid #eee;border-radius:6px;padding:12px;margin-bottom:12px"
    >
      <el-form label-width="80px">
        <el-form-item label="字段名">
          <el-input v-model="rule.field" placeholder="如: phone" />
        </el-form-item>
        <el-form-item label="匹配正则">
          <el-input v-model="rule.pattern" placeholder='如: (\\d{3})\\d{4}(\\d{4})' />
        </el-form-item>
        <el-form-item label="替换为">
          <el-input v-model="rule.replacement" placeholder='如: $1****$2' />
        </el-form-item>
        <el-form-item>
          <el-button size="small" type="danger" @click="localConfig.rules.splice(idx, 1)">删除此规则</el-button>
        </el-form-item>
      </el-form>
    </div>
    <el-button size="small" @click="addRule">+ 添加脱敏规则</el-button>
  </div>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{ modelValue: Record<string, any> }>()
const emit = defineEmits<{ (e: 'update:modelValue', val: Record<string, any>): void }>()

const localConfig = reactive({
  rules: (props.modelValue?.rules || []).map((r: any) => ({ ...r })),
})

function addRule() {
  localConfig.rules.push({ field: '', pattern: '', replacement: '' })
}

watch(localConfig, () => emit('update:modelValue', { rules: localConfig.rules.map((r: any) => ({ ...r })) }), { deep: true })
</script>


<style scoped>
.el-form {
  max-width: 100%;
}
</style>