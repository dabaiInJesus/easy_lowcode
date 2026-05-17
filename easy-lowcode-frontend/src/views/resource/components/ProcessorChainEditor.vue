<template>
  <div>
    <el-alert
      v-if="processors.length === 0"
      title="尚未配置处理器，数据将直接透传"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom:12px"
    />

    <div v-for="(p, idx) in processors" :key="idx" class="processor-item">
      <el-card shadow="hover" style="margin-bottom:8px">
        <div style="display:flex;align-items:center;gap:12px">
          <span style="color:#999;font-size:12px;min-width:24px">#{{ idx + 1 }}</span>
          <el-tag>{{ getProcessorLabel(p.type) }}</el-tag>
          <span style="flex:1;color:#666;font-size:13px">{{ getProcessorDesc(p.type) }}</span>
          <el-switch v-model="p.enabled" size="small" />
          <el-button circle size="small" @click="editItem(idx)">
            <el-icon><Edit /></el-icon>
          </el-button>
          <el-button circle size="small" type="danger" @click="removeItem(idx)">
            <el-icon><Delete /></el-icon>
          </el-button>
          <el-button circle size="small" @click="moveUp(idx)" :disabled="idx === 0">
            <el-icon><ArrowUp /></el-icon>
          </el-button>
          <el-button circle size="small" @click="moveDown(idx)" :disabled="idx === processors.length - 1">
            <el-icon><ArrowDown /></el-icon>
          </el-button>
        </div>
      </el-card>
    </div>

    <el-button type="primary" size="small" plain @click="showAddDialog = true">
      + 添加处理器
    </el-button>

    <el-dialog v-model="showAddDialog" title="选择处理器类型" width="500">
      <el-radio-group v-model="selectedType" style="width:100%">
        <el-radio
          v-for="bp in builtinList"
          :key="bp.type"
          :value="bp.type"
          style="display:block;margin-bottom:12px;padding:8px;border:1px solid #eee;border-radius:6px"
        >
          <div style="font-weight:600">{{ bp.label }}</div>
          <div style="font-size:12px;color:#999">{{ bp.description }}</div>
        </el-radio>
      </el-radio-group>
      <template #footer>
        <el-button @click="showAddDialog = false">取消</el-button>
        <el-button type="primary" @click="addProcessor">添加</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showConfigDialog" :title="'配置: ' + getProcessorLabel(editingType)" width="600">
      <component :is="getConfigForm(editingType)" v-if="editingConfig" v-model="editingConfig" />
      <template #footer>
        <el-button @click="showConfigDialog = false">取消</el-button>
        <el-button type="primary" @click="saveConfig">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { ProcessorConfig } from '@/types/tableResource'
import { BUILTIN_PARAM_PROCESSORS, BUILTIN_RESULT_PROCESSORS } from '@/types/tableResource'
import { ElMessage } from 'element-plus'
import { Edit, Delete, ArrowUp, ArrowDown } from '@element-plus/icons-vue'

const props = defineProps<{
  modelValue: ProcessorConfig[]
  type: 'param' | 'result'
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: ProcessorConfig[]): void
}>()

const processors = ref<ProcessorConfig[]>([...props.modelValue])

const builtinList = computed(() =>
  props.type === 'param' ? BUILTIN_PARAM_PROCESSORS : BUILTIN_RESULT_PROCESSORS
)

const allBuiltins = computed(() => [...BUILTIN_PARAM_PROCESSORS, ...BUILTIN_RESULT_PROCESSORS])

function getProcessorLabel(type: string): string {
  return allBuiltins.value.find(b => b.type === type)?.label || type
}

function getProcessorDesc(type: string): string {
  return allBuiltins.value.find(b => b.type === type)?.description || ''
}

const showAddDialog = ref(false)
const selectedType = ref('')

function addProcessor() {
  if (!selectedType.value) {
    ElMessage.warning('请选择处理器类型')
    return
  }
  const defaults = getDefaultConfig(selectedType.value)
  processors.value.push({
    type: selectedType.value,
    enabled: true,
    order: processors.value.length + 1,
    config: defaults,
  })
  selectedType.value = ''
  showAddDialog.value = false
  emitUpdate()
}

function removeItem(idx: number) {
  processors.value.splice(idx, 1)
  emitUpdate()
}

function moveUp(idx: number) {
  if (idx > 0) {
    [processors.value[idx], processors.value[idx - 1]] = [processors.value[idx - 1], processors.value[idx]]
    emitUpdate()
  }
}

function moveDown(idx: number) {
  if (idx < processors.value.length - 1) {
    [processors.value[idx], processors.value[idx + 1]] = [processors.value[idx + 1], processors.value[idx]]
    emitUpdate()
  }
}

const showConfigDialog = ref(false)
const editingIdx = ref(-1)
const editingType = ref('')
const editingConfig = ref<Record<string, any>>({})

function editItem(idx: number) {
  editingIdx.value = idx
  editingType.value = processors.value[idx].type
  editingConfig.value = { ...(processors.value[idx].config || {}) }
  showConfigDialog.value = true
}

function saveConfig() {
  if (editingIdx.value >= 0) {
    processors.value[editingIdx.value].config = { ...editingConfig.value }
    emitUpdate()
  }
  showConfigDialog.value = false
}

function getDefaultConfig(type: string): Record<string, any> {
  switch (type) {
    case 'defaultValue':
      return { field: '', value: '' }
    case 'paramMapping':
      return { mappings: {} }
    case 'paramValidator':
      return { required: [] }
    case 'fieldFilter':
      return { mode: 'WHITELIST', fields: [] }
    case 'dataMasking':
      return { rules: [] }
    case 'enumMapping':
      return { mappings: {} }
    case 'dateFormat':
      return { defaultFormat: 'yyyy-MM-dd HH:mm:ss', fieldFormats: {} }
    default:
      return {}
  }
}

function getConfigForm(type: string): string {
  switch (type) {
    case 'defaultValue':
      return 'DefaultValueConfig'
    case 'fieldFilter':
      return 'FieldFilterConfig'
    case 'dataMasking':
      return 'DataMaskingConfig'
    case 'paramMapping':
    case 'enumMapping':
      return 'KeyValueConfig'
    case 'dateFormat':
      return 'DateFormatConfig'
    default:
      return 'RawJsonConfig'
  }
}

function emitUpdate() {
  emit('update:modelValue', [...processors.value])
}
</script>

<style scoped>
.processor-item {
  transition: all 0.2s;
}
</style>
