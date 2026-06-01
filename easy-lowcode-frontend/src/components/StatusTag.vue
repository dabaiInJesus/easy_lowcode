<template>
  <el-tag
    :type="typeMap[status]"
    :size="size"
    :effect="effect"
    :hit="hit"
    :round="round"
  >
    {{ label || status }}
  </el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'

interface Props {
  status: string | number
  typeMap?: Record<string, any>
  labelMap?: Record<string, string>
  label?: string
  size?: 'large' | 'default' | 'small'
  effect?: 'light' | 'dark' | 'plain'
  hit?: boolean
  round?: boolean
}

const props = withDefaults(defineProps<Props>(), {
  typeMap: () => ({
    1: 'success',
    0: 'danger',
    ENABLE: 'success',
    DISABLE: 'danger',
    ENABLE: 'success',
    DISABLE: 'danger',
    SUCCESS: 'success',
    FAILURE: 'danger',
    RUNNING: 'warning',
    PENDING: 'info',
    ACTIVE: 'success',
    INACTIVE: 'info',
    DELETED: 'info'
  }),
  labelMap: () => ({}),
  size: 'small',
  effect: 'light',
  hit: false,
  round: false
})

const type = computed(() => props.typeMap[props.status] || 'info')
</script>