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
        <el-menu :default-active="activeMenu" @select="(idx: string) => activeMenu = idx">
          <el-menu-item index="charts">
            <el-icon><PieChart /></el-icon>
            <span>图表列表</span>
          </el-menu-item>
          <el-menu-item index="add">
            <el-icon><Plus /></el-icon>
            <span>添加图表</span>
          </el-menu-item>
          <el-menu-item index="ai">
            <el-icon><PieChart /></el-icon>
            <span>🤖 AI 生成</span>
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
                <el-option label="雷达图" value="radar" />
                <el-option label="仪表盘" value="gauge" />
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

        <!-- AI 生成图表 -->
        <div v-if="activeMenu === 'ai'" class="sidebar-content ai-sidebar">
          <div class="ai-intro">
            <p>🤖 使用 AI 智能生成图表，只需描述你想看的数据即可。</p>
          </div>
          <el-button type="primary" style="width:100%;margin-bottom:12px" @click="showTextToSqlDialog = true">
            🚀 打开 AI 生成器
          </el-button>
          <el-divider content-position="center">使用示例</el-divider>
          <div class="example-list">
            <div class="example-item" @click="quickGenerate('近7天每日销售额趋势')">
              <span class="example-icon">📈</span>
              <span>"近7天每日销售额趋势"</span>
            </div>
            <div class="example-item" @click="quickGenerate('各省份订单量分布')">
              <span class="example-icon">🗺️</span>
              <span>"各省份订单量分布"</span>
            </div>
            <div class="example-item" @click="quickGenerate('TOP10 畅销商品')">
              <span class="example-icon">🏆</span>
              <span>"TOP10 畅销商品"</span>
            </div>
            <div class="example-item" @click="quickGenerate('本月各品类销售占比')">
              <span class="example-icon">🥧</span>
              <span>"本月各品类销售占比"</span>
            </div>
          </div>
          <el-divider content-position="center">支持的数据源</el-divider>
          <div class="datasource-tags">
            <el-tag size="small" type="success">MySQL</el-tag>
            <el-tag size="small" type="success">PostgreSQL</el-tag>
            <el-tag size="small" type="success">Oracle</el-tag>
            <el-tag size="small" type="success">Hive</el-tag>
            <el-tag size="small" type="info">SQLServer</el-tag>
          </div>
        </div>
      </div>

        <!-- 画布 -->
      <div class="designer-canvas" :style="{ backgroundColor: dashboard.backgroundColor || '#0a1628' }">
        <el-empty v-if="charts.length === 0" description="左侧添加图表开始设计" :image-size="80" style="color:#fff" />
        <div class="chart-grid">
          <div
            v-for="(chart, idx) in charts"
            :key="chart.id"
            class="chart-preview"
            :class="{
              selected: selectedChartIdx === idx,
              'drag-over': dragOverIdx === idx,
              'is-dragging': draggingIdx === idx,
            }"
            :style="{
              gridColumn: `span ${chart.width || 4}`,
              gridRow: `span ${chart.height || 3}`,
            }"
            draggable="true"
            @click="selectedChartIdx = idx"
            @dragstart="onDragStart(idx, $event)"
            @dragend="onDragEnd"
            @dragover.prevent="onDragOver(idx)"
            @drop="onDrop(idx)"
          >
            <div class="chart-preview-header">
              <span class="chart-preview-title">{{ chart.title }}</span>
              <el-tag size="small">{{ chart.chartType }}</el-tag>
            </div>
            <div class="chart-preview-body">
              <div v-if="chart.chartType === 'number'" class="number-value-preview">
                {{ chartDataMap[chart.id!]?.displayValue || '--' }}
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

    <!-- Text-to-SQL AI 生成对话框 -->
    <TextToSqlDialog
      v-model="showTextToSqlDialog"
      :dashboard-id="dashboardId"
      @chart-added="handleChartAdded"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { PieChart, Plus, Setting } from '@element-plus/icons-vue'
import { getDashboardById, getDashboardCharts, addChart as apiAddChart, removeChart as apiRemoveChart, queryChartData, type Dashboard, type DashboardChart } from '@/api/dashboard'
import { getEtlDatasources } from '@/api/etl'
import { buildChartOption } from '@/utils/chartRenderer'
import TextToSqlDialog from './TextToSqlDialog.vue'

const route = useRoute()
const router = useRouter()

// 安全获取大屏 ID
const dashboardId = computed(() => {
  const id = Number(route.params.id)
  if (isNaN(id) || id <= 0) {
    return null
  }
  return id
})

// 如果没有有效的大屏 ID，跳转回管理页面
if (dashboardId.value === null) {
  ElMessage.error('无效的大屏ID，正在跳转...')
  router.replace({ name: 'dashboardManagement' })
}

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
  dashboardId: 0, title: '', chartType: 'bar', datasourceId: undefined,
  querySql: '', xField: '', yField: '', width: 4, height: 3
})

const showTextToSqlDialog = ref(false)
const draggingIdx = ref<number | null>(null)
const dragOverIdx = ref<number | null>(null)

const loadDashboard = async () => {
  if (!dashboardId.value) return
  try {
    const d = await getDashboardById(dashboardId.value)
    dashboard.value = d || dashboard.value
    const c = await getDashboardCharts(dashboardId.value)
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
    el.textContent = '预览无数据'
    el.style.color = 'rgba(255,255,255,.3)'
    el.style.padding = '10px'
    el.style.fontSize = '11px'
    el.style.textAlign = 'center'
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
  if (!dashboardId.value) { ElMessage.error('无效的大屏ID'); return }
  addingChart.value = true
  try {
    newChart.dashboardId = dashboardId.value
    await apiAddChart(newChart as DashboardChart)
    ElMessage.success('添加成功')
    // 重新加载图表
    const c = await getDashboardCharts(dashboardId.value)
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
  router.push({ name: 'dashboardView', params: { id: dashboardId.value } })
}

const handleBack = () => { router.push({ name: 'dashboardManagement' }) }

/** AI 快速生成：打开 AI 生成器并预填问题 */
const quickGenerate = (question: string) => {
  activeMenu.value = 'ai'
  showTextToSqlDialog.value = true
}

/** 处理 AI 生成的图表添加 */
const handleChartAdded = async (chart: Partial<DashboardChart>) => {
  try {
    await apiAddChart(chart)
    ElMessage.success('图表添加成功')
    // 重新加载图表
    const c = await getDashboardCharts(dashboardId.value)
    charts.value = c || []
    await loadChartData()
  } catch (e: any) {
    ElMessage.error(e.message || '添加失败')
  }
}

// ========== 拖拽排序 ==========

function onDragStart(idx: number, event: DragEvent) {
  draggingIdx.value = idx
  if (event.dataTransfer) {
    event.dataTransfer.effectAllowed = 'move'
    event.dataTransfer.setData('text/plain', String(idx))
  }
}

function onDragOver(idx: number) {
  dragOverIdx.value = idx
}

function onDrop(targetIdx: number) {
  const srcIdx = draggingIdx.value
  if (srcIdx === null || srcIdx === targetIdx) {
    resetDragState()
    return
  }
  // 数组重排序
  const items = [...charts.value]
  const [moved] = items.splice(srcIdx, 1)
  items.splice(targetIdx, 0, moved)
  charts.value = items
  // 更新选中
  selectedChartIdx.value = targetIdx
  // 保存新排序到后端
  savePositions(items)
  resetDragState()
}

function onDragEnd() {
  resetDragState()
}

function resetDragState() {
  draggingIdx.value = null
  dragOverIdx.value = null
}

async function savePositions(items: DashboardChart[]) {
  const positions = items.map((c, order) => ({
    id: c.id,
    sortOrder: order,
    width: c.width,
    height: c.height,
  }))
  try {
    const { updateChartPositions } = await import('@/api/dashboard')
    await updateChartPositions(positions)
  } catch (e: any) {
    ElMessage.warning('位置保存失败: ' + (e.message || ''))
  }
}

onMounted(() => { loadDashboard(); loadDatasources() })

onUnmounted(() => {
  previewChartInstances.forEach((instance: any, _id: any) => {
    const ro = (instance as any).__ro
    if (ro) ro.disconnect()
    instance.dispose()
  })
  previewChartInstances.clear()
})
</script>

<style scoped>
.dashboard-designer { height: 100vh; display: flex; flex-direction: column; }
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

/* 拖拽状态样式 */
.chart-preview.drag-over {
  border: 2px dashed #409eff !important;
  background: rgba(64, 158, 255, 0.15) !important;
}
.chart-preview.is-dragging {
  opacity: 0.4;
  border: 2px dashed #909399 !important;
}
.chart-preview:hover { cursor: grab; }
.chart-preview:active { cursor: grabbing; }

/* AI 生成器侧边栏 */
.ai-sidebar .ai-intro { background: #f0f9eb; border-radius: 8px; padding: 12px; margin-bottom: 16px; }
.ai-sidebar .ai-intro p { margin: 0; font-size: 13px; color: #606266; line-height: 1.6; }
.ai-sidebar .example-list { display: flex; flex-direction: column; gap: 8px; }
.ai-sidebar .example-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 12px; background: #f5f7fa; border-radius: 6px;
  cursor: pointer; font-size: 13px; color: #606266; transition: all .2s;
}
.ai-sidebar .example-item:hover { background: #ecf5ff; color: #409eff; }
.ai-sidebar .example-icon { font-size: 16px; }
.ai-sidebar .datasource-tags { display: flex; flex-wrap: wrap; gap: 6px; }
</style>
