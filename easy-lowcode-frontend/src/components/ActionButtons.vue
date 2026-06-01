<template>
  <div class="action-buttons">
    <el-button
      v-for="btn in buttons"
      :key="btn.key"
      :type="btn.type"
      :size="btn.size || 'small'"
      :loading="loading === btn.key"
      :disabled="btn.disabled"
      :plain="btn.plain"
      @click="handleClick(btn)"
    >
      <component v-if="btn.icon && !loading" :is="btn.icon" />
      {{ btn.label }}
    </el-button>
  </div>
</template>

<script setup lang="ts">
import { type Component } from 'vue'

export interface ActionButton {
  key: string
  label: string
  type?: 'primary' | 'success' | 'warning' | 'danger' | 'info' | 'default'
  icon?: Component
  size?: 'large' | 'default' | 'small'
  plain?: boolean
  disabled?: boolean
  click?: () => void | Promise<void>
}

interface Props {
  buttons: ActionButton[]
  loading?: string | null
}

defineProps<Props>()

const emit = defineEmits<{
  (e: 'click', btn: ActionButton): void
}>()

const handleClick = (btn: ActionButton) => {
  if (btn.click) {
    btn.click()
  }
  emit('click', btn)
}
</script>

<style scoped>
.action-buttons {
  display: flex;
  gap: 8px;
}

.action-buttons .el-button {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
</style>