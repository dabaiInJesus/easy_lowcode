<template>
  <div class="dashboard-designer">
    <div class="designer-header">
      <el-row align="middle" style="width:100%">
        <el-col :span="12">
          <h2 class="designer-title">{{ dashboard.name }} - 大屏设计器</h2>
        </el-col>
        <el-col :span="12" style="text-align:right">
          <el-button @click="handleBack">返回</el-button>
          <el-button @click="saveCharts" type="primary" :loading="saving">保存</el-button>
          <el-button @click="previewDashboard" type="success">预览</el-button>
        </el-col>
      </el-row>
    </div>
    <div class="designer-body">
      <div class="designer-sidebar">
        <el-menu :default-active="activeMenu" @select="(idx) => activeMenu = idx">
          <el-menu-item index="charts">
            <el-icon><PieChart /></el-icon>
            <span>图表列表</span>
          </el-menu-item>
          <el-menu-item index="add">
            <el-icon><Plus /></el-icon>
            <span>添加图表</span>
          </el-menu-item>
          <el-menu-item index="settings">
            <el-icon><Setting /></el-icon>
            <span>大屏设置</span>
          </el-menu-item>
        </el-menu>

        <!-- 图表列表 -->
        <div v-if="activeMenu === 'charts'" class="sidebar-content">
          <el-empty v-if="charts.length === 0" description="暂无图表" />
          <div v-for="(chart, idx) in charts" :key="chart.id" class="chart-item"
            :class="{ active: selectedChartIdx === idx }"
            @click="selectedChartIdx = idx">
            <div class="chart-item-title">{{ chart.title }}</div>
            <div class="chart-item-type">
              <el-tag size="small">{{ chart.chartType }}</el-tag>
              <el-button link type="danger" size="small" @click.stop="removeChart(idx)">删除</el-button>
            </div>
          </div>
        </div>

        <!-- 添加图表 -->
        <div v-if="activeMenu === 'add'" class="sidebar-content">
          <el-form label-width="100px" size="small">
            <el-form-item label="图表标题">
              <el-input v-model="newChart.title" placeholder="图表标题" />
            </el-form-item>
            <el-form-item label="图表类型">
              <el-select v-model="newChart.chartType" placeholder="选择类型" style="width:100%">
                <el-option label="柱状图" value="bar" />
                <el-option label="折线图" value="line" />
                <el-option label="饼图" value="pie" />
                <el-option label="散点图" value="scatter" />
                <el-option label="数字" value="number" />
                <el-option label="表格" value="table" />
                <el-option label="文本" value="text" />
              </el-select>
            </el-form-item>
            <el-form-item label="数据源">
              <el-select v-model="newChart.datasourceId" placeholder="数据源" style="width:100%" clearable>
                <el-option v-for="ds in datasources" :key="ds.id" :label="ds.name" :value="ds.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="查询SQL">
              <el-input v-model="newChart.querySql" type="textarea" :rows="4" placeholder="SELECT ..." />
            </el-form-item>
            <el-form-item label="X轴字段">
              <el-input v-model="newChart.xField" placeholder="X轴字段名" />
            </el-form-item>
            <el-form-item label="Y轴字段">
              <el-input v-model="newChart.yField" placeholder="Y轴字段名" />
            </el-form-item>
            <el-form-item label="宽度">
              <el-input-number v-model="newChart.width" :min="1" :max="12" />
            </el-form-item>
            <el-form-item label="高度">
              <el-input-number v-model="newChart.height" :min="1" :max="12" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="addChart" :loading="addingChart">添加</el-button>
            </el-form-item>
          </el-form>
        </div>

        <!-- 大屏设置 -->
        <div v-if="activeMenu === 'settings'" class="sidebar-content">
          <el-form label-width="100px" size="small">
            <el-form-item label="名称">
              <el-input v-model="dashboard.name" />
            </el-form-item>
            <el-form-item label="背景色">
              <el-color-picker v-model="dashboard.backgroundColor" />
            </el-form-item>
            <el-form-item label="刷新(秒)">
              <el-input-number v-model="dashboard.refreshInterval" :min="0" :max="3600" />
            </el-form-item>
          </el-form>
        </div>
      </div>

      <!-- 画布 -->
      <div class="designer-canvas" :style="{ backgroundColor: dashboard.backgroundColor || '#0a1628' }">
        <el-empty v-if="charts.length === 0" description="左侧添加图表开始设计" :image-size="80" style="color:#fff" />
        <div class="chart-grid">
          <div v-for="(chart, idx) in charts" :key="chart.id" class="chart-preview"
            :class="{ selected: selectedChartIdx === idx }"
            :style="{ gridColumn: `span ${chart.width || 4}`, gridRow: `span ${chart.height || 3}` }"
            @click="selectedChartIdx = idx">
            <div class="chart-preview-header">
              <span class="chart-preview-title">{{ chart.title }}</span>
              <el-tag size="small">{{ chart.chartType }}</el-tag>
            </div>
            <div class="chart-preview-body">
              <div v-if="chart.chartType === 'number'" class="number-value-preview">
                {{ chartDataMap[chart.id]?.displayValue || '--' }}
              </div>
              <div v-else-if="chart.chartType === 'text'" class="chart-text-preview">
                {{ chart.querySql }}
              </div>
              <div v-else class="chart-echarts-preview"
                :ref="(el) => initPreviewChart(el as HTMLElement | null, chart)"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { PieChart, Plus, Setting } from '@element-plus/icons-vue'
import { getDashboardById, getDashboardCharts, addChart as apiAddChart, removeChart as apiRemoveChart, queryChartData, type Dashboard, type DashboardChart } from '@/api/dashboard'
import { getEtlDatasources } from '@/api/etl'
import { buildChartOption } from '@/utils/chartRenderer'

const route = useRoute()
const router = useRouter()
const dashboardId = Number(route.params.id)

const dashboard = ref<Dashboard>({ name: '', code: '', backgroundColor: '#0a1628', width: 1920, height: 1080, refreshInterval: 0, status: 0 } as Dashboard)
const charts = ref<DashboardChart[]>([])
const datasources = ref<any[]>([])
const activeMenu = ref('charts')
const selectedChartIdx = ref<number | null>(null)
const saving = ref(false)
const addingChart = ref(false)
const chartDataMap = ref<Record<number, { data: any[]; displayValue: string }>>({})
const previewChartInstances = new Map<number, echarts.ECharts>()

const newChart = reactive<Partial<DashboardChart>>({
  dashboardId, title: '', chartType: 'bar', datasourceId: undefined,
  querySql: '', xField: '', yField: '', width: 4, height: 3
})

const loadDashboard = async () => {
  try {
    const d = await getDashboardById(dashboardId)
    dashboard.value = d || dashboard.value
    const c = await getDashboardCharts(dashboardId)
    charts.value = c || []
    // 加载图表预览数据
    await loadChartData()
  } catch { ElMessage.error('加载大屏失败') }
}

const loadDatasources = async () => {
  try {
    const res = await getEtlDatasources()
    datasources.value = res || []
  } catch { /* ignore */ }
}

/** 加载所有图表的预览数据 */
const loadChartData = async () => {
  for (const chart of charts.value) {
    if (!chart.id || !chart.chartType || chart.chartType === 'text') continue
    try {
      const data = await queryChartData(chart.id, { _limit: 20 })
      let displayValue = '--'
      if (data && data.length > 0 && chart.chartType === 'number') {
        const row = data[0]
        const keys = Object.keys(row)
        displayValue = row[keys[keys.length - 1]] !== null ? String(row[keys[keys.length - 1]]) : '--'
      }
      chartDataMap.value[chart.id] = { data: data || [], displayValue }
    } catch {
      chartDataMap.value[chart.id] = { data: [], displayValue: 'err' }
    }
  }
  await nextTick()
  // 触发所有图表的echarts渲染
  charts.value.forEach(chart => {
    if (chart.id) {
      const dataEntry = chartDataMap.value[chart.id]
      if (dataEntry && dataEntry.data.length > 0) {
        // ref callback 会在DOM更新后自动调用 initPreviewChart
      }
    }
  })
}

const initPreviewChart = (el: HTMLElement | null, chart: any) => {
  if (!el || !chart || !chart.id) return
  const chartId = chart.id
  // 销毁旧实例
  const old = previewChartInstances.get(chartId)
  if (old) { old.dispose(); previewChartInstances.delete(chartId) }

  const dataEntry = chartDataMap.value[chartId]
  if (!dataEntry || !dataEntry.data || dataEntry.data.length === 0) {
    el.innerHTML = `<div style="color:rgba(255,255,255,.3);padding:10px;font-size:11px;text-align:center">预览无数据</div>`
    el.style.display = 'flex'
    el.style.alignItems = 'center'
    el.style.justifyContent = 'center'
    return
  }

  const instance = echarts.init(el, undefined, { renderer: 'canvas' })
  previewChartInstances.set(chartId, instance)

  const option = buildChartOption(
    chart.chartType, chart.title || '',
    dataEntry.data,
    chart.xField, chart.yField, chart.groupField,
    chart.chartOption,
  )
  instance.setOption(option, true)

  const ro = new ResizeObserver(() => instance.resize())
  ro.observe(el)
  ;(instance as any).__ro = ro
}

const addChart = async () => {
  if (!newChart.title) { ElMessage.warning('请输入图表标题'); return }
  addingChart.value = true
  try {
    await apiAddChart(newChart as DashboardChart)
    ElMessage.success('添加成功')
    // 重新加载图表
    const c = await getDashboardCharts(dashboardId)
    charts.value = c || []
    newChart.title = ''; newChart.querySql = ''; newChart.xField = ''; newChart.yField = ''
  } catch (e: any) { ElMessage.error(e.message || '添加失败') }
  finally { addingChart.value = false }
}

const removeChart = async (idx: number) => {
  const chart = charts.value[idx]
  if (!chart.id) return
  try {
    await apiRemoveChart(chart.id)
    charts.value.splice(idx, 1)
    ElMessage.success('删除成功')
  } catch (e: any) { ElMessage.error(e.message || '删除失败') }
}

const saveCharts = async () => {
  saving.value = true
  try {
    // 更新大屏基本信息
    const { updateDashboard } = await import('@/api/dashboard')
    await updateDashboard(dashboard.value)
    ElMessage.success('保存成功')
  } catch (e: any) { ElMessage.error(e.message || '保存失败') }
  finally { saving.value = false }
}

const previewDashboard = () => {
  router.push({ name: 'dashboardView', params: { id: dashboardId } })
}

const handleBack = () => { router.push({ name: 'dashboardManagement' }) }

onMounted(() => { loadDashboard(); loadDatasources() })

onUnmounted(() => {
  previewChartInstances.forEach((instance, id) => {
    const ro = (instance as any).__ro
    if (ro) ro.disconnect()
    instance.dispose()
  })
  previewChartInstances.clear()
})
</script>

<style scoped>
.dashboard-designer { height: calc(100vh - 60px); display: flex; flex-direction: column; }
.designer-header { padding: 12px 20px; background: #fff; border-bottom: 1px solid #dcdfe6; display: flex; align-items: center; }
.designer-title { margin: 0; font-size: 18px; }
.designer-body { flex: 1; display: flex; overflow: hidden; }
.designer-sidebar { width: 340px; background: #fff; border-right: 1px solid #dcdfe6; overflow-y: auto; flex-shrink: 0; }
.sidebar-content { padding: 12px; }
.chart-item { padding: 8px 12px; border: 1px solid #ebeef5; border-radius: 6px; margin-bottom: 6px; cursor: pointer; transition: all .2s; }
.chart-item:hover { border-color: #409eff; }
.chart-item.active { border-color: #409eff; background: #ecf5ff; }
.chart-item-title { font-size: 13px; font-weight: 500; margin-bottom: 4px; }
.chart-item-type { display: flex; justify-content: space-between; align-items: center; }
.designer-canvas { flex: 1; padding: 20px; overflow-y: auto; display: flex; align-items: flex-start; justify-content: center; }
.chart-grid { display: grid; grid-template-columns: repeat(12, 1fr); gap: 12px; width: 100%; max-width: 1400px; }
.chart-preview { background: rgba(255,255,255,.08); border: 2px solid transparent; border-radius: 8px; display: flex; flex-direction: column; transition: all .2s; min-height: 120px; }
.chart-preview:hover { border-color: rgba(64,158,255,.5); }
.chart-preview.selected { border-color: #409eff; }
.chart-preview-header { display: flex; justify-content: space-between; align-items: center; padding: 6px 10px; background: rgba(0,0,0,.2); border-radius: 6px 6px 0 0; }
.chart-preview-title { color: #fff; font-size: 12px; }
.chart-preview-body { flex: 1; display: flex; align-items: center; justify-content: center; color: rgba(255,255,255,.4); font-size: 13px; padding: 10px; }
.number-value-preview { font-size: 36px; font-weight: 700; color: #409eff; }
.chart-text-preview { font-size: 12px; color: rgba(255,255,255,.5); word-break: break-all; }
.chart-echarts-preview { width: 100%; height: 100%; min-height: 80px; }
</style>
