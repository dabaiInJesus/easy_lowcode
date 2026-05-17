<template>
  <div>
    <el-alert
      v-if="templates.length === 0"
      title="尚未配置查询模板，将使用自动生成的 SQL 查询"
      type="info"
      show-icon
      :closable="false"
      style="margin-bottom:12px"
    />

    <div style="margin-bottom:12px;display:flex;gap:8px;flex-wrap:wrap">
      <el-tag
        v-for="(t, idx) in templates"
        :key="t.name"
        :type="currentIndex === idx ? 'primary' : 'info'"
        :closable="templates.length > 1"
        style="cursor:pointer"
        @click="currentIndex = idx"
        @close="removeTemplate(idx)"
      >
        {{ t.label || t.name }}
        <span v-if="t.isDefault" style="margin-left:4px;font-size:11px">(默认)</span>
      </el-tag>
      <el-button size="small" @click="addTemplate">+ 新建模板</el-button>
    </div>

    <template v-if="currentTemplate">
      <el-form label-width="100px" size="small">
        <el-row :gutter="16">
          <el-col :span="8">
            <el-form-item label="模板名称">
              <el-input v-model="currentTemplate.name" placeholder="如: default" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="显示名称">
              <el-input v-model="currentTemplate.label" placeholder="如: 默认查询" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="设为默认">
              <el-switch v-model="currentTemplate.isDefault" @change="onDefaultChange" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="说明">
          <el-input v-model="currentTemplate.description" placeholder="模板用途说明" />
        </el-form-item>
        <el-form-item label="SQL 模板">
          <div style="width:100%">
            <el-input
              v-model="currentTemplate.sql"
              type="textarea"
              :rows="8"
              placeholder='SELECT * FROM {tableName} WHERE 1=1 {#status} AND status = {status} {/status}'
              style="font-family:monospace;font-size:13px"
            />
            <div style="margin-top:6px;font-size:12px;color:#999">
              <p>可用占位符:</p>
              <ul style="margin:4px 0;padding-left:20px">
                <li><code v-pre>{{tableName}}</code> - 自动替换为注册表名</li>
                <li><code v-pre>{{fieldName}}</code> - 替换为参数值（PreparedStatement）</li>
                <li><code v-pre>{{#fieldName}}</code>...<code v-pre>{{/fieldName}}</code> - 按条件包含 SQL 片段</li>
              </ul>
            </div>
          </div>
        </el-form-item>
      </el-form>

      <el-divider>自动检测的参数列表</el-divider>
      <el-table :data="currentTemplate.parameters" border stripe size="small" max-height="200">
        <el-table-column label="参数名" prop="name" width="160" />
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-select v-model="row.type" size="small">
              <el-option label="string" value="string" />
              <el-option label="int" value="int" />
              <el-option label="boolean" value="boolean" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="必填" width="70" align="center">
          <template #default="{ row }">
            <el-checkbox v-model="row.required" />
          </template>
        </el-table-column>
        <el-table-column label="默认值" width="120">
          <template #default="{ row }">
            <el-input v-model="row.defaultValue" size="small" />
          </template>
        </el-table-column>
        <el-table-column label="显示名称" width="140">
          <template #default="{ row }">
            <el-input v-model="row.label" size="small" />
          </template>
        </el-table-column>
      </el-table>
      <el-button size="small" style="margin-top:8px" @click="detectParams">
        从 SQL 自动检测参数
      </el-button>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import type { QueryTemplate, TemplateParam } from '@/types/tableResource'
import { ElMessage } from 'element-plus'

const props = defineProps<{
  modelValue: QueryTemplate[]
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: QueryTemplate[]): void
}>()

const templates = ref<QueryTemplate[]>(JSON.parse(JSON.stringify(props.modelValue || [])))
const currentIndex = ref(0)

if (templates.value.length === 0) {
  templates.value.push(createDefaultTemplate())
}

const currentTemplate = computed(() => templates.value[currentIndex.value])

function createDefaultTemplate(): QueryTemplate {
  return {
    name: 'default',
    label: '默认查询',
    description: '按传入条件过滤查询',
    sql: 'SELECT * FROM {{tableName}} WHERE 1=1\n{{#status}} AND status = {{status}} {{/status}}\n{{#keyword}} AND (name LIKE {{keyword}} OR remark LIKE {{keyword}}) {{/keyword}}\nORDER BY {{orderField}} {{orderDir}}',
    enabled: true,
    isDefault: true,
    parameters: [
      { name: 'status', type: 'int', required: false, defaultValue: '', label: '状态' },
      { name: 'keyword', type: 'string', required: false, defaultValue: '', label: '关键词' },
      { name: 'orderField', type: 'string', required: false, defaultValue: 'id', label: '排序字段' },
      { name: 'orderDir', type: 'string', required: false, defaultValue: 'DESC', label: '排序方向' },
    ],
  }
}

function onDefaultChange() {
  templates.value.forEach((t, idx) => {
    if (idx !== currentIndex.value) t.isDefault = false
  })
}

function addTemplate() {
  const name = `template_${templates.value.length + 1}`
  templates.value.push({
    name,
    label: name,
    description: '',
    sql: 'SELECT * FROM {{tableName}}',
    enabled: true,
    isDefault: false,
    parameters: [],
  })
  currentIndex.value = templates.value.length - 1
}

function removeTemplate(idx: number) {
  templates.value.splice(idx, 1)
  if (currentIndex.value >= templates.value.length) {
    currentIndex.value = templates.value.length - 1
  }
  if (templates.value.length === 0) {
    templates.value.push(createDefaultTemplate())
    currentIndex.value = 0
  }
}

function detectParams() {
  if (!currentTemplate.value) return
  const sql = currentTemplate.value.sql
  const paramRegex = /\{\{([a-zA-Z_][a-zA-Z0-9_]*)\}\}/g
  const found = new Set<string>()
  let match
  while ((match = paramRegex.exec(sql)) !== null) {
    const name = match[1]
    if (name !== 'tableName') {
      found.add(name)
    }
  }
  const existing = new Set(currentTemplate.value.parameters.map(p => p.name))
  for (const name of found) {
    if (!existing.has(name)) {
      currentTemplate.value.parameters.push({
        name,
        type: 'string',
        required: false,
        defaultValue: '',
        label: name,
      })
    }
  }
  ElMessage.success(`检测到 ${found.size} 个参数`)
}

watch(templates, () => {
  emit('update:modelValue', JSON.parse(JSON.stringify(templates.value)))
}, { deep: true })
</script>
