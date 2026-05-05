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
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleRegister">注册表资源</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="datasourceName" label="数据源" min-width="150" />
        <el-table-column prop="tableName" label="表名" min-width="200" />
        <el-table-column prop="tableComment" label="表注释" min-width="250" show-overflow-tooltip />
        <el-table-column prop="resourceCode" label="资源编码" min-width="150" />
        <el-table-column prop="apiPath" label="API路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="methods" label="支持方法" width="150">
          <template #default="{ row }">
            <el-tag v-for="method in row.methods?.split(',')" :key="method" size="small" style="margin-right: 5px">
              {{ method }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleGenerateApi(row)">
              生成API
            </el-button>
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

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
      top="8vh"
      :close-on-click-modal="false"
      @close="handleRegisterDialogClose"
    >
      <div class="dialog-content">
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
        <el-input
          v-model="tableSearchKeyword"
          placeholder="搜索表名或表注释"
          clearable
          prefix-icon="Search"
          style="margin-bottom: 15px"
        />
        <el-table
          :data="filteredTableList"
          border
          stripe
          max-height="300"
          v-loading="tableListLoading"
          @row-click="handleTableSelect"
          highlight-current-row
        >
          <el-table-column type="index" width="50" />
          <el-table-column prop="tableName" label="表名" min-width="200" />
          <el-table-column prop="tableComment" label="表注释" min-width="200" />
          <el-table-column label="注册状态" width="100" align="center">
            <template #default="{ row }">
              <el-tag v-if="row.isRegistered" type="success" size="small">已注册</el-tag>
              <el-tag v-else type="info" size="small">未注册</el-tag>
            </template>
          </el-table-column>
        </el-table>
        <div v-if="filteredTableList.length === 0 && !tableListLoading" style="text-align: center; padding: 20px; color: #909399">
          暂无匹配的表
        </div>
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
          
          <!-- 字段配置 -->
          <el-divider>字段查询配置</el-divider>
          <el-form-item label="字段列表">
            <el-table
              :data="tableColumns"
              border
              stripe
              max-height="300"
              v-loading="columnsLoading"
              size="small"
            >
              <el-table-column prop="columnName" label="字段名" min-width="150" />
              <el-table-column prop="dataType" label="数据类型" width="120" />
              <el-table-column prop="columnComment" label="字段注释" min-width="150" show-overflow-tooltip />
              <el-table-column label="精确查询" width="100" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.exactQuery" />
                </template>
              </el-table-column>
              <el-table-column label="模糊查询" width="100" align="center">
                <template #default="{ row }">
                  <el-checkbox v-model="row.fuzzyQuery" @change="handleFuzzyQueryChange(row)" />
                </template>
              </el-table-column>
            </el-table>
            <div class="form-tip">
              勾选字段以支持对应的查询方式，默认主键支持精确查询
            </div>
          </el-form-item>
        </el-form>
      </div>
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

    <!-- 生成API对话框 -->
    <el-dialog
      v-model="generateApiDialogVisible"
      title="生成 API 接口"
      width="900px"
      top="8vh"
      :close-on-click-modal="false"
    >
      <div class="dialog-content">
        <el-alert
          title="请确认以下 API 配置信息"
          type="info"
          :closable="false"
          style="margin-bottom: 20px"
        />
        
        <!-- 基本信息 -->
        <el-descriptions title="基本信息" :column="2" border>
          <el-descriptions-item label="表名">{{ generateApiData.tableName }}</el-descriptions-item>
          <el-descriptions-item label="表注释">{{ generateApiData.tableComment || '-' }}</el-descriptions-item>
          <el-descriptions-item label="数据源">{{ generateApiData.datasourceName || '-' }}</el-descriptions-item>
          <el-descriptions-item label="资源编码">{{ generateApiData.resourceCode }}</el-descriptions-item>
          <el-descriptions-item label="API路径" :span="2">{{ generateApiData.apiPath }}</el-descriptions-item>
          <el-descriptions-item label="支持方法">
            <el-tag v-for="method in generateApiData.methods?.split(',')" :key="method" size="small" style="margin-right: 5px">
              {{ method }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="generateApiData.status === 1 ? 'success' : 'danger'">
              {{ generateApiData.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
        
        <!-- 字段配置 -->
        <el-divider>查询字段配置</el-divider>
        <div v-if="generateApiData.fieldConfig && generateApiData.fieldConfig.length > 0">
          <el-table :data="generateApiData.fieldConfig" border stripe size="small">
            <el-table-column prop="columnName" label="字段名" min-width="150" />
            <el-table-column prop="dataType" label="数据类型" width="120" />
            <el-table-column prop="columnComment" label="字段注释" min-width="150" show-overflow-tooltip />
            <el-table-column label="精确查询" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.exactQuery" type="success" size="small">是</el-tag>
                <el-tag v-else type="info" size="small">否</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="模糊查询" width="100" align="center">
              <template #default="{ row }">
                <el-tag v-if="row.fuzzyQuery" type="success" size="small">是</el-tag>
                <el-tag v-else type="info" size="small">否</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </div>
        <el-empty v-else description="未配置查询字段" :image-size="80" />
        
        <!-- API示例 -->
        <el-divider>API 调用示例</el-divider>
        <div class="api-example">
          <div class="example-item">
            <strong>GET 查询列表：</strong>
            <code>{{ generateApiData.apiPath }}</code>
          </div>
          <div class="example-item">
            <strong>GET 条件查询：</strong>
            <code>{{ generateApiData.apiPath }}?page=1&size=10</code>
          </div>
          <div v-if="generateApiData.fieldConfig && generateApiData.fieldConfig.some((f: any) => f.exactQuery)" class="example-item">
            <strong>精确查询示例：</strong>
            <code>{{ generateApiData.apiPath }}?{{ generateApiData.fieldConfig.find((f: any) => f.exactQuery)?.columnName }}=value</code>
          </div>
          <div v-if="generateApiData.fieldConfig && generateApiData.fieldConfig.some((f: any) => f.fuzzyQuery)" class="example-item">
            <strong>模糊查询示例：</strong>
            <code>{{ generateApiData.apiPath }}?{{ generateApiData.fieldConfig.find((f: any) => f.fuzzyQuery)?.columnName }}=keyword</code>
          </div>
        </div>
      </div>
      
      <template #footer>
        <el-button @click="generateApiDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmGenerateApi" :loading="submitLoading">
          确认生成
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
import { getDataSourcePage, scanTables, getTableColumns, type DataSourceConfig } from '@/api/datasource'

// 搜索表单
const searchForm = reactive({
  datasourceId: undefined as number | undefined,
  keyword: '',
  status: undefined as number | undefined,
})

// 表格数据
const tableData = ref<TableResource[]>([])
const loading = ref(false)

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
const tableSearchKeyword = ref('') // 表搜索关键词
const tableColumns = ref<any[]>([]) // 表字段列表
const columnsLoading = ref(false) // 字段加载状态

const registerFormData = reactive<Partial<TableResource>>({
  tableName: '',
  tableComment: '',
  resourceCode: '',
  apiPath: '',
  methods: 'GET',
  status: 1,
})

const selectedMethods = ref<string[]>(['GET'])

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

// 生成API对话框
const generateApiDialogVisible = ref(false)
const generateApiData = reactive<any>({
  id: null,
  tableName: '',
  tableComment: '',
  datasourceName: '',
  resourceCode: '',
  apiPath: '',
  methods: '',
  status: 1,
  fieldConfig: [],
})

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

// 过滤后的表列表（支持模糊搜索）
const filteredTableList = computed(() => {
  if (!tableSearchKeyword.value) {
    return tableList.value
  }
  
  const keyword = tableSearchKeyword.value.toLowerCase()
  return tableList.value.filter((table: any) => {
    const tableName = table.tableName?.toLowerCase() || ''
    const tableComment = table.tableComment?.toLowerCase() || ''
    return tableName.includes(keyword) || tableComment.includes(keyword)
  })
})

// 监听数据源变化，自动扫描表
watch(selectedDatasourceId, async (newVal, oldVal) => {
  // 只在注册对话框打开且在第0步时才自动扫描
  if (!registerDialogVisible.value || currentStep.value !== 0) {
    return
  }
  
  if (!newVal) {
    return
  }
  
  tableListLoading.value = true
  try {
    const res = await scanTables(newVal)
    // 响应拦截器已经解包，res 就是数组
    tableList.value = res || []
    
    // 自动进入下一步（选择表）
    if (tableList.value.length > 0) {
      setTimeout(() => {
        currentStep.value = 1
      }, 500)
    }
  } catch (error) {
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
    pagination.total = res.total
  } catch (error) {
    ElMessage.error('加载数据失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadData()
}

// 重置
const handleReset = () => {
  searchForm.datasourceId = undefined
  searchForm.keyword = ''
  searchForm.status = undefined
  handleSearch()
}

// 注册表资源
const handleRegister = async () => {
  // 确保列表数据是最新的
  await loadData()
  registerDialogVisible.value = true
  currentStep.value = 0
}

// 数据源变化
const handleDatasourceChange = async () => {
  if (!selectedDatasourceId.value) {
    return
  }
  
  tableListLoading.value = true
  try {
    const res = await scanTables(selectedDatasourceId.value)
    // 响应拦截器已经解包，res 就是数组
    const tables = res || []
    
    // 标记已注册的表（使用宽松比较，兼容字符串和数字类型）
    const registeredTableNames = new Set(
      tableData.value
        .filter(item => String(item.datasourceId) === String(selectedDatasourceId.value))
        .map(item => item.tableName)
    )
    
    console.log('已注册的表名:', Array.from(registeredTableNames))
    console.log('扫描到的表:', tables.map(t => t.tableName))
    
    tableList.value = tables.map(table => ({
      ...table,
      isRegistered: registeredTableNames.has(table.tableName),
    }))
    
    console.log('标记后的表列表:', tableList.value.map(t => ({ name: t.tableName, registered: t.isRegistered })))
    
    ElMessage.success(`扫描到 ${tables.length} 个表`)
    
    // 自动进入下一步（选择表）
    if (tableList.value.length > 0) {
      setTimeout(() => {
        currentStep.value = 1
      }, 500)
    }
  } catch (error) {
    ElMessage.error('扫描表失败')
  } finally {
    tableListLoading.value = false
  }
}

// 选择表
const handleTableSelect = (row: any) => {
  selectedTable.value = row
}

// 处理模糊查询变化（互斥）
const handleFuzzyQueryChange = (row: any) => {
  // 如果勾选了模糊查询，自动取消精确查询
  if (row.fuzzyQuery) {
    row.exactQuery = false
  }
}

// 加载表字段
const loadTableColumns = async () => {
  if (!selectedDatasourceId.value || !selectedTable.value) {
    return
  }
  
  columnsLoading.value = true
  try {
    const res = await getTableColumns(selectedDatasourceId.value, selectedTable.value.tableName)
    // 响应拦截器已经解包，res 就是数组
    tableColumns.value = (res || []).map((col: any) => ({
      ...col,
      exactQuery: col.columnKey === 'PRI', // 主键默认支持精确查询
      fuzzyQuery: false,
    }))
  } catch (error) {
    ElMessage.error('加载表字段失败')
  } finally {
    columnsLoading.value = false
  }
}

// 下一步
const handleNextStep = async () => {
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
    
    // 加载表字段
    await loadTableColumns()
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
      // 构建字段配置
      const fieldConfig = tableColumns.value
        .filter(col => col.exactQuery || col.fuzzyQuery)
        .map(col => ({
          columnName: col.columnName,
          dataType: col.dataType,
          columnComment: col.columnComment,
          exactQuery: col.exactQuery,
          fuzzyQuery: col.fuzzyQuery,
        }))
      
      const data: Partial<TableResource> = {
        ...registerFormData,
        datasourceId: selectedDatasourceId.value,
        methods: selectedMethods.value.join(','),
        configJson: JSON.stringify({ fields: fieldConfig }),
      }
      
      await registerTableResource(data)
      ElMessage.success('注册成功')
      registerDialogVisible.value = false
      loadData()
    } catch (error: any) {
      const errorMsg = error?.response?.data?.message || error?.message || '注册失败'
      
      // 如果是资源编码已存在的错误，提供特殊处理
      if (errorMsg.includes('资源编码已存在')) {
        try {
          await ElMessageBox.confirm(
            `${errorMsg}\n\n您可以：\n1. 修改资源编码后重新注册\n2. 查看并编辑已有的资源`,
            '资源编码冲突',
            {
              confirmButtonText: '去编辑',
              cancelButtonText: '修改编码',
              type: 'warning',
            }
          )
          
          // 用户选择“去编辑”，先刷新列表数据，再查找已有资源
          registerDialogVisible.value = false
          await loadData() // 先刷新列表
          
          const existingResource = tableData.value.find(
            item => item.resourceCode === registerFormData.resourceCode
          )
          
          if (existingResource) {
            handleEdit(existingResource)
          } else {
            // 如果还是找不到，尝试通过API路径搜索
            const searchByPath = tableData.value.find(
              item => item.apiPath === registerFormData.apiPath
            )
            if (searchByPath) {
              handleEdit(searchByPath)
            } else {
              ElMessage.warning('未找到该资源，请手动在列表中查找')
            }
          }
        } catch (confirmError) {
          // 用户选择“修改编码”，保持对话框打开，让用户修改
          if (confirmError !== 'cancel') {
            console.error('操作失败:', confirmError)
          }
        }
      } else {
        // 其他错误，显示错误信息
        ElMessage.error(errorMsg)
      }
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
  tableSearchKeyword.value = '' // 清空搜索关键词
  tableColumns.value = [] // 清空字段列表
  registerFormRef.value?.resetFields()
  Object.assign(registerFormData, {
    tableName: '',
    tableComment: '',
    resourceCode: '',
    apiPath: '',
    methods: 'GET',
    status: 1,
  })
  selectedMethods.value = ['GET']
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
    await ElMessageBox.confirm(
      `确定要删除表资源「${row.tableName}」吗？\n此操作不可恢复！`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    )
    
    await deleteTableResource(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (error: any) {
    if (error !== 'cancel') {
      console.error('删除失败:', error)
      // 错误提示由响应拦截器统一处理，这里不需要额外显示
    }
  }
}

// 生成API
const handleGenerateApi = async (row: TableResource) => {
  // 填充生成API对话框数据
  generateApiData.id = row.id
  generateApiData.tableName = row.tableName || ''
  generateApiData.tableComment = row.tableComment || ''
  generateApiData.datasourceName = row.datasourceName || ''
  generateApiData.resourceCode = row.resourceCode || ''
  generateApiData.apiPath = row.apiPath || ''
  generateApiData.methods = row.methods || 'GET'
  generateApiData.status = row.status || 1
  
  // 解析字段配置
  if (row.configJson) {
    try {
      const config = JSON.parse(row.configJson)
      generateApiData.fieldConfig = config.fields || []
    } catch (e) {
      generateApiData.fieldConfig = []
    }
  } else {
    generateApiData.fieldConfig = []
  }
  
  // 显示对话框
  generateApiDialogVisible.value = true
}

// 确认生成API
const confirmGenerateApi = async () => {
  try {
    submitLoading.value = true
    await generateApi(generateApiData.id!)
    ElMessage.success('API 生成成功')
    generateApiDialogVisible.value = false
    loadData()
  } catch (error: any) {
    console.error('生成 API 失败:', error)
    const errorMsg = error?.response?.data?.message || error?.message || 'API 生成失败'
    
    // 如果是API路径冲突的错误，提供特殊处理
    if (errorMsg.includes('duplicate key') && errorMsg.includes('uk_api_path_method')) {
      try {
        await ElMessageBox.confirm(
          `API 已存在！\n\n` +
          `API路径：${generateApiData.apiPath}\n` +
          `HTTP方法：${generateApiData.methods}\n\n` +
          `该 API 已经生成过，您可以：\n` +
          `1. 前往 API 管理页面查看和编辑\n` +
          `2. 取消操作`,
          'API 已存在',
          {
            confirmButtonText: '去 API 管理',
            cancelButtonText: '取消',
            type: 'warning',
          }
        )
        
        // 用户选择“去 API 管理”，跳转到 API 管理页面
        generateApiDialogVisible.value = false
        // 这里可以通过路由跳转到 API 管理页面
        window.location.href = '/#/resource/api'
      } catch (confirmError) {
        // 用户选择“取消”
        if (confirmError !== 'cancel') {
          console.error('操作失败:', confirmError)
        }
      }
    } else {
      // 其他错误，显示错误信息
      ElMessage.error(errorMsg)
    }
  } finally {
    submitLoading.value = false
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

/* 对话框内容区域 */
.dialog-content {
  max-height: calc(80vh - 120px);
  overflow-y: auto;
  padding-right: 10px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

.step-content {
  margin-top: 20px;
  min-height: 200px;
}

.form-tip {
  margin-top: 5px;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
}

.api-example {
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 4px;
}

.example-item {
  margin-bottom: 10px;
  line-height: 1.8;
}

.example-item:last-child {
  margin-bottom: 0;
}

.example-item code {
  display: inline-block;
  margin-left: 10px;
  padding: 2px 8px;
  background-color: #fff;
  border: 1px solid #dcdfe6;
  border-radius: 3px;
  font-family: 'Courier New', monospace;
  font-size: 13px;
  color: #409eff;
}
</style>
