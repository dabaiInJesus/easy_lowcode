<template>
  <div class="dashboard-view" :style="viewStyle">
    <div class="view-header">
      <span class="view-title">{{ dashboard.title || dashboard.name }}</span>
      <div class="view-actions">
        <el-button size="small" v-if="refreshInterval > 0" @click="toggleAutoRefresh">
          {{ autoRefresh ? '暂停刷新' : '自动刷新' }} ({{ refreshInterval }}s)
        </el-button>
        <el-button size="small" @click="fullscreen">全屏</el-button>
        <el-button size="small" @click="goBack">返回</el-button>
      </div>
    </div>

    <div class="view-content">
      <div class="chart-layout" :style="{ gridTemplateColumns: `repeat(${gridCols}, 1fr)` }">
        <div v-for="item in chartData" :key="item.chart.id" class="chart-wrapper"
          :style="{ gridColumn: `span ${item.chart.width || 4}`, gridRow: `span ${item.chart.height || 3}` }">
          <div class="chart-title-bar">{{ item.chart.title }}</div>
          <div class="chart-body">
            <div v-if="item.chart.chartType === 'number'" class="chart-number">
              <span class="number-value">{{ getNumberValue(item.data) }}</span>
              <span class="number-label">{{ item.chart.xField }}</span>
            </div>
            <div v-else-if="item.chart.chartType === 'table'" class="chart-table-wrapper">
              <el-table :data="item.data || []" size="small" border stripe max-height="100%"
                style="width:100%;background:transparent">
                <el-table-column v-for="col in getTableColumns(item.data)" :key="col" :prop="col" :label="col" min-width="100" />
              </el-table>
            </div>
            <div v-else-if="item.chart.chartType === 'text'" class="chart-text">
              {{ item.chart.querySql }}
            </div>
            <div v-else class="chart-echarts" :ref="(el) => initChart(el as HTMLElement | null, item)"></div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { previewDashboard } from '@/api/dashboard'
import { buildChartOption } from '@/utils/chartRenderer'

const route = useRoute()
const router = useRouter()
const dashboardId = Number(route.params.id)

const dashboard = ref<any>({})
const chartData = ref<any[]>([])
const gridCols = ref(12)
const autoRefresh = ref(false)
const refreshInterval = computed(() => dashboard.value.refreshInterval || 0)
let refreshTimer: any = null
const chartInstances = new Map<number, echarts.ECharts>()

const viewStyle = computed(() => ({
  backgroundColor: dashboard.value.backgroundColor || '#0a1628',
  color: '#fff',
  minHeight: '100vh',
}))

const loadData = async () => {
  try {
    const res = await previewDashboard(dashboardId)
    dashboard.value = res.dashboard || {}
    chartData.value = res.charts || []
    // 等待 DOM 更新后重绘
    await nextTick()
    chartData.value.forEach(item => renderChart(item))
  } catch { ElMessage.error('加载大屏失败') }
}

const renderChart = (item: any) => {
  if (!item || !item.chart) return
  const chartId = item.chart.id
  if (!chartId) return
  // 获取或创建实例在 initChart 中处理
}

const initChart = (el: HTMLElement | null, item: any) => {
  if (!el || !item.chart) return

  const chartId = item.chart.id
  // 销毁旧实例
  const old = chartInstances.get(chartId)
  if (old) { old.dispose(); chartInstances.delete(chartId) }

  // 空数据时不初始化
  if (!item.data || item.data.length === 0) {
    el.textContent = '暂无数据'
    el.style.color = 'rgba(255,255,255,.3)'
    el.style.textAlign = 'center'
    el.style.padding = '20px'
    el.style.fontSize = '13px'
    return
  }

  const instance = echarts.init(el, undefined, { renderer: 'canvas' })
  chartInstances.set(chartId, instance)

  const option = buildChartOption(
    item.chart.chartType,
    item.chart.title,
    item.data,
    item.chart.xField,
    item.chart.yField,
    item.chart.groupField,
    item.chart.chartOption,
  )
  instance.setOption(option, true)

  // 自适应
  const ro = new ResizeObserver(() => {
    instance.resize()
  })
  ro.observe(el)
  ;(instance as any).__resizeObserver = ro

  // 监听窗口大小变化
  const onResize = () => instance.resize()
  window.addEventListener('resize', onResize)
  ;(instance as any).__windowResize = onResize
}

const toggleAutoRefresh = () => {
  autoRefresh.value = !autoRefresh.value
  if (autoRefresh.value) startAutoRefresh()
  else stopAutoRefresh()
}

const startAutoRefresh = () => {
  if (refreshInterval.value > 0) {
    refreshTimer = setInterval(loadData, refreshInterval.value * 1000)
  }
}

const stopAutoRefresh = () => {
  if (refreshTimer) { clearInterval(refreshTimer); refreshTimer = null }
}

const fullscreen = () => {
  const el = document.documentElement
  if (el.requestFullscreen) el.requestFullscreen()
}

const goBack = () => { router.back() }

const getNumberValue = (data: any[]) => {
  if (!data || data.length === 0) return '--'
  const firstRow = data[0]
  const keys = Object.keys(firstRow)
  const val = firstRow[keys[keys.length - 1]]
  return val !== null && val !== undefined ? String(val) : '--'
}

const getTableColumns = (data: any[]) => {
  if (!data || data.length === 0) return []
  return Object.keys(data[0])
}

onMounted(() => { loadData() })

onUnmounted(() => {
  stopAutoRefresh()
  chartInstances.forEach((instance: any, _id: any) => {
    const ro = (instance as any).__resizeObserver
    if (ro) ro.disconnect()
    const wr = (instance as any).__windowResize
    if (wr) window.removeEventListener('resize', wr)
    instance.dispose()
  })
  chartInstances.clear()
})
</script>

<style scoped>
.dashboard-view { display: flex; flex-direction: column; position: relative; }
.view-header { display: flex; justify-content: space-between; align-items: center; padding: 10px 20px; background: rgba(0,0,0,.3); position: sticky; top: 0; z-index: 10; }
.view-title { font-size: 20px; font-weight: 600; }
.view-actions { display: flex; gap: 8px; }
.view-content { flex: 1; padding: 16px; overflow-y: auto; }
.chart-layout { display: grid; gap: 12px; }
.chart-wrapper { background: rgba(255,255,255,.06); border-radius: 8px; overflow: hidden; display: flex; flex-direction: column; }
.chart-title-bar { padding: 6px 12px; font-size: 13px; background: rgba(0,0,0,.2); }
.chart-body { flex: 1; display: flex; align-items: center; justify-content: center; padding: 8px; overflow: auto; min-height: 0; }
.chart-number { text-align: center; }
.number-value { font-size: 42px; font-weight: 700; display: block; line-height: 1.2; }
.number-label { font-size: 14px; opacity: .6; display: block; margin-top: 4px; }
.chart-table-wrapper { width: 100%; height: 100%; overflow: auto; }
.chart-text { font-size: 14px; padding: 10px; }
.chart-echarts { width: 100%; height: 100%; min-height: 150px; }
</style>
