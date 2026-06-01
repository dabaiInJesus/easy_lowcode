<template>
  <el-card class="table-card" :class="{ 'has-pagination': showPagination }">
    <slot />
    <el-pagination
      v-if="showPagination"
      v-model:current-page="current"
      v-model:page-size="size"
      :total="total"
      :page-sizes="pageSizes"
      :layout="layout"
      class="pagination"
      @size-change="onSizeChange"
      @current-change="onCurrentChange"
    />
  </el-card>
</template>

<script setup lang="ts">
interface Props {
  showPagination?: boolean
  current?: number
  size?: number
  total?: number
  pageSizes?: number[]
  layout?: string
}

const props = withDefaults(defineProps<Props>(), {
  showPagination: true,
  current: 1,
  size: 10,
  total: 0,
  pageSizes: () => [10, 20, 50, 100],
  layout: 'total, sizes, prev, pager, next, jumper'
})

const emit = defineEmits<{
  (e: 'update:current', val: number): void
  (e: 'update:size', val: number): void
  (e: 'page-change', current: number, size: number): void
}>()

const onSizeChange = (val: number) => {
  emit('update:size', val)
  emit('page-change', props.current, val)
}

const onCurrentChange = (val: number) => {
  emit('update:current', val)
  emit('page-change', val, props.size)
}
</script>

<style scoped>
.table-card {
  margin-bottom: 20px;
}

.table-card.has-pagination :deep(.el-table) {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>