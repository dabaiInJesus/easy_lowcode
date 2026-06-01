<template>
  <div class="etl-task-management">
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input v-model="searchForm.keyword" placeholder="任务名称/编码" clearable @keyup.enter="handleSearch" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleCreate">新建ETL任务</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="taskName" label="任务名称" min-width="160" />
        <el-table-column prop="taskCode" label="任务编码" width="150" />
        <el-table-column prop="sourceDatasourceName" label="源数据源" width="150" show-overflow-tooltip />
        <el-table-column prop="sourceTable" label="源表" width="140" />
        <el-table-column label="→" width="40" align="center">→</el-table-column>
        <el-table-column prop="targetDatasourceName" label="目标数据源" width="150" show-overflow-tooltip />
        <el-table-column prop="targetTable" label="目标表" width="140" />
        <el-table-column prop="writeMode" label="写入模式" width="100">
          <template #default="{ row }">
            <el-tag :type="getWriteModeTag(row.writeMode)" size="small">{{ row.writeMode }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="scheduleType" label="调度" width="80">
          <template #default="{ row }"><el-tag size="small">{{ getScheduleTypeLabel(row.scheduleType) || row.scheduleType }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ (row.status === 1 ? '启用' : '禁用') }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="调度" width="100">
          <template #default="{ row }">
            <el-switch
              v-if="row.scheduleType && row.scheduleType !== 'MANUAL'"
              :model-value="row.status === 1"
              @click.stop
              @change="(val: boolean) => handleToggleSchedule(row, val)"
              size="small"
            />
            <span v-else style="color:#909399;font-size:12px">手动</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="350" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleExecute(row)" :loading="execLoading === row.id">执行</el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" @click="handleHistory(row)">历史</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-model:current-page="pagination.current" v-model:page-size="pagination.size"
        :total="pagination.total" :page-sizes="[10,20,50,100]" layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData" @current-change="loadData" class="pagination" />
    </el-card>

    <!-- 创建/编辑对话框 -->
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑ETL任务' : '新建ETL任务'" width="800px" top="8vh"
      :close-on-click-modal="false" @close="handleDialogClose">
      <el-steps :active="currentStep" finish-status="success" style="margin-bottom: 20px">
        <el-step title="基本信息" />
        <el-step title="数据映射" />
        <el-step title="调度配置" />
      </el-steps>

      <!-- Step 1: 基本信息 -->
      <div v-show="currentStep === 0">
        <el-form ref="formRef1" :model="formData" :rules="formRules" label-width="120px">
          <el-form-item label="任务名称" prop="taskName">
            <el-input v-model="formData.taskName" placeholder="请输入任务名称" />
          </el-form-item>
          <el-form-item label="任务编码" prop="taskCode">
            <el-input v-model="formData.taskCode" placeholder="唯一标识" />
          </el-form-item>
          <el-divider content-position="left">源数据配置</el-divider>
          <el-form-item label="源数据源" prop="sourceDatasourceId">
            <el-select v-model="formData.sourceDatasourceId" placeholder="选择源数据源" style="width:100%" @change="handleDsChange('source')">
              <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="读取模式">
            <el-radio-group v-model="formData.readMode">
              <el-radio value="TABLE">全表读取</el-radio>
              <el-radio value="SQL">自定义SQL</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="formData.readMode === 'TABLE'" label="源表名" prop="sourceTable">
            <el-select v-model="formData.sourceTable" placeholder="选择或输入表名" style="width:100%" filterable allow-create>
              <el-option v-for="t in sourceTables" :key="t" :label="t" :value="t" />
            </el-select>
          </el-form-item>
          <el-form-item v-if="formData.readMode === 'SQL'" label="查询SQL" prop="sourceSql">
            <el-input v-model="formData.sourceSql" type="textarea" :rows="4" placeholder="SELECT * FROM ..." />
          </el-form-item>
          <el-divider content-position="left">目标数据配置</el-divider>
          <el-form-item label="目标数据源" prop="targetDatasourceId">
            <el-select v-model="formData.targetDatasourceId" placeholder="选择目标数据源" style="width:100%" @change="handleDsChange('target')">
              <el-option v-for="ds in datasourceList" :key="ds.id" :label="ds.name" :value="ds.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="目标表名" prop="targetTable">
            <el-input v-model="formData.targetTable" placeholder="目标表名（需提前创建）" />
          </el-form-item>
          <el-form-item label="写入模式">
            <el-radio-group v-model="formData.writeMode">
              <el-radio value="INSERT">追加插入</el-radio>
              <el-radio value="MERGE">合并</el-radio>
              <el-radio value="TRUNCATE">清空后插入</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="批处理大小">
            <el-input-number v-model="formData.batchSize" :min="100" :max="10000" :step="100" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="formData.remark" type="textarea" :rows="2" placeholder="备注" />
          </el-form-item>
        </el-form>
      </div>

      <!-- Step 2: 数据映射 -->
      <div v-show="currentStep === 1">
        <el-alert title="配置源字段与目标字段的映射关系（留空则按同名自动映射）" type="info" :closable="false" style="margin-bottom:15px" />
        <el-table :data="fieldMappingTable" border stripe max-height="400" size="small">
          <el-table-column label="源字段" width="180">
            <template #default="{ row }">
              <el-select v-model="row.source" filterable allow-create placeholder="源字段" style="width:100%">
                <el-option v-for="col in sourceColumnList" :key="col.columnName" :label="col.columnName" :value="col.columnName" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="→" width="40" align="center">→</el-table-column>
          <el-table-column label="目标字段" width="180">
            <template #default="{ row }">
              <el-input v-model="row.target" placeholder="目标字段" />
            </template>
          </el-table-column>
          <el-table-column label="转换类型" width="120">
            <template #default="{ row }">
              <el-select v-model="row.transformType" placeholder="无" size="small">
                <el-option label="无" value="NONE" />
                <el-option label="函数转换" value="FUNC" />
                <el-option label="表达式" value="EXPR" />
              </el-select>
            </template>
          </el-table-column>
          <el-table-column label="转换规则" min-width="150">
            <template #default="{ row }">
              <el-input v-model="row.transformRule" placeholder="函数/表达式" size="small" />
            </template>
          </el-table-column>
          <el-table-column label="操作" width="60" fixed="right">
            <template #default="{ $index }">
              <el-button link type="danger" size="small" @click="fieldMappingTable.splice($index, 1)">删除</el-button>
            </template>
          </el-table-column>
        </el-table>
        <el-button size="small" style="margin-top:10px" @click="fieldMappingTable.push({source:'',target:'',transformType:'NONE',transformRule:''})">添加映射</el-button>
        <el-button size="small" style="margin-top:10px;margin-left:8px" @click="autoMapFields">自动匹配同名映射</el-button>
      </div>

      <!-- Step 3: 调度配置 -->
      <div v-show="currentStep === 2">
        <el-form label-width="120px">
          <el-form-item label="调度方式">
            <el-radio-group v-model="formData.scheduleType">
              <el-radio value="MANUAL">手动执行</el-radio>
              <el-radio value="CRON">定时执行</el-radio>
              <el-radio value="INTERVAL">间隔执行</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item v-if="formData.scheduleType === 'CRON'" label="CRON表达式">
            <el-input v-model="formData.cronExpression" placeholder="如：0 0 2 * * ? (每天凌晨2点)" />
          </el-form-item>
          <el-form-item v-if="formData.scheduleType === 'INTERVAL'" label="间隔(秒)">
            <el-input-number v-model="formData.intervalSeconds" :min="60" :max="86400" />
          </el-form-item>
          <el-form-item label="跳过错误行">
            <el-switch v-model="formData.skipError" :active-value="1" :inactive-value="0" />
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="formData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button v-if="currentStep < 2" type="primary" @click="currentStep++">下一步</el-button>
        <el-button v-if="currentStep === 2" type="primary" @click="handleSubmit" :loading="submitLoading">确定</el-button>
      </template>
    </el-dialog>

    <!-- 执行历史对话框 -->
    <el-dialog v-model="historyVisible" title="执行历史" width="900px" top="10vh">
      <el-table :data="historyList" border stripe v-loading="historyLoading" size="small">
        <el-table-column prop="id" label="日志ID" width="180" />
        <el-table-column prop="execStatus" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="getExecStatusTagType(row.execStatus)" size="small">
              {{ getExecStatusLabel(row.execStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="180" />
        <el-table-column prop="endTime" label="结束时间" width="180" />
        <el-table-column prop="readCount" label="读取" width="80" />
        <el-table-column prop="writeCount" label="写入" width="80" />
        <el-table-column prop="skipCount" label="跳过" width="80" />
        <el-table-column prop="errorMessage" label="错误信息" min-width="200" show-overflow-tooltip />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import { getEtlTaskPage, createEtlTask, updateEtlTask, deleteEtlTask, executeEtlTask, getEtlTaskHistory, getEtlTaskSourceColumns, getEtlDatasources, toggleEtlTaskSchedule, scanTableColumns, type EtlTask } from '@/api/etl'
import { getDataSourcePage } from '@/api/datasource'

const searchForm = reactive({ keyword: '' })
const tableData = ref<EtlTask[]>([])
const loading = ref(false)
const execLoading = ref<number | null>(null)
const pagination = reactive({ current: 1, size: 10, total: 0 })

const dialogVisible = ref(false)
const isEdit = ref(false)
const currentStep = ref(0)
const formRef1 = ref<FormInstance>()
const submitLoading = ref(false)

const datasourceList = ref<any[]>([])
const sourceTables = ref<string[]>([])
const sourceColumnList = ref<any[]>([])

const fieldMappingTable = ref<any[]>([])

const formData = reactive<Partial<EtlTask>>({
  taskName: '', taskCode: '', sourceDatasourceId: undefined, sourceTable: '', sourceSql: '',
  readMode: 'TABLE', targetDatasourceId: undefined, targetTable: '', writeMode: 'INSERT',
  batchSize: 1000, scheduleType: 'MANUAL', cronExpression: '', intervalSeconds: 3600,
  skipError: 0, status: 1, remark: '',
  fieldMapping: '', transformRules: ''
})

const formRules: FormRules = {
  taskName: [{ required: true, message: '请输入任务名称', trigger: 'blur' }],
  taskCode: [{ required: true, message: '请输入任务编码', trigger: 'blur' }],
  sourceDatasourceId: [{ required: true, message: '请选择源数据源', trigger: 'change' }],
  targetDatasourceId: [{ required: true, message: '请选择目标数据源', trigger: 'change' }],
  targetTable: [{ required: true, message: '请输入目标表名', trigger: 'blur' }],
}

const historyVisible = ref(false)
const historyList = ref<any[]>([])
const historyLoading = ref(false)

const loadData = async () => {
  loading.value = true
  try {
    const res = await getEtlTaskPage(pagination.current, pagination.size, searchForm.keyword)
    tableData.value = res.records
    pagination.total = res.total
  } catch { ElMessage.error('加载失败') }
  finally { loading.value = false }
}

const loadDatasources = async () => {
  try {
    const res = await getEtlDatasources()
    datasourceList.value = res || []
  } catch { /* ignore */ }
}

const handleSearch = () => { pagination.current = 1; loadData() }
const handleReset = () => { searchForm.keyword = ''; handleSearch() }

const handleCreate = () => {
  isEdit.value = false; currentStep.value = 0; dialogVisible.value = true
  loadDatasources()
  Object.assign(formData, { taskName: '', taskCode: '', sourceDatasourceId: undefined, sourceTable: '', sourceSql: '', readMode: 'TABLE', targetDatasourceId: undefined, targetTable: '', writeMode: 'INSERT', batchSize: 1000, scheduleType: 'MANUAL', cronExpression: '', intervalSeconds: 3600, skipError: 0, status: 1, remark: '', fieldMapping: '', transformRules: '' })
  fieldMappingTable.value = []
}

const handleEdit = async (row: EtlTask) => {
  isEdit.value = true; currentStep.value = 0
  await loadDatasources()
  Object.assign(formData, row)
  if (row.fieldMapping) {
    try { fieldMappingTable.value = JSON.parse(row.fieldMapping) } catch { fieldMappingTable.value = [] }
  } else { fieldMappingTable.value = [] }
  dialogVisible.value = true
}

const handleDelete = async (row: EtlTask) => {
  try {
    await ElMessageBox.confirm('确定删除该ETL任务？', '提示', { type: 'warning' })
    await deleteEtlTask(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch { /* cancel */ }
}

const handleExecute = async (row: EtlTask) => {
  execLoading.value = row.id!
  try {
    await executeEtlTask(row.id!)
    ElMessage.success('任务已提交执行')
  } catch (e: any) { ElMessage.error(e.message || '执行失败') }
  finally { execLoading.value = null }
}

const handleToggleSchedule = async (row: EtlTask, enabled: boolean) => {
  try {
    await toggleEtlTaskSchedule(row.id!, enabled)
    ElMessage.success(enabled ? '调度已开启' : '调度已关闭')
    loadData()
  } catch (e: any) { ElMessage.error(e.message || '操作失败') }
}

const handleDsChange = (type: 'source' | 'target') => {
  if (type === 'source' && formData.sourceDatasourceId) {
    // 扫描源表列表
    getDataSourcePage(1, 100).then(async () => {
      const ds = datasourceList.value.find(d => d.id === formData.sourceDatasourceId)
      if (ds) {
        try {
          const { scanTables } = await import('@/api/datasource')
          const tables = await scanTables(ds.id)
          sourceTables.value = (tables || []).map((t: any) => t.tableName)
        } catch { sourceTables.value = [] }
      }
    })
  }
}

const autoMapFields = async () => {
  if (!formData.sourceDatasourceId || !formData.sourceTable || !formData.targetDatasourceId || !formData.targetTable) {
    ElMessage.warning('请先选择源数据源、源表、目标数据源和目标表')
    return
  }
  try {
    // 扫描源表字段
    const sourceCols = await scanTableColumns(formData.sourceDatasourceId, formData.sourceTable)
    // 扫描目标表字段
    const targetCols = await scanTableColumns(formData.targetDatasourceId, formData.targetTable)
    
    const sourceColumnNames = sourceCols.map((c: any) => c.columnName as string)
    const targetColumnNames = targetCols.map((c: any) => c.columnName as string)
    
    // 同名列自动映射
    const mappings: any[] = []
    sourceColumnNames.forEach(srcCol => {
      if (targetColumnNames.includes(srcCol)) {
        mappings.push({ source: srcCol, target: srcCol, transformType: 'NONE', transformRule: '' })
      }
    })
    
    if (mappings.length > 0) {
      fieldMappingTable.value = mappings
      ElMessage.success(`自动匹配成功，共 ${mappings.length} 个字段`)
    } else {
      ElMessage.warning('未找到同名字段，请手动配置映射')
    }
  } catch (e: any) {
    ElMessage.error('获取字段列表失败: ' + (e.message || ''))
  }
}

const handleSubmit = async () => {
  if (!formRef1.value) return
  await formRef1.value.validate(async (valid) => {
    if (!valid) return
    submitLoading.value = true
    try {
      formData.fieldMapping = JSON.stringify(fieldMappingTable.value.filter(m => m.source && m.target))
      if (isEdit.value) {
        await updateEtlTask(formData as EtlTask)
        ElMessage.success('更新成功')
      } else {
        await createEtlTask(formData as EtlTask)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (e: any) { ElMessage.error(e.message || '操作失败') }
    finally { submitLoading.value = false }
  })
}

const handleDialogClose = () => {
  currentStep.value = 0
  formRef1.value?.resetFields()
  fieldMappingTable.value = []
}

const handleHistory = async (row: EtlTask) => {
  historyVisible.value = true
  historyLoading.value = true
  try {
    const res = await getEtlTaskHistory(row.id!)
    historyList.value = res || []
  } finally { historyLoading.value = false }
}

const getWriteModeTag = (mode: string) => ({ INSERT: '', MERGE: 'warning', REPLACE: 'danger', TRUNCATE: 'danger' })[mode] || ''
const getScheduleTypeLabel = (type: string) => ({ MANUAL: '手动', CRON: '定时', INTERVAL: '间隔' }[type] || type)
const getExecStatusTagType = (status: string) => ({ RUNNING: 'warning', SUCCESS: 'success', FAILED: 'danger', STOPPED: 'info' }[status] || 'info')
const getExecStatusLabel = (status: string) => ({ RUNNING: '运行中', SUCCESS: '成功', FAILED: '失败', STOPPED: '已停止' }[status] || status)

onMounted(() => { loadData() })
</script>

<style scoped>
.etl-task-management { padding: 20px; }
.search-card { margin-bottom: 20px; }
.table-card { margin-bottom: 20px; }
.pagination { margin-top: 20px; justify-content: flex-end; }
</style>
