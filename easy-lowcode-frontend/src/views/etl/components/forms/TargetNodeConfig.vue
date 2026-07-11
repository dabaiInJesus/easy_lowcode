<template>
  <el-form label-position="top" size="small" :model="formData">
    <!-- 数据库类型 -->
    <template v-if="isDbType">
      <DataSourceSelector :db-type="nodeSubType" @select="onDataSourceSelect" />
      <el-divider v-if="formData.datasourceId" content-position="left">
        <span style="font-size: 12px; color: #999">已选择数据源，可手动修改</span>
      </el-divider>
      <el-form-item label="JDBC URL">
        <el-input v-model="formData.url" placeholder="jdbc:mysql://localhost:3306/db" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="formData.username" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="密码">
        <el-input v-model="formData.password" type="password" show-password @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="目标表名">
        <el-input v-model="formData.table" placeholder="table_name" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="写入模式">
        <el-select v-model="formData.writeMode" @change="emitUpdate">
          <el-option label="INSERT" value="INSERT" />
          <el-option label="MERGE (UPSERT)" value="MERGE" />
          <el-option label="TRUNCATE + INSERT" value="TRUNCATE" />
        </el-select>
      </el-form-item>
    </template>

    <!-- CSV -->
    <template v-else-if="nodeSubType === 'csv'">
      <el-form-item label="输出文件路径">
        <el-input v-model="formData.filePath" placeholder="/data/output.csv" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="分隔符">
        <el-input v-model="formData.delimiter" placeholder="," @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="写入表头">
        <el-switch v-model="formData.writeHeader" @change="emitUpdate" />
      </el-form-item>
    </template>

    <!-- Excel -->
    <template v-else-if="nodeSubType === 'excel'">
      <el-form-item label="输出文件路径">
        <el-input v-model="formData.filePath" placeholder="/data/output.xlsx" @change="emitUpdate" />
      </el-form-item>
      <el-form-item label="写入表头">
        <el-switch v-model="formData.writeHeader" @change="emitUpdate" />
      </el-form-item>
    </template>
  </el-form>
</template>

<script setup lang="ts">
import { reactive, computed, watch } from 'vue'
import DataSourceSelector from './DataSourceSelector.vue'
import type { DataSourceConfig } from '@/api/datasource'

interface Props {
  nodeSubType: string
  config: Record<string, any>
}

const props = defineProps<Props>()
const emit = defineEmits<{
  (e: 'update', config: Record<string, any>): void
}>()

const isDbType = computed(() => ['mysql', 'postgresql', 'oracle', 'hive'].includes(props.nodeSubType))

const formData = reactive<Record<string, any>>({
  url: '',
  username: '',
  password: '',
  table: '',
  writeMode: 'INSERT',
  datasourceId: undefined,
  filePath: '',
  delimiter: ',',
  writeHeader: true,
  ...props.config,
})

function onDataSourceSelect(ds: DataSourceConfig) {
  formData.datasourceId = ds.id
  formData.url = ds.url || ''
  formData.username = ds.username || ''
  emitUpdate()
}

function emitUpdate() {
  emit('update', { ...formData })
}

watch(() => props.config, (newConfig) => {
  Object.assign(formData, newConfig)
}, { deep: true })
</script>
