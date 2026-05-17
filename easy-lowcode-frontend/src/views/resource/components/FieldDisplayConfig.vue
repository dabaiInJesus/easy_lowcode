<template>
  <div>
    <el-form :model="localSettings" label-width="100px" size="small">
      <el-row :gutter="20">
        <el-col :span="8">
          <el-form-item label="每页条数">
            <el-input-number v-model="localSettings.pageSize" :min="10" :max="200" :step="10" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="斑马纹">
            <el-switch v-model="localSettings.stripe" />
          </el-form-item>
        </el-col>
        <el-col :span="8">
          <el-form-item label="表格边框">
            <el-switch v-model="localSettings.border" />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>

    <el-table :data="fieldList" border stripe size="small" max-height="400">
      <el-table-column label="字段名" prop="columnName" width="140" />
      <el-table-column label="数据类型" prop="dataType" width="100" />
      <el-table-column label="显示名称" width="140">
        <template #default="{ row }">
          <el-input v-model="row._setting.label" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="显示" width="70" align="center">
        <template #default="{ row }">
          <el-switch v-model="row._setting.visible" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="宽度" width="100">
        <template #default="{ row }">
          <el-input-number v-model="row._setting.width" :min="40" :max="600" :step="10" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="对齐" width="100">
        <template #default="{ row }">
          <el-select v-model="row._setting.align" size="small">
            <el-option label="左对齐" value="left" />
            <el-option label="居中" value="center" />
            <el-option label="右对齐" value="right" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="固定列" width="100">
        <template #default="{ row }">
          <el-select v-model="row._setting.fixed" size="small" clearable>
            <el-option label="无" value="" />
            <el-option label="左侧" value="left" />
            <el-option label="右侧" value="right" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="日期格式" width="150">
        <template #default="{ row }">
          <el-select
            v-model="row._setting.format"
            size="small"
            clearable
            :disabled="!isDateType(row.dataType)"
          >
            <el-option label="yyyy-MM-dd HH:mm:ss" value="yyyy-MM-dd HH:mm:ss" />
            <el-option label="yyyy-MM-dd" value="yyyy-MM-dd" />
            <el-option label="yyyy/MM/dd" value="yyyy/MM/dd" />
            <el-option label="HH:mm:ss" value="HH:mm:ss" />
            <el-option label="MM-dd" value="MM-dd" />
          </el-select>
        </template>
      </el-table-column>
      <el-table-column label="枚举映射" width="200">
        <template #default="{ row }">
          <el-button size="small" @click="editEnumMapping(row)">
            {{ hasEnumMapping(row) ? '已配置' : '未配置' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="enumDialog.visible" title="枚举映射配置" width="400">
      <div v-for="(val, key, idx) in enumDialog.mappings" :key="idx" style="display:flex;gap:8px;margin-bottom:8px">
        <el-input v-model="enumDialog.mappings[key]" placeholder="原始值" disabled style="width:120px" />
        <span style="line-height:32px">→</span>
        <el-input v-model="enumDialog.mappings[key]" placeholder="显示名称" style="width:160px" />
      </div>
      <div v-if="Object.keys(enumDialog.mappings).length === 0" style="color:#999;text-align:center;padding:20px">
        暂无可用的枚举字段，请选择需要映射的值
      </div>
      <template #footer>
        <el-button @click="enumDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="saveEnumMapping">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, computed, watch } from 'vue'
import type { DisplaySettings, DisplayFieldSetting, FieldConfig } from '@/types/tableResource'

interface FieldRow extends FieldConfig {
  _setting: DisplayFieldSetting
}

const props = defineProps<{
  fields: FieldConfig[]
  displaySettings: DisplaySettings
}>()

const emit = defineEmits<{
  (e: 'update', val: DisplaySettings): void
}>()

const localSettings = reactive<DisplaySettings>({
  pageSize: props.displaySettings?.pageSize || 20,
  stripe: props.displaySettings?.stripe ?? true,
  border: props.displaySettings?.border ?? false,
  fields: { ...(props.displaySettings?.fields || {}) },
})

watch(() => localSettings, () => {
  emit('update', { ...localSettings, fields: { ...localSettings.fields } })
}, { deep: true })

const fieldList = computed<FieldRow[]>(() => {
  return props.fields.map(f => {
    const existing = localSettings.fields[f.columnName]
    return {
      ...f,
      _setting: existing || {
        visible: true,
        label: f.columnComment || f.columnName,
        width: 150,
        align: 'left' as const,
        format: isDateType(f.dataType) ? 'yyyy-MM-dd HH:mm:ss' : undefined,
        sortable: true,
      },
    }
  })
})

function isDateType(dt: string): boolean {
  const lower = (dt || '').toLowerCase()
  return lower.includes('time') || lower.includes('date') || lower.includes('timestamp')
}

const enumDialog = reactive<{
  visible: boolean
  currentField: string
  mappings: Record<string, string>
}>({
  visible: false,
  currentField: '',
  mappings: {},
})

function hasEnumMapping(row: FieldRow): boolean {
  const setting = localSettings.fields[row.columnName]
  return !!setting?.enumMapping && Object.keys(setting.enumMapping).length > 0
}

function editEnumMapping(row: FieldRow) {
  enumDialog.currentField = row.columnName
  enumDialog.mappings = { ...(localSettings.fields[row.columnName]?.enumMapping || {}) }
  enumDialog.visible = true
}

function saveEnumMapping() {
  if (!localSettings.fields[enumDialog.currentField]) {
    localSettings.fields[enumDialog.currentField] = {
      visible: true,
      label: enumDialog.currentField,
    }
  }
  localSettings.fields[enumDialog.currentField].enumMapping = { ...enumDialog.mappings }
  enumDialog.visible = false
}
</script>
