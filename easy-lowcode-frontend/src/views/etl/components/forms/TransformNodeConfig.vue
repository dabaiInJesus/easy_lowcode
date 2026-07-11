<template>
  <el-form label-position="top" size="small" :model="formData">
    <!-- 过滤 -->
    <template v-if="nodeSubType === 'filter'">
      <el-form-item label="过滤字段">
        <el-input v-model="formData.field" placeholder="字段名" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="操作符">
        <el-select v-model="formData.operator" @change="emitUpdate">
          <el-option label="等于" value="eq" />
          <el-option label="不等于" value="neq" />
          <el-option label="大于" value="gt" />
          <el-option label="大于等于" value="gte" />
          <el-option label="小于" value="lt" />
          <el-option label="小于等于" value="lte" />
          <el-option label="包含" value="contains" />
          <el-option label="以...开头" value="startsWith" />
          <el-option label="以...结尾" value="endsWith" />
          <el-option label="为空" value="isEmpty" />
          <el-option label="不为空" value="isNotEmpty" />
        </el-select>
      </el-form-item>
      <el-form-item label="比较值">
        <el-input v-model="formData.value" placeholder="值" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- 字段映射 -->
    <template v-else-if="nodeSubType === 'map'">
      <el-form-item label="字段映射">
        <div v-for="(mapping, index) in formData.mappings" :key="index" class="mapping-row">
          <el-input v-model="mapping.source" placeholder="源字段" size="small" @change="emitUpdate" />
          <span class="mapping-arrow">→</span>
          <el-input v-model="mapping.target" placeholder="目标字段" size="small" @change="emitUpdate" />
          <el-button text size="small" @click="removeMapping(Number(index))">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <el-button size="small" @click="addMapping">
          <el-icon><Plus /></el-icon> 添加映射
        </el-button>
      </el-form-item>
    </template>

    <!-- 聚合 -->
    <template v-else-if="nodeSubType === 'aggregate'">
      <el-form-item label="分组字段">
        <el-input v-model="formData.groupBy" placeholder="字段名" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="聚合函数">
        <el-select v-model="formData.aggFunction" @change="emitUpdate">
          <el-option label="SUM" value="sum" />
          <el-option label="COUNT" value="count" />
          <el-option label="AVG" value="avg" />
          <el-option label="MIN" value="min" />
          <el-option label="MAX" value="max" />
        </el-select>
      </el-form-item>
      <el-form-item label="聚合字段">
        <el-input v-model="formData.aggField" placeholder="字段名" @change="emitUpdate" />
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
import { Plus, Delete } from '@element-plus/icons-vue'

interface Props {
  nodeSubType: string
  config: Record<string, any>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', config: Record<string, any>): void
}>()

const formData = reactive<Record<string, any>>({
  field: '',
  operator: 'eq',
  value: '',
  mappings: [{ source: '', target: '' }],
  groupBy: '',
  aggFunction: 'sum',
  aggField: '',
  ...props.config,
})

function emitUpdate() {
  emit('update', { ...formData })
}

function addMapping() {
  formData.mappings.push({ source: '', target: '' })
}

function removeMapping(index: number) {
  formData.mappings.splice(index, 1)
  emitUpdate()
}

watch(() => props.config, (newConfig) => {
  Object.assign(formData, newConfig)
}, { deep: true })
</script>

<style scoped>
.mapping-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.mapping-arrow {
  color: #999;
}
</style>
