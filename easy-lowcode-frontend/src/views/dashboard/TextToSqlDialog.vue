<template>
  <el-dialog
    v-model="visible"
    title="🤖 AI 生成图表"
    width="900px"
    :close-on-click-modal="false"
    destroy-on-close
  >
    <el-steps :active="step" finish-status="success" style="margin-bottom:24px">
      <el-step title="选择数据源" />
      <el-step title="描述需求" />
      <el-step title="预览确认" />
    </el-steps>

    <!-- Step 1: 选择数据源 -->
    <div v-if="step === 0">
      <el-form label-width="110px" size="default">
        <el-form-item label="数据源">
          <el-select
            v-model="form.datasourceId"
            placeholder="请选择数据源"
            style="width:100%"
            filterable
            @change="onDsChange"
          >
            <el-option
              v-for="ds in datasources"
              :key="ds.id"
              :label="`${ds.name} (${ds.dbType})`"
              :value="ds.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据表">
          <el-select
            v-model="form.tableName"
            placeholder="请输入或选择表名"
            allow-create
            filterable
            default-first-option
            style="width:100%"
            :loading="loadingTables"
          >
            <el-option
              v-for="t in tables"
              :key="t"
              :label="t"
              :value="t"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="数据库类型">
          <el-input :model-value="dbTypeLabel" disabled placeholder="选择数据源后自动识别" />
        </el-form-item>
      </el-form>
    </div>

    <!-- Step 2: 描述需求 -->
    <div v-if="step === 1">
      <el-alert
        v-if="form.tableName"
        :title="`表名: ${form.tableName}`"
        type="info"
        :closable="false"
        style="margin-bottom:16px"
      />
      <el-form label-width="110px" size="default">
        <el-form-item label="你想看什么？">
          <el-input
            v-model="form.question"
            type="textarea"
            :rows="4"
            placeholder="例如：近7天每日销售额趋势、全国各省份订单量分布、TOP10 畅销商品"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>
        <el-form-item label="返回条数">
          <el-input-number v-model="form.limit" :min="5" :max="1000" />
        </el-form-item>
      </el-form>
      <div class="prompt-hints">
        <span class="hint-label">参考提示：</span>
        <el-tag
          v-for="hint in promptHints"
          :key="hint"
          class="hint-tag"
          @click="form.question = hint"
        >{{ hint }}</el-tag>
      </div>
    </div>

    <!-- Step 3: 预览确认 -->
    <div v-if="step === 2">
      <!-- SQL 预览 -->
      <el-alert
        v-if="result.sql"
        :title="result.sql"
        type="success"
        :closable="false"
        style="margin-bottom:12px;font-family:monospace;font-size:12px;white-space:pre-wrap"
        show-icon
      />
      <el-alert
        v-if="result.errorMessage"
        :title="result.errorMessage"
        type="error"
        :closable="false"
        style="margin-bottom:12px"
        show-icon
      />

      <!-- AI 推荐 -->
      <div v-if="result.recommendedChartType" class="ai-recommend">
        <div class="recommend-header">
          <span class="recommend-title">🤖 AI 推荐图表</span>
          <el-tag type="success">{{ result.recommendedChartType }}</el-tag>
          <el-tag type="info" v-if="result.rowCount">{{ result.rowCount }} 条数据</el-tag>
        </div>
      </div>

      <!-- 数据预览表格 -->
      <div v-if="result.data && result.data.length > 0" class="data-preview">
        <div class="preview-header">
          <span>数据预览（前 10 行）</span>
          <el-tag size="small" type="info">共 {{ result.rowCount }} 条</el-tag>
        </div>
        <el-table :data="result.data.slice(0, 10)" stripe size="small" max-height="240" style="font-size:12px">
          <el-table-column
            v-for="col in Object.keys(result.data[0] || {})"
            :key="col"
            :prop="col"
            :label="col"
            min-width="100"
            show-overflow-tooltip
          />
        </el-table>
      </div>

      <!-- 图表预览 -->
      <div v-if="result.data && result.data.length > 0 && result.recommendedChartType" class="chart-preview">
        <div class="preview-header">
          <span>图表预览</span>
          <span class="preview-chart-type">{{ result.recommendedChartType }} 图</span>
        </div>
        <div ref="previewChartRef" class="echarts-container" />
      </div>
    </div>

    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button v-if="step > 0" @click="step--">上一步</el-button>
      <el-button v-if="step === 0" type="primary" :disabled="!form.datasourceId || !form.tableName" @click="nextToQuestion">
        下一步
      </el-button>
      <el-button v-if="step === 1" type="primary" :loading="generating" @click="generateSql">
        🚀 生成 SQL
      </el-button>
      <el-button v-if="step === 2" type="primary" :disabled="!result.success || !result.sql" @click="confirmAdd">
        ✅ 添加图表
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { getDataSourceList, getTableColumns, textToSql } from '@/api/dashboard'
import { buildChartOption } from '@/utils/chartRenderer'

const props = defineProps<{
  modelValue: boolean
  dashboardId: number
}>()

const emit = defineEmits<{
  'update:modelValue': [val: boolean]
  'chart-added': [chart: any]
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (v) => emit('update:modelValue', v),
})

const step = ref(0)
const generating = ref(false)
const loadingTables = ref(false)

const datasources = ref<any[]>([])
const tables = ref<string[]>([])
const previewChartRef = ref<HTMLElement | null>(null)

const form = reactive({
  datasourceId: undefined as number | undefined,
  tableName: '',
  question: '',
  limit: 100,
})

const result = reactive<any>({
  sql: '',
  data: [],
  rowCount: 0,
  recommendedChartType: '',
  recommendedEchartsOption: '',
  success: false,
  errorMessage: '',
})

const dbTypeLabel = computed(() => {
  const ds = datasources.value.find(d => d.id === form.datasourceId)
  return ds ? ds.dbType : ''
})

const promptHints = [
  '近7天每日销售额趋势',
  '各省份订单量分布',
  'TOP10 畅销商品',
  '本月各品类销售占比',
  '每小时访问量变化',
]

// 监听打开，加载数据源列表
watch(visible, async (v) => {
  if (v) {
    step.value = 0
    Object.assign(result, { sql: '', data: [], rowCount: 0, recommendedChartType: '', success: false, errorMessage: '' })
    form.question = ''
    form.tableName = ''
    form.datasourceId = undefined
    tables.value = []
    try {
      datasources.value = await getDataSourceList()
    } catch {
      ElMessage.error('加载数据源失败')
    }
  }
})

async function onDsChange() {
  form.tableName = ''
  tables.value = []
  if (!form.datasourceId) return
  // 尝试获取表的列信息，如果支持的话
  loadingTables.value = true
  try {
    // 暂时不加载表列表，让用户手动输入表名
    // 如果后端有表列表 API，可以用这个
    // const cols = await getTableColumns(form.datasourceId, '')
  } finally {
    loadingTables.value = false
  }
}

function nextToQuestion() {
  if (!form.tableName) {
    ElMessage.warning('请输入表名')
    return
  }
  step.value = 1
}

async function generateSql() {
  if (!form.question.trim()) {
    ElMessage.warning('请输入你想看的数据描述')
    return
  }
  generating.value = true
  result.errorMessage = ''
  try {
    const res = await textToSql({
      datasourceId: form.datasourceId!,
      tableName: form.tableName,
      question: form.question,
      limit: form.limit,
      execute: true,
    })
    Object.assign(result, res)
    if (res.success) {
      step.value = 2
      await nextTick()
      renderPreviewChart()
    } else {
      ElMessage.error(res.errorMessage || '生成失败')
    }
  } catch (e: any) {
    result.errorMessage = e.message || '请求失败'
    ElMessage.error(result.errorMessage)
  } finally {
    generating.value = false
  }
}

function renderPreviewChart() {
  if (!previewChartRef.value || !result.data?.length || !result.recommendedChartType) return
  const chart = echarts.getInstanceByDom(previewChartRef.value)
  if (chart) chart.dispose()

  const inst = echarts.init(previewChartRef.value)
  const columns = Object.keys(result.data[0])
  const xField = columns[0]
  const yField = columns[columns.length - 1]

  let option: any = {}
  try {
    if (result.recommendedEchartsOption) {
      option = JSON.parse(result.recommendedEchartsOption)
    }
  } catch { /* ignore */ }

  if (Object.keys(option).length === 0) {
    option = buildChartOption(result.recommendedChartType, '', result.data, xField, yField)
  }

  // 自动填入 x/y 数据
  if (result.recommendedChartType === 'line' || result.recommendedChartType === 'bar') {
    option.xAxis = option.xAxis || {}
    option.xAxis.data = result.data.map((d: any) => d[xField])
    option.series = option.series || []
    if (option.series[0]) {
      option.series[0].data = result.data.map((d: any) => d[yField])
    }
  } else if (result.recommendedChartType === 'pie') {
    option.series = option.series || [{}]
    if (option.series[0]) {
      option.series[0].data = result.data.map((d: any) => ({
        name: d[xField],
        value: d[yField],
      }))
    }
  }

  inst.setOption(option, true)
}

async function confirmAdd() {
  if (!result.sql || !result.success) return

  // 计算合适的宽高（基于图表类型）
  const width = result.recommendedChartType === 'number' ? 3 : 6
  const height = result.recommendedChartType === 'number' ? 2 : 4

  emit('chart-added', {
    dashboardId: props.dashboardId,
    title: form.question.length > 30 ? form.question.slice(0, 30) + '...' : form.question,
    chartType: result.recommendedChartType,
    datasourceId: form.datasourceId,
    querySql: result.sql,
    xField: Object.keys(result.data[0] || {})[0] || '',
    yField: Object.keys(result.data[0] || {})[Object.keys(result.data[0] || {}).length - 1] || '',
    width,
    height,
    chartOption: result.recommendedEchartsOption || '',
  })

  visible.value = false
  ElMessage.success('图表已添加到画布！')
}
</script>

<style scoped>
.prompt-hints { display: flex; flex-wrap: wrap; gap: 6px; align-items: center; margin-top: 4px; }
.hint-label { font-size: 12px; color: #909399; }
.hint-tag { cursor: pointer; }

.ai-recommend {
  background: #f0f9eb;
  border: 1px solid #e1f3d8;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}
.recommend-header { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.recommend-title { font-weight: 600; font-size: 14px; color: #67c23a; }
.recommend-reason { margin: 0; font-size: 13px; color: #606266; }

.data-preview {
  background: #f5f7fa;
  border-radius: 6px;
  padding: 12px;
  margin-bottom: 12px;
}
.preview-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 8px; font-size: 13px; color: #606266; }
.preview-chart-type { font-size: 12px; color: #909399; }

.chart-preview { margin-top: 8px; }
.echarts-container { height: 260px; background: #fff; border-radius: 6px; }
</style>
