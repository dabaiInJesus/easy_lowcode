<template>
  <div class="unified-key-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span class="card-title">统一Key映射管理</span>
          <el-button type="primary" @click="showAddKeyDialog = true">新建统一Key</el-button>
        </div>
      </template>

      <el-table :data="keys" border stripe v-loading="loading" row-key="unifiedKey">
        <el-table-column prop="unifiedKey" label="统一Key" width="150" />
        <el-table-column prop="displayName" label="显示名称" width="150" />
        <el-table-column label="映射数量" width="120">
          <template #default="{ row }">
            <el-tag>{{ mappingCounts[row.unifiedKey] || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button size="small" @click="editKey(row)">编辑</el-button>
            <el-button size="small" type="primary" @click="viewMappings(row)">查看映射</el-button>
            <el-button size="small" type="success" @click="showAutoMap(row)">自动检测</el-button>
            <el-button size="small" type="danger" @click="deleteKey(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑统一Key -->
    <el-dialog v-model="showAddKeyDialog" :title="editingKey ? '编辑统一Key' : '新建统一Key'" width="500px">
      <el-form :model="keyForm" label-width="100px">
        <el-form-item label="统一Key" required>
          <el-input v-model="keyForm.unifiedKey" placeholder="如 email, phone, name" :disabled="!!editingKey" />
        </el-form-item>
        <el-form-item label="显示名称" required>
          <el-input v-model="keyForm.displayName" placeholder="如 邮箱, 手机号, 姓名" />
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="keyForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showAddKeyDialog = false">取消</el-button>
        <el-button type="primary" @click="saveKey">保存</el-button>
      </template>
    </el-dialog>

    <!-- 映射详情 -->
    <el-dialog v-model="showMappingsDialog" :title="'映射: ' + currentKey" width="900px" top="5vh">
      <el-table :data="currentMappings" border stripe size="small">
        <el-table-column prop="resourceCode" label="资源编码" width="150" />
        <el-table-column prop="fieldName" label="字段名" width="150" />
        <el-table-column prop="dataType" label="数据类型" width="100" />
        <el-table-column prop="queryType" label="查询方式" width="100" />
        <el-table-column label="操作" width="120">
          <template #default="{ row, $index }">
            <el-button size="small" type="danger" @click="removeMapping($index)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="showMappingsDialog = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 自动检测 -->
    <el-dialog v-model="showAutoMapDialog" :title="'自动检测映射: ' + autoMapKey" width="1000px" top="5vh">
      <p style="margin-bottom:12px;color:#666">
        以下是根据字段名相似度自动检测到的可能映射，请勾选后点击"确认添加"。
      </p>
      <el-table :data="suggestions" border stripe size="small" v-loading="suggestLoading" max-height="500" row-key="fieldName" @selection-change="onSuggestionSelect">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="resourceCode" label="资源编码" width="150" />
        <el-table-column prop="resourceName" label="资源名称" width="150" />
        <el-table-column prop="fieldName" label="字段名" width="150" />
        <el-table-column prop="columnComment" label="字段注释" width="150" />
        <el-table-column prop="dataType" label="类型" width="80" />
      </el-table>
      <template #footer>
        <el-button @click="showAutoMapDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmSuggestions">确认添加</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getDistinctKeys, getMappingsByKey, createMapping, batchCreateMappings,
  suggestMappings, deleteMapping,
} from '@/api/unifiedKey'
import type { UnifiedKeyMapping } from '@/api/unifiedKey'

const loading = ref(false)
const keys = ref<UnifiedKeyMapping[]>([])
const mappingCounts = reactive<Record<string, number>>({})

const showAddKeyDialog = ref(false)
const editingKey = ref<UnifiedKeyMapping | null>(null)
const keyForm = reactive({ unifiedKey: '', displayName: '', description: '' })

const showMappingsDialog = ref(false)
const currentKey = ref('')
const currentMappings = ref<UnifiedKeyMapping[]>([])

const showAutoMapDialog = ref(false)
const autoMapKey = ref('')
const autoMapDisplayName = ref('')
const suggestions = ref<any[]>([])
const selectedSuggestions = ref<any[]>([])
const suggestLoading = ref(false)

function onSuggestionSelect(rows: any[]) {
  selectedSuggestions.value = rows
}

async function loadKeys() {
  loading.value = true
  try {
    const data = await getDistinctKeys() as any
    keys.value = Array.isArray(data) ? data : []
    for (const k of keys.value) {
      const ms = await getMappingsByKey(k.unifiedKey)
      mappingCounts[k.unifiedKey] = Array.isArray(ms) ? ms.length : 0
    }
  } catch { keys.value = [] }
  finally { loading.value = false }
}

async function saveKey() {
  if (!keyForm.unifiedKey || !keyForm.displayName) {
    ElMessage.warning('请填写完整信息')
    return
  }
  try {
    await createMapping({
      unifiedKey: keyForm.unifiedKey,
      displayName: keyForm.displayName,
      description: keyForm.description,
      resourceCode: '',
      fieldName: '',
      dataType: 'string',
      queryType: 'exact',
    })
    ElMessage.success('保存成功')
    showAddKeyDialog.value = false
    editingKey.value = null
    loadKeys()
  } catch (e: any) {
    ElMessage.error(e.message || '保存失败')
  }
}

function editKey(row: UnifiedKeyMapping) {
  editingKey.value = row
  keyForm.unifiedKey = row.unifiedKey
  keyForm.displayName = row.displayName
  keyForm.description = row.description || ''
  showAddKeyDialog.value = true
}

async function viewMappings(row: UnifiedKeyMapping) {
  currentKey.value = row.unifiedKey
  currentMappings.value = await getMappingsByKey(row.unifiedKey) || []
  showMappingsDialog.value = true
}

function deleteKey(row: UnifiedKeyMapping) {
  ElMessageBox.confirm(`确定删除统一Key "${row.unifiedKey}" 的所有映射？`, '确认', { type: 'warning' })
    .then(async () => {
      const ms = await getMappingsByKey(row.unifiedKey)
      for (const m of ms) {
        if (m.id) await deleteMapping(m.id)
      }
      ElMessage.success('已删除')
      loadKeys()
    }).catch(() => {})
}

function removeMapping(idx: number) {
  const mapping = currentMappings.value[idx]
  if (mapping.id) {
    deleteMapping(mapping.id).then(() => {
      currentMappings.value.splice(idx, 1)
      ElMessage.success('已移除')
    })
  } else {
    currentMappings.value.splice(idx, 1)
  }
}

async function showAutoMap(row: UnifiedKeyMapping) {
  autoMapKey.value = row.unifiedKey
  autoMapDisplayName.value = row.displayName
  suggestLoading.value = true
  showAutoMapDialog.value = true
  try {
    suggestions.value = await suggestMappings(row.unifiedKey, row.displayName) || []
  } catch { suggestions.value = [] }
  finally { suggestLoading.value = false }
}

async function confirmSuggestions() {
  const selected = selectedSuggestions.value
  const mappings: UnifiedKeyMapping[] = selected.map((s: any) => ({
    unifiedKey: autoMapKey.value,
    displayName: autoMapDisplayName.value,
    resourceCode: s.resourceCode,
    fieldName: s.fieldName,
    dataType: s.dataType || 'string',
    queryType: 'exact',
  }))
  if (mappings.length === 0) {
    ElMessage.warning('请选择至少一个映射')
    return
  }
  try {
    await batchCreateMappings(mappings)
    ElMessage.success(`成功添加 ${mappings.length} 个映射`)
    showAutoMapDialog.value = false
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

onMounted(() => { loadKeys() })
</script>

<style scoped>
.unified-key-management { padding: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.card-title { font-size: 16px; font-weight: 600; }
</style>
