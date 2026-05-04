<template>
  <div class="table-resource-management">
    <!-- 搜索和操作栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="数据源">
          <el-select
            v-model="searchForm.datasourceId"
            placeholder="请选择数据源"
            clearable
            style="width: 200px"
          >
            <el-option
              v-for="ds in datasourceList"
              :key="ds.id"
              :label="ds.name"
              :value="ds.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入表名、资源编码或API路径"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item label="视图">
          <el-radio-group v-model="viewMode" size="small">
            <el-radio-button value="table">列表</el-radio-button>
            <el-radio-button value="catalog">目录</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="warning" @click="handleScanTables" :disabled="!searchForm.datasourceId">
            扫描表
          </el-button>
          <el-button type="success" @click="handleRegister">注册表资源</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-alert
        v-if="isScanning"
        title="扫描模式：显示数据源中的所有表（包括已注册和未注册的）"
        type="info"
        :closable="false"
        style="margin-bottom: 15px"
      />
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" v-if="!isScanning" />
        <el-table-column prop="datasourceName" label="数据源" min-width="150" v-if="!isScanning" />
        <el-table-column prop="tableName" label="表名" min-width="200" />
        <el-table-column prop="tableComment" label="表注释" min-width="250" show-overflow-tooltip />
        <el-table-column prop="resourceCode" label="资源编码" min-width="150" v-if="!isScanning" />
        <el-table-column prop="apiPath" label="API路径" min-width="200" show-overflow-tooltip v-if="!isScanning" />
        <el-table-column prop="methods" label="支持方法" width="150" v-if="!isScanning">
          <template #default="{ row }">
            <el-tag v-for="method in row.methods?.split(',')" :key="method" size="small" style="margin-right: 5px">
              {{ method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" v-if="!isScanning">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" v-if="!isScanning">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleGenerateApi(row)">
              生成API
            </el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right" v-if="isScanning">
          <template #default="{ row }">
            <el-button size="small" type="success" @click="handleRegisterFromScan(row)">
              注册
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 目录视图 -->
      <div v-if="viewMode === 'catalog'" class="catalog-view">
        <el-row :gutter="20">
          <el-col
            v-for="item in tableData"
            :key="item.id"
            :xs="24"
            :sm="12"
            :md="8"
            :lg="6"
          >
            <el-card class="resource-card" shadow="hover">
              <template #header>
                <div class="card-header">
                  <span class="table-name">{{ item.tableName }}</span>
                  <el-tag :type="item.status === 1 ? 'success' : 'danger'" size="small">
                    {{ item.status === 1 ? '启用' : '禁用' }}
                  </el-tag>
                </div>
              </template>
              <div class="card-body">
                <div class="info-item">
                  <span class="label">数据源：</span>
                  <span class="value">{{ item.datasourceName }}</span>
                </div>
                <div class="info-item">
                  <span class="label">资源编码：</span>
                  <span class="value">{{ item.resourceCode }}</span>
                </div>
                <div class="info-item">
                  <span class="label">API路径：</span>
                  <el-tooltip :content="item.apiPath" placement="top">
                    <span class="value api-path">{{ item.apiPath }}</span>
                  </el-tooltip>
                </div>
                <div class="info-item" v-if="item.tableComment">
                  <span class="label">表注释：</span>
                  <el-tooltip :content="item.tableComment" placement="top">
                    <span class="value">{{ item.tableComment }}</span>
                  </el-tooltip>
                </div>
                <div class="info-item">
                  <span class="label">支持方法：</span>
                  <div class="methods">
                    <el-tag
                      v-for="method in item.methods?.split(',')"
                      :key="method"
                      size="small"
                      type="info"
                    >
                      {{ method }}
                    </el-tag>
                  </div>
                </div>
              </div>
              <template #footer>
                <div class="card-footer">
                  <el-button size="small" type="primary" @click="handleGenerateApi(item)">
                    生成API
                  </el-button>
                  <el-button size="small" @click="handleEdit(item)">编辑</el-button>
                  <el-button size="small" type="danger" @click="handleDelete(item)">删除</el-button>
                </div>
              </template>
            </el-card>
          </el-col>
        </el-row>
        <el-empty v-if="tableData.length === 0" description="暂无数据" />
      </div>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="pagination.current"
        v-model:page-size="pagination.size"
        :total="pagination.total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="handleCurrentChange"
        class="pagination"
      />
    </el-card>

    <!-- 注册表资源对话框 -->
    <el-dialog
      v-model="registerDialogVisible"
      title="注册表资源"
      width="900px"
      @close="handleRegisterDialogClose"
    >
      <el-steps :active="currentStep" finish-status="success">
        <el-step title="选择数据源" />
        <el-step title="选择表" />
        <el-step title="配置接口" />
      </el-steps>

      <!-- 步骤1：选择数据源 -->
      <div v-if="currentStep === 0" class="step-content">
        <el-form label-width="120px">
          <el-form-item label="数据源">
            <el-select
              v-model="selectedDatasourceId"
              placeholder="请选择数据源"
              style="width: 100%"
              @change="(val) => { console.log('选择的数据源ID:', val); handleDatasourceChange(); }"
            >
              <el-option
                v-for="ds in datasourceList"
                :key="ds.id"
                :label="ds.name"
                :value="ds.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
      </div>

      <!-- 步骤2：选择表 -->
      <div v-if="currentStep === 1" class="step-content">
        <el-alert
          title="扫描到的表列表"
          type="info"
          :closable="false"
          style="margin-bottom: 15px"
        />
        <el-table
          :data="tableList"
          border
          stripe
          max-height="400"
          v-loading="tableListLoading"
          @row-click="handleTableSelect"
          highlight-current-row
        >
          <el-table-column type="index" width="50" />
          <el-table-column prop="tableName" label="表名" min-width="200" />
          <el-table-column prop="tableComment" label="表注释" min-width="250" />
        </el-table>
      </div>

      <!-- 步骤3：配置接口 -->
      <div v-if="currentStep === 2" class="step-content">
        <el-form
          ref="registerFormRef"
          :model="registerFormData"
          :rules="registerFormRules"
          label-width="120px"
        >
          <el-form-item label="表名">
            <el-input v-model="registerFormData.tableName" disabled />
          </el-form-item>
          <el-form-item label="表注释">
            <el-input v-model="registerFormData.tableComment" disabled />
          </el-form-item>
          <el-form-item label="资源编码" prop="resourceCode">
            <el-input
              v-model="registerFormData.resourceCode"
              placeholder="例如：r_main_db_user"
            />
            <div class="form-tip">
              格式：r_{数据源编码}_{表名}，避免多库重名冲突
            </div>
          </el-form-item>
          <el-form-item label="API路径" prop="apiPath">
            <el-input
              v-model="registerFormData.apiPath"
              placeholder="例如：/api/user、/api/order"
            />
          </el-form-item>
          <el-form-item label="支持方法">
            <el-checkbox-group v-model="selectedMethods">
              <el-checkbox value="GET">GET（查询）</el-checkbox>
              <el-checkbox value="POST">POST（新增）</el-checkbox>
              <el-checkbox value="PUT">PUT（修改）</el-checkbox>
              <el-checkbox value="DELETE">DELETE（删除）</el-checkbox>
            </el-checkbox-group>
          </el-form-item>
          <el-form-item label="状态">
            <el-radio-group v-model="registerFormData.status">
              <el-radio :value="1">启用</el-radio>
              <el-radio :value="0">禁用</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>

      <template #footer>
        <el-button @click="registerDialogVisible = false">取消</el-button>
        <el-button v-if="currentStep > 0" @click="currentStep--">上一步</el-button>
        <el-button
          v-if="currentStep < 2"
          type="primary"
          @click="handleNextStep"
          :disabled="!canNextStep"
        >
          下一步
        </el-button>
        <el-button
          v-if="currentStep === 2"
          type="primary"
          @click="handleSubmitRegister"
          :loading="submitLoading"
        >
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑表资源"
      width="600px"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="editFormRules"
        label-width="120px"
      >
        <el-form-item label="表名">
          <el-input v-model="editFormData.tableName" disabled />
        </el-form-item>
        <el-form-item label="资源编码" prop="resourceCode">
          <el-input v-model="editFormData.resourceCode" />
        </el-form-item>
        <el-form-item label="API路径" prop="apiPath">
          <el-input v-model="editFormData.apiPath" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="editFormData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitEdit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed, watch } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getTableResourcePage,
  registerTableResource,
  updateTableResource,
  deleteTableResource,
  generateApi,
  type TableResource,
} from '@/api/tableResource'
import { getDataSourcePage, scanTables, type DataSourceConfig } from '@/api/datasource'

// 搜索表单
const searchForm = reactive({
  datasourceId: undefined as number | undefined,
  keyword: '',
  status: undefined as number | undefined,
})

// 视图模式：table（列表）或 catalog（目录）
const viewMode = ref<'table' | 'catalog'>('table')

// 表格数据
const tableData = ref<TableResource[]>([])
const loading = ref(false)
const isScanning = ref(false) // 是否正在扫描模式
const scannedTables = ref<any[]>([]) // 保存扫描到的所有表，用于前端过滤

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

// 数据源列表
const datasourceList = ref<DataSourceConfig[]>([])

// 注册对话框
const registerDialogVisible = ref(false)
const currentStep = ref(0)
const selectedDatasourceId = ref<number>()
const tableList = ref<any[]>([])
const tableListLoading = ref(false)
const selectedTable = ref<any>(null)
const registerFormRef = ref<FormInstance>()
const submitLoading = ref(false)

const registerFormData = reactive<Partial<TableResource>>({
  tableName: '',
  tableComment: '',
  resourceCode: '',
  apiPath: '',
  methods: 'GET,POST,PUT,DELETE',
  status: 1,
})

const selectedMethods = ref<string[]>(['GET', 'POST', 'PUT', 'DELETE'])

const registerFormRules: FormRules = {
  resourceCode: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入API路径', trigger: 'blur' }],
}

// 编辑对话框
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editFormData = reactive<Partial<TableResource>>({})

const editFormRules: FormRules = {
  resourceCode: [{ required: true, message: '请输入资源编码', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入API路径', trigger: 'blur' }],
}

// 是否可以进入下一步
const canNextStep = computed(() => {
  if (currentStep.value === 0) {
    return !!selectedDatasourceId.value
  }
  if (currentStep.value === 1) {
    return !!selectedTable.value
  }
  return true
})

// 监听数据源变化，自动扫描表
watch(selectedDatasourceId, async (newVal, oldVal) => {
  // 只在注册对话框打开且在第0步时才自动扫描
  if (!registerDialogVisible.value || currentStep.value !== 0) {
    return
  }
  
  if (!newVal) {
    console.log('数据源ID清空')
    return
  }
  
  console.log('检测到数据源变化:', newVal)
  
  tableListLoading.value = true
  try {
    const res = await scanTables(newVal)
    console.log('扫描结果:', res)
    // 响应拦截器已经解包，res 就是数组
    tableList.value = res || []
    ElMessage.success(`扫描到 ${res?.length || 0} 个表`)
    
    // 自动进入下一步（选择表）
    if (tableList.value.length > 0) {
      setTimeout(() => {
        currentStep.value = 1
      }, 500)
    }
  } catch (error) {
    console.error('扫描表失败:', error)
    ElMessage.error('扫描表失败')
  } finally {
    tableListLoading.value = false
  }
})

// 加载数据源列表
const loadDatasources = async () => {
  try {
    const res = await getDataSourcePage(1, 100)
    // 响应拦截器已经解包，res 就是 PageResult
    datasourceList.value = res.records
  } catch (error) {
    ElMessage.error('加载数据源失败')
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getTableResourcePage(
      pagination.current,
      pagination.size,
      searchForm.datasourceId,
      searchForm.keyword
    )
    // 响应拦截器已经解包，res 就是 PageResult
    let records = res.records
    
    // 前端过滤状态
    if (searchForm.status !== undefined && searchForm.status !== null) {
      records = records.filter((item: TableResource) => item.status === searchForm.status)
    }
    
    tableData.value = records
    pagination.total = records.length
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  
  if (isScanning.value) {
    // 扫描模式下，前端过滤
    filterScannedTables()
  } else {
    // 正常模式，后端查询
    loadData()
  }
}

// 过滤扫描到的表（前端模糊搜索）
const filterScannedTables = () => {
  const keyword = searchForm.keyword?.toLowerCase() || ''
  
  if (!keyword) {
    // 没有关键词，显示所有表
    tableData.value = scannedTables.value
  } else {
    // 有关键词，过滤表名或表注释
    tableData.value = scannedTables.value.filter((table: any) => {
      const tableName = table.tableName?.toLowerCase() || ''
      const tableComment = table.tableComment?.toLowerCase() || ''
      return tableName.includes(keyword) || tableComment.includes(keyword)
    })
  }
  
  pagination.total = tableData.value.length
}

// 扫描表
const handleScanTables = async () => {
  if (!searchForm.datasourceId) {
    ElMessage.warning('请先选择数据源')
    return
  }
  
  loading.value = true
  isScanning.value = true
  try {
    const res = await scanTables(searchForm.datasourceId)
    // 响应拦截器已经解包，res 就是数组
    scannedTables.value = res || []
    
    // 应用过滤
    filterScannedTables()
    
    ElMessage.success(`扫描到 ${scannedTables.value.length} 个表`)
  } catch (error) {
    console.error('扫描表失败:', error)
    ElMessage.error('扫描表失败')
  } finally {
    loading.value = false
  }
}

// 从扫描结果注册表
const handleRegisterFromScan = (table: any) => {
  // 打开注册对话框，并预填信息
  registerDialogVisible.value = true
  currentStep.value = 2 // 直接跳到第三步
  selectedDatasourceId.value = searchForm.datasourceId
  
  // 生成资源编码：r_{数据源编码}_{表名}
  const datasource = datasourceList.value.find(ds => ds.id === searchForm.datasourceId)
  const dsCode = datasource?.code || 'db'
  const tableName = table.tableName.toLowerCase()
  const resourceCode = `r_${dsCode}_${tableName}`
  
  // 预填表单
  registerFormData.tableName = table.tableName
  registerFormData.tableComment = table.tableComment
  registerFormData.resourceCode = resourceCode
  registerFormData.apiPath = `/api/${dsCode}/${tableName}`
  registerFormData.datasourceId = searchForm.datasourceId
}

// 重置
const handleReset = () => {
  isScanning.value = false
  scannedTables.value = []
  searchForm.datasourceId = undefined
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}

// 注册表资源
const handleRegister = () => {
  registerDialogVisible.value = true
  currentStep.value = 0
}

// 数据源变化
const handleDatasourceChange = async () => {
  if (!selectedDatasourceId.value) {
    console.log('未选择数据源')
    return
  }
  
  console.log('开始扫描数据源 ID:', selectedDatasourceId.value)
  tableListLoading.value = true
  try {
    const res = await scanTables(selectedDatasourceId.value)
    console.log('扫描结果:', res)
    // 响应拦截器已经解包，res 就是数组
    tableList.value = res || []
    ElMessage.success(`扫描到 ${res?.length || 0} 个表`)
    
    // 自动进入下一步（选择表）
    if (tableList.value.length > 0) {
      setTimeout(() => {
        currentStep.value = 1
      }, 500)
    }
  } catch (error) {
    console.error('扫描表失败:', error)
    ElMessage.error('扫描表失败')
  } finally {
    tableListLoading.value = false
  }
}

// 选择表
const handleTableSelect = (row: any) => {
  selectedTable.value = row
}

// 下一步
const handleNextStep = () => {
  if (currentStep.value === 1 && selectedTable.value) {
    // 填充表单
    const datasource = datasourceList.value.find(ds => ds.id === selectedDatasourceId.value)
    const dsCode = datasource?.code || 'db'
    const tableName = selectedTable.value.tableName.toLowerCase()
    
    registerFormData.tableName = selectedTable.value.tableName
    registerFormData.tableComment = selectedTable.value.tableComment
    // 生成资源编码：r_{数据源编码}_{表名}
    registerFormData.resourceCode = `r_${dsCode}_${tableName}`
    registerFormData.apiPath = `/api/${dsCode}/${tableName}`
  }
  currentStep.value++
}

// 提交注册
const handleSubmitRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      const data: Partial<TableResource> = {
        ...registerFormData,
        datasourceId: selectedDatasourceId.value,
        methods: selectedMethods.value.join(','),
      }
      
      await registerTableResource(data)
      ElMessage.success('注册成功')
      registerDialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('注册失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 关闭注册对话框
const handleRegisterDialogClose = () => {
  currentStep.value = 0
  selectedDatasourceId.value = undefined
  selectedTable.value = null
  tableList.value = []
  registerFormRef.value?.resetFields()
  Object.assign(registerFormData, {
    tableName: '',
    tableComment: '',
    resourceCode: '',
    apiPath: '',
    methods: 'GET,POST,PUT,DELETE',
    status: 1,
  })
  selectedMethods.value = ['GET', 'POST', 'PUT', 'DELETE']
}

// 编辑
const handleEdit = (row: TableResource) => {
  editDialogVisible.value = true
  Object.assign(editFormData, row)
}

// 提交编辑
const handleSubmitEdit = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      await updateTableResource(editFormData)
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('更新失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 关闭编辑对话框
const handleEditDialogClose = () => {
  editFormRef.value?.resetFields()
  Object.assign(editFormData, {})
}

// 删除
const handleDelete = async (row: TableResource) => {
  try {
    await ElMessageBox.confirm('确定要删除该表资源吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    await deleteTableResource(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 生成API
const handleGenerateApi = async (row: TableResource) => {
  try {
    await generateApi(row.id!)
    ElMessage.success('API生成成功')
  } catch (error) {
    ElMessage.error('API生成失败')
  }
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadData()
}

// 页码变化
const handleCurrentChange = (current: number) => {
  pagination.current = current
  loadData()
}

onMounted(() => {
  loadDatasources()
  loadData()
})
</script>

<style scoped>
.table-resource-management {
  padding: 20px;
}

.search-card {
  margin-bottom: 20px;
}

.table-card {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.step-content {
  margin-top: 20px;
  min-height: 300px;
}

/* 目录视图样式 */
.catalog-view {
  margin-top: 20px;
}

.resource-card {
  margin-bottom: 20px;
  transition: all 0.3s;
}

.resource-card:hover {
  transform: translateY(-5px);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.table-name {
  font-weight: bold;
  font-size: 16px;
  color: #303133;
}

.card-body {
  min-height: 150px;
}

.info-item {
  margin-bottom: 12px;
  display: flex;
  align-items: flex-start;
}

.info-item .label {
  font-weight: 500;
  color: #909399;
  min-width: 80px;
  flex-shrink: 0;
}

.info-item .value {
  color: #606266;
  flex: 1;
  word-break: break-all;
}

.info-item .api-path {
  font-family: 'Courier New', monospace;
  font-size: 12px;
  color: #409eff;
}

.methods {
  display: flex;
  gap: 5px;
  flex-wrap: wrap;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}

.card-footer .el-button {
  flex: 1;
}

.form-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}
</style>
