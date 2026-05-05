<template>
  <div class="api-management">
    <el-card>
      <!-- 搜索栏 -->
      <el-form :inline="true" :model="searchForm" class="search-form">
        <el-form-item label="API名称">
          <el-input
            v-model="searchForm.apiName"
            placeholder="请输入API名称"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="API类型">
          <el-select v-model="searchForm.apiType" placeholder="请选择" clearable style="width: 150px">
            <el-option label="表资源" value="TABLE_RESOURCE" />
            <el-option label="外部接口" value="EXTERNAL" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="请选择" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleRegisterExternal">注册外部接口</el-button>
        </el-form-item>
      </el-form>

      <!-- API列表 -->
      <el-table :data="apiList" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="180" show-overflow-tooltip />
        <el-table-column prop="apiName" label="API名称" min-width="150" show-overflow-tooltip />
        <el-table-column prop="apiPath" label="API路径" min-width="200" show-overflow-tooltip />
        <el-table-column prop="apiMethod" label="方法" width="100">
          <template #default="{ row }">
            <el-tag :type="getMethodTagType(row.apiMethod)" size="small">
              {{ row.apiMethod }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="apiType" label="类型" width="120">
          <template #default="{ row }">
            <el-tag :type="row.apiType === 'TABLE_RESOURCE' ? 'success' : 'warning'" size="small">
              {{ row.apiType === 'TABLE_RESOURCE' ? '表资源' : '外部接口' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="version" label="版本" width="80" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
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

    <!-- 注册外部接口对话框 -->
    <el-dialog
      v-model="registerDialogVisible"
      title="注册外部接口"
      width="1000px"
      top="50px"
      :close-on-click-modal="false"
      @close="handleRegisterDialogClose"
    >
      <div class="dialog-content">
        <el-form
          ref="registerFormRef"
          :model="registerFormData"
          :rules="registerFormRules"
          label-width="120px"
        >
        <el-form-item label="API名称" prop="apiName">
          <el-input v-model="registerFormData.apiName" placeholder="请输入API名称" />
        </el-form-item>
        <el-form-item label="API路径" prop="apiPath">
          <el-input v-model="registerFormData.apiPath" placeholder="例如：/api/external/user" />
        </el-form-item>
        <el-form-item label="API版本">
          <el-input v-model="registerFormData.version" placeholder="例如：v1" />
        </el-form-item>
        <el-form-item label="API描述">
          <el-input
            v-model="registerFormData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入API描述"
          />
        </el-form-item>
        <el-form-item label="HTTP方法" prop="apiMethod">
          <el-radio-group v-model="registerFormData.apiMethod">
            <el-radio value="GET">GET</el-radio>
            <el-radio value="POST">POST</el-radio>
            <el-radio value="PUT">PUT</el-radio>
            <el-radio value="DELETE">DELETE</el-radio>
            <el-radio value="PATCH">PATCH</el-radio>
          </el-radio-group>
        </el-form-item>
        
        <!-- 请求参数配置 -->
        <el-divider content-position="left">请求参数配置</el-divider>
        
        <el-form-item label="参数配置">
          <el-tabs v-model="activeParamTab" type="border-card">
            <!-- Query Params -->
            <el-tab-pane label="Query Params" name="params">
              <div class="param-config">
                <el-button size="small" @click="addParam('params')" style="margin-bottom: 10px">
                  <el-icon><Plus /></el-icon> 添加参数
                </el-button>
                <el-table :data="requestParams.params" border size="small">
                  <el-table-column label="参数名" width="180">
                    <template #default="{ row }">
                      <el-input v-model="row.key" placeholder="参数名" size="small" />
                    </template>
                  </el-table-column>
                  <el-table-column label="参数值" min-width="250">
                    <template #default="{ row }">
                      <el-input v-model="row.value" placeholder="参数值" size="small" />
                    </template>
                  </el-table-column>
                  <el-table-column label="描述" min-width="150">
                    <template #default="{ row }">
                      <el-input v-model="row.description" placeholder="描述" size="small" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="80" align="center" fixed="right">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removeParam('params', $index)">
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
            
            <!-- Headers -->
            <el-tab-pane label="Headers" name="headers">
              <div class="param-config">
                <el-button size="small" @click="addParam('headers')" style="margin-bottom: 10px">
                  <el-icon><Plus /></el-icon> 添加Header
                </el-button>
                <el-table :data="requestParams.headers" border size="small">
                  <el-table-column label="Header名" width="180">
                    <template #default="{ row }">
                      <el-autocomplete
                        v-model="row.key"
                        :fetch-suggestions="querySearchHeader"
                        placeholder="例如：Content-Type"
                        size="small"
                        clearable
                        style="width: 100%"
                      >
                        <template #default="{ item }">
                          <div class="header-suggestion">
                            <span class="header-name">{{ item.value }}</span>
                            <span class="header-desc">{{ item.description }}</span>
                          </div>
                        </template>
                      </el-autocomplete>
                    </template>
                  </el-table-column>
                  <el-table-column label="Header值" min-width="250">
                    <template #default="{ row }">
                      <el-autocomplete
                        v-model="row.value"
                        :fetch-suggestions="(queryString, cb) => querySearchHeaderValue(row, queryString, cb)"
                        placeholder="例如：application/json"
                        size="small"
                        clearable
                        style="width: 100%"
                      >
                        <template #default="{ item }">
                          <div class="header-suggestion">
                            <span class="header-name">{{ item.value }}</span>
                            <span class="header-desc">{{ item.description }}</span>
                          </div>
                        </template>
                      </el-autocomplete>
                    </template>
                  </el-table-column>
                  <el-table-column label="描述" min-width="150">
                    <template #default="{ row }">
                      <el-input v-model="row.description" placeholder="描述" size="small" />
                    </template>
                  </el-table-column>
                  <el-table-column label="操作" width="80" align="center" fixed="right">
                    <template #default="{ $index }">
                      <el-button link type="danger" size="small" @click="removeParam('headers', $index)">
                        删除
                      </el-button>
                    </template>
                  </el-table-column>
                </el-table>
              </div>
            </el-tab-pane>
            
            <!-- Body -->
            <el-tab-pane label="Body" name="body">
              <div class="body-config">
                <el-radio-group v-model="bodyType" size="small" style="margin-bottom: 10px">
                  <el-radio value="none">none</el-radio>
                  <el-radio value="form-data">form-data</el-radio>
                  <el-radio value="x-www-form-urlencoded">x-www-form-urlencoded</el-radio>
                  <el-radio value="raw">raw (JSON)</el-radio>
                </el-radio-group>
                
                <!-- form-data -->
                <div v-if="bodyType === 'form-data'" class="param-config">
                  <el-button size="small" @click="addParam('body')" style="margin-bottom: 10px">
                    <el-icon><Plus /></el-icon> 添加字段
                  </el-button>
                  <el-table :data="requestParams.body" border size="small">
                    <el-table-column label="字段名" width="180">
                      <template #default="{ row }">
                        <el-input v-model="row.key" placeholder="字段名" size="small" />
                      </template>
                    </el-table-column>
                    <el-table-column label="字段值" min-width="200">
                      <template #default="{ row }">
                        <el-input v-model="row.value" placeholder="字段值" size="small" />
                      </template>
                    </el-table-column>
                    <el-table-column label="类型" width="100">
                      <template #default="{ row }">
                        <el-select v-model="row.type" size="small">
                          <el-option label="Text" value="text" />
                          <el-option label="File" value="file" />
                        </el-select>
                      </template>
                    </el-table-column>
                    <el-table-column label="描述" min-width="150">
                      <template #default="{ row }">
                        <el-input v-model="row.description" placeholder="描述" size="small" />
                      </template>
                    </el-table-column>
                    <el-table-column label="操作" width="80" align="center" fixed="right">
                      <template #default="{ $index }">
                        <el-button link type="danger" size="small" @click="removeParam('body', $index)">
                          删除
                        </el-button>
                      </template>
                    </el-table-column>
                  </el-table>
                </div>
                
                <!-- raw JSON -->
                <div v-else-if="bodyType === 'raw'">
                  <el-input
                    v-model="rawBodyJson"
                    type="textarea"
                    :rows="10"
                    placeholder='{"key": "value"}'
                    style="font-family: monospace"
                  />
                </div>
              </div>
            </el-tab-pane>
          </el-tabs>
        </el-form-item>
        <el-form-item label="是否需要认证">
          <el-radio-group v-model="registerFormData.authRequired">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="限流次数/分钟">
          <el-input-number v-model="registerFormData.rateLimit" :min="0" placeholder="0表示不限流" />
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
        <el-button type="primary" @click="handleSubmitRegister" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      title="编辑API"
      width="700px"
      @close="handleEditDialogClose"
    >
      <el-form
        ref="editFormRef"
        :model="editFormData"
        :rules="editFormRules"
        label-width="120px"
      >
        <el-form-item label="API名称" prop="apiName">
          <el-input v-model="editFormData.apiName" placeholder="请输入API名称" />
        </el-form-item>
        <el-form-item label="API路径" prop="apiPath">
          <el-input v-model="editFormData.apiPath" placeholder="例如：/api/external/user" />
        </el-form-item>
        <el-form-item label="HTTP方法" prop="apiMethod">
          <el-radio-group v-model="editFormData.apiMethod">
            <el-radio value="GET">GET</el-radio>
            <el-radio value="POST">POST</el-radio>
            <el-radio value="PUT">PUT</el-radio>
            <el-radio value="DELETE">DELETE</el-radio>
            <el-radio value="PATCH">PATCH</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="API描述">
          <el-input
            v-model="editFormData.description"
            type="textarea"
            :rows="3"
            placeholder="请输入API描述"
          />
        </el-form-item>
        <el-form-item label="API版本">
          <el-input v-model="editFormData.version" placeholder="例如：v1" />
        </el-form-item>
        <el-form-item label="是否需要认证">
          <el-radio-group v-model="editFormData.authRequired">
            <el-radio :value="0">否</el-radio>
            <el-radio :value="1">是</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="限流次数/分钟">
          <el-input-number v-model="editFormData.rateLimit" :min="0" placeholder="0表示不限流" />
        </el-form-item>
        <el-form-item label="排序号">
          <el-input-number v-model="editFormData.sortOrder" :min="0" />
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
import { ref, reactive, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getApiPage,
  registerExternalApi,
  updateApi,
  deleteApi,
  updateApiStatus,
  type ApiManagement,
} from '@/api/apiManagement'

// 搜索表单
const searchForm = reactive({
  apiName: '',
  apiType: '',
  status: undefined as number | undefined,
})

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

// API列表
const apiList = ref<ApiManagement[]>([])
const loading = ref(false)

// 注册对话框
const registerDialogVisible = ref(false)
const registerFormRef = ref<FormInstance>()
const registerFormData = reactive<Partial<ApiManagement>>({
  apiName: '',
  apiPath: '',
  apiMethod: 'GET',
  apiType: 'EXTERNAL',
  description: '',
  version: 'v1',
  authRequired: 0,
  rateLimit: 0,
  status: 1,
  sortOrder: 0,
})
const submitLoading = ref(false)

const registerFormRules: FormRules = {
  apiName: [{ required: true, message: '请输入API名称', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入API路径', trigger: 'blur' }],
  apiMethod: [{ required: true, message: '请选择HTTP方法', trigger: 'change' }],
}

// 请求参数配置
const activeParamTab = ref('params')
const bodyType = ref('none')
const rawBodyJson = ref('')

interface ParamItem {
  key: string
  value: string
  type?: string
  description: string
}

const requestParams = reactive({
  params: [] as ParamItem[],
  headers: [] as ParamItem[],
  body: [] as ParamItem[],
})

// 添加参数
const addParam = (type: 'params' | 'headers' | 'body') => {
  requestParams[type].push({
    key: '',
    value: '',
    type: 'text',
    description: '',
  })
}

// 删除参数
const removeParam = (type: 'params' | 'headers' | 'body', index: number) => {
  requestParams[type].splice(index, 1)
}

// 常用的HTTP Headers列表
const commonHeaders = [
  { value: 'Content-Type', description: '媒体类型，如 application/json' },
  { value: 'Authorization', description: '认证令牌，如 Bearer token' },
  { value: 'Accept', description: '可接受的响应类型' },
  { value: 'User-Agent', description: '客户端标识' },
  { value: 'Cache-Control', description: '缓存控制' },
  { value: 'Cookie', description: 'Cookie数据' },
  { value: 'Set-Cookie', description: '设置Cookie' },
  { value: 'X-Requested-With', description: '请求类型，如 XMLHttpRequest' },
  { value: 'Referer', description: '来源页面URL' },
  { value: 'Origin', description: '请求来源' },
  { value: 'Host', description: '目标主机' },
  { value: 'Connection', description: '连接类型，如 keep-alive' },
  { value: 'Content-Length', description: '请求体长度' },
  { value: 'Content-Encoding', description: '内容编码，如 gzip' },
  { value: 'Transfer-Encoding', description: '传输编码' },
  { value: 'Date', description: '消息发送日期' },
  { value: 'Expires', description: '过期时间' },
  { value: 'Last-Modified', description: '最后修改时间' },
  { value: 'ETag', description: '资源版本标识' },
  { value: 'If-Modified-Since', description: '条件请求-最后修改时间' },
  { value: 'If-None-Match', description: '条件请求-ETag' },
  { value: 'Access-Control-Allow-Origin', description: 'CORS-允许的源' },
  { value: 'Access-Control-Allow-Methods', description: 'CORS-允许的方法' },
  { value: 'Access-Control-Allow-Headers', description: 'CORS-允许的Header' },
  { value: 'X-Forwarded-For', description: '客户端IP' },
  { value: 'X-Real-IP', description: '真实IP' },
]

// Header搜索建议
interface HeaderSuggestion {
  value: string
  description: string
}

const querySearchHeader = (queryString: string, cb: (results: HeaderSuggestion[]) => void) => {
  const results = queryString
    ? commonHeaders.filter(header => 
        header.value.toLowerCase().includes(queryString.toLowerCase()) ||
        header.description.toLowerCase().includes(queryString.toLowerCase())
      )
    : commonHeaders
  cb(results)
}

// 常见Header值映射
const commonHeaderValues: Record<string, Array<{ value: string; description: string }>> = {
  'Content-Type': [
    { value: 'application/json', description: 'JSON格式' },
    { value: 'application/xml', description: 'XML格式' },
    { value: 'text/html', description: 'HTML文本' },
    { value: 'text/plain', description: '纯文本' },
    { value: 'multipart/form-data', description: '表单数据（文件上传）' },
    { value: 'application/x-www-form-urlencoded', description: '表单编码数据' },
  ],
  'Accept': [
    { value: 'application/json', description: '接受JSON' },
    { value: 'text/html', description: '接受HTML' },
    { value: 'text/plain', description: '接受纯文本' },
    { value: '*/*', description: '接受所有类型' },
  ],
  'Authorization': [
    { value: 'Bearer ', description: 'Bearer Token认证' },
    { value: 'Basic ', description: 'Basic认证' },
    { value: 'ApiKey ', description: 'API Key认证' },
  ],
  'Cache-Control': [
    { value: 'no-cache', description: '不使用缓存' },
    { value: 'no-store', description: '不存储缓存' },
    { value: 'max-age=3600', description: '缓存1小时' },
    { value: 'public', description: '公共缓存' },
    { value: 'private', description: '私有缓存' },
  ],
  'X-Requested-With': [
    { value: 'XMLHttpRequest', description: 'AJAX请求' },
  ],
}

// Header值搜索建议
const querySearchHeaderValue = (row: any, queryString: string, cb: (results: HeaderSuggestion[]) => void) => {
  // 获取当前行的header key
  const headerKey = row.key
  
  // 查找匹配的Header值建议
  let suggestions: Array<{ value: string; description: string }> = []
  
  if (headerKey && commonHeaderValues[headerKey]) {
    suggestions = commonHeaderValues[headerKey]
  } else {
    // 如果没有预定义的，尝试模糊匹配
    const matchedKey = Object.keys(commonHeaderValues).find(key => 
      key.toLowerCase() === headerKey?.toLowerCase()
    )
    if (matchedKey) {
      suggestions = commonHeaderValues[matchedKey]
    }
  }
  
  // 过滤建议
  const results = queryString
    ? suggestions.filter(item => 
        item.value.toLowerCase().includes(queryString.toLowerCase()) ||
        item.description.toLowerCase().includes(queryString.toLowerCase())
      )
    : suggestions
  
  cb(results)
}

// 编辑对话框
const editDialogVisible = ref(false)
const editFormRef = ref<FormInstance>()
const editFormData = reactive<Partial<ApiManagement>>({})

const editFormRules: FormRules = {
  apiName: [{ required: true, message: '请输入API名称', trigger: 'blur' }],
  apiPath: [{ required: true, message: '请输入API路径', trigger: 'blur' }],
  apiMethod: [{ required: true, message: '请选择HTTP方法', trigger: 'change' }],
}

// 加载API列表
const loadApiList = async () => {
  loading.value = true
  try {
    const res = await getApiPage({
      current: pagination.current,
      size: pagination.size,
      apiName: searchForm.apiName,
      apiType: searchForm.apiType,
      status: searchForm.status,
    })
    apiList.value = res.records || []
    pagination.total = res.total || 0
  } catch (error) {
    console.error('加载API列表失败:', error)
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.current = 1
  loadApiList()
}

// 重置
const handleReset = () => {
  searchForm.apiName = ''
  searchForm.apiType = ''
  searchForm.status = undefined
  pagination.current = 1
  loadApiList()
}

// 分页大小变化
const handleSizeChange = (size: number) => {
  pagination.size = size
  loadApiList()
}

// 页码变化
const handleCurrentChange = (current: number) => {
  pagination.current = current
  loadApiList()
}

// 获取HTTP方法的标签类型
const getMethodTagType = (method: string) => {
  const types: Record<string, string> = {
    GET: '',
    POST: 'success',
    PUT: 'warning',
    DELETE: 'danger',
    PATCH: 'info',
  }
  return types[method] || ''
}

// 状态变化
const handleStatusChange = async (row: ApiManagement) => {
  try {
    await updateApiStatus(row.id!, row.status!)
    ElMessage.success('状态更新成功')
  } catch (error) {
    // 恢复原状态
    row.status = row.status === 1 ? 0 : 1
    console.error('状态更新失败:', error)
  }
}

// 注册外部接口
const handleRegisterExternal = () => {
  registerDialogVisible.value = true
}

// 关闭注册对话框
const handleRegisterDialogClose = () => {
  registerFormRef.value?.resetFields()
  Object.assign(registerFormData, {
    apiName: '',
    apiPath: '',
    apiMethod: 'GET',
    apiType: 'EXTERNAL',
    description: '',
    version: 'v1',
    authRequired: 0,
    rateLimit: 0,
    status: 1,
    sortOrder: 0,
  })
  // 重置参数配置
  activeParamTab.value = 'params'
  bodyType.value = 'none'
  rawBodyJson.value = ''
  requestParams.params = []
  requestParams.headers = []
  requestParams.body = []
}

// 提交注册
const handleSubmitRegister = async () => {
  if (!registerFormRef.value) return
  
  await registerFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      // 构建请求配置
      const requestConfig: any = {
        params: requestParams.params.filter(p => p.key),
        headers: requestParams.headers.filter(p => p.key),
        bodyType: bodyType.value,
      }
      
      if (bodyType.value === 'form-data' || bodyType.value === 'x-www-form-urlencoded') {
        requestConfig.body = requestParams.body.filter(p => p.key)
      } else if (bodyType.value === 'raw') {
        requestConfig.rawBody = rawBodyJson.value
      }
      
      // 将配置转换为JSON字符串
      registerFormData.requestConfig = JSON.stringify(requestConfig, null, 2)
      
      await registerExternalApi(registerFormData as ApiManagement)
      ElMessage.success('注册成功')
      registerDialogVisible.value = false
      loadApiList()
    } catch (error) {
      console.error('注册失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

// 编辑
const handleEdit = (row: ApiManagement) => {
  Object.assign(editFormData, row)
  editDialogVisible.value = true
}

// 关闭编辑对话框
const handleEditDialogClose = () => {
  editFormRef.value?.resetFields()
  Object.assign(editFormData, {})
}

// 提交编辑
const handleSubmitEdit = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      await updateApi(editFormData.id!, editFormData as ApiManagement)
      ElMessage.success('更新成功')
      editDialogVisible.value = false
      loadApiList()
    } catch (error) {
      console.error('更新失败:', error)
    } finally {
      submitLoading.value = false
    }
  })
}

// 删除
const handleDelete = (row: ApiManagement) => {
  ElMessageBox.confirm('确定要删除该API吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning',
  }).then(async () => {
    try {
      await deleteApi(row.id!)
      ElMessage.success('删除成功')
      loadApiList()
    } catch (error) {
      console.error('删除失败:', error)
    }
  })
}

onMounted(() => {
  loadApiList()
})
</script>

<style scoped>
.api-management {
  padding: 20px;
}

.search-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  justify-content: flex-end;
}

/* Header搜索建议样式 */
.header-suggestion {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-name {
  font-weight: 500;
  color: #303133;
}

.header-desc {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.param-config {
  min-height: 150px;
}

.body-config {
  min-height: 200px;
}
</style>
