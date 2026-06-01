<template>
  <el-dialog
    v-model="visible"
    :title="title"
    :width="width"
    :top="top"
    :close-on-click-modal="closeOnClickModal"
    @closed="onClosed"
  >
    <div class="dialog-body" :style="{ maxHeight: maxHeight }">
      <slot />
    </div>
    <template v-if="$slots.footer" #footer>
      <slot name="footer" />
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  modelValue: boolean
  title?: string
  width?: string
  top?: string
  maxHeight?: string
  closeOnClickModal?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  width: '600px',
  top: '8vh',
  maxHeight: '60vh',
  closeOnClickModal: false
})

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'closed'): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (val) => emit('update:modelValue', val)
})

const onClosed = () => {
  emit('closed')
}
</script>

<style scoped>
.dialog-body {
  overflow-y: auto;
  padding-right: 10px;
}

.dialog-body::-webkit-scrollbar {
  width: 6px;
}

.dialog-body::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

.dialog-body::-webkit-scrollbar-thumb:hover {
  background-color: #c0c4cc;
}
</style>