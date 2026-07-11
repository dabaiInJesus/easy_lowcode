<template>
  <el-form-item label="选择已有数据源">
    <el-select
      v-model="selectedId"
      placeholder="选择数据源（可选）"
      clearable
      filterable
      @change="onSelect"
      style="width: 100%"
    >
      <el-option
        v-for="ds in dataSources"
        :key="ds.id"
        :label="`${ds.name} (${ds.dbType})`"
        :value="ds.id"
      />
    </el-select>
  </el-form-item>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getActiveDataSources } from '@/api/datasource'
import type { DataSourceConfig } from '@/api/datasource'

interface Props {
  dbType?: string
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'select', dataSource: DataSourceConfig): void
}>()

const selectedId = ref<number | undefined>(undefined)
const dataSources = ref<DataSourceConfig[]>([])

onMounted(async () => {
  try {
    const list = await getActiveDataSources()
    // 按 dbType 过滤
    if (props.dbType) {
      dataSources.value = list.filter(ds => ds.dbType?.toLowerCase().includes(props.dbType!.toLowerCase()))
    } else {
      dataSources.value = list
    }
  } catch (e) {
    // 静默失败
  }
})

function onSelect(id: number) {
  if (!id) return
  const ds = dataSources.value.find(d => d.id === id)
  if (ds) {
    emit('select', ds)
  }
}
</script>
