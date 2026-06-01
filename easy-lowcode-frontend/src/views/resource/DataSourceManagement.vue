<template>
  <div class="datasource-management">
    <!-- 搜索和操作栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm">
        <el-form-item label="关键词">
          <el-input
            v-model="searchForm.keyword"
            placeholder="请输入数据源名称或编码"
            clearable
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="success" @click="handleAdd">新增数据源</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="数据源名称" min-width="150" />
        <el-table-column prop="code" label="数据源编码" min-width="150" />
        <el-table-column prop="dbType" label="数据库类型" width="120">
          <template #default="{ row }">
            <el-tag :type="getDbTypeTag(row.dbType)">
              {{ getDbTypeLabel(row.dbType) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="url" label="连接URL" min-width="300" show-overflow-tooltip />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="remark" label="备注" min-width="200" show-overflow-tooltip />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleTestConnection(row)">
              测试连接
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="700px"
      top="35px"
      @close="handleDialogClose"
    >
      <div class="dialog-content">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="120px"
        >
        <el-form-item label="数据源名称" prop="name">
          <el-input v-model="formData.name" placeholder="请输入数据源名称" />
        </el-form-item>
        <el-form-item label="数据库类型" prop="dbType">
          <el-select v-model="formData.dbType" placeholder="请选择数据库类型" style="width: 100%" @change="handleDbTypeChange">
            <el-option label="MySQL" value="mysql" />
            <el-option label="PostgreSQL" value="postgresql" />
            <el-option label="Oracle" value="oracle" />
            <el-option label="SQL Server" value="sqlserver" />
            <el-option label="达梦 DM" value="dm" />
            <el-option label="人大金仓" value="kingbase" />
            <el-option label="南大通用 GBase" value="gbase" />
            <el-option label="OceanBase" value="oceanbase" />
            <el-option label="TiDB" value="tidb" />
            <el-option label="openGauss" value="opengauss" />
            <el-option label="华为 GaussDB" value="gaussdb" />
            <el-option label="瀚高 HighGo" value="highgo" />
          </el-select>
        </el-form-item>
        <el-form-item label="数据库名称" prop="dbName">
          <el-input v-model="formData.dbName" placeholder="请输入数据库名称" @blur="updateUrlByDbName" />
        </el-form-item>
        <el-form-item label="数据源编码" prop="code">
          <el-input v-model="formData.code" placeholder="请输入数据源编码（唯一）" />
        </el-form-item>
        <el-form-item label="连接URL" prop="url">
          <el-input
            v-model="formData.url"
            placeholder="例如：jdbc:mysql://localhost:3306/database"
            type="textarea"
            :rows="2"
          />
        </el-form-item>
        <el-form-item label="驱动类名">
          <el-input
            v-model="formData.driverClassName"
            placeholder="自动识别，也可手动输入"
          />
        </el-form-item>
        <el-form-item label="用户名" prop="username">
          <el-input v-model="formData.username" placeholder="请输入数据库用户名" />
        </el-form-item>
        <el-form-item label="密码" :prop="formData.id ? '' : 'password'" :rules="getPasswordRules()">
          <el-input
            v-model="formData.password"
            :type="showPassword ? 'text' : 'password'"
            :placeholder="formData.id ? '不修改密码请留空，如需修改请输入新密码' : '请输入数据库密码'"
            show-password
          />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="formData.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="备注">
          <el-input
            v-model="formData.remark"
            type="textarea"
            :rows="3"
            placeholder="请输入备注信息"
          />
        </el-form-item>
      </el-form>
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="warning" @click="handleTestConnectionInDialog" :loading="testLoading">
            测试连接
          </el-button>
          <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getDataSourcePage,
  createDataSource,
  updateDataSource,
  deleteDataSource,
  testConnection,
  type DataSourceConfig,
} from '@/api/datasource'

// 搜索表单
const searchForm = reactive({
  keyword: '',
})

// 表格数据
const tableData = ref<DataSourceConfig[]>([])
const loading = ref(false)

// 分页
const pagination = reactive({
  current: 1,
  size: 10,
  total: 0,
})

// 对话框
const dialogVisible = ref(false)
const dialogTitle = ref('')
const formRef = ref<FormInstance>()
const submitLoading = ref(false)
const testLoading = ref(false)
const showPassword = ref(false)
const formData = reactive<Partial<DataSourceConfig>>({
  name: '',
  code: '',
  dbType: 'mysql',
  dbName: 'easy_lowcode',  // 数据库名称（默认值）
  url: 'jdbc:mysql://localhost:3306/easy_lowcode?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai',
  username: '',
  password: '',
  driverClassName: 'com.mysql.cj.jdbc.Driver',  // 默认MySQL驱动
  status: 1,
  remark: '',
})

// 表单验证规则
const formRules: FormRules = {
  name: [{ required: true, message: '请输入数据源名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入数据源编码', trigger: 'blur' }],
  dbType: [{ required: true, message: '请选择数据库类型', trigger: 'change' }],
  dbName: [{ required: true, message: '请输入数据库名称', trigger: 'blur' }],
  url: [{ required: true, message: '请输入连接URL', trigger: 'blur' }],
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  driverClassName: [{ required: true, message: '请输入驱动类名', trigger: 'blur' }],
}

// 动态密码验证规则
const getPasswordRules = () => {
  // 新增时必须填密码，编辑时可选
  if (!formData.id) {
    return [{ required: true, message: '请输入密码', trigger: 'blur' }]
  }
  return []
}

// 数据库类型配置
const dbTypeConfig: Record<string, { driverClassName: string; urlTemplate: string }> = {
  mysql: {
    driverClassName: 'com.mysql.cj.jdbc.Driver',
    urlTemplate: 'jdbc:mysql://localhost:3306/database?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai'
  },
  postgresql: {
    driverClassName: 'org.postgresql.Driver',
    urlTemplate: 'jdbc:postgresql://localhost:5432/database'
  },
  oracle: {
    driverClassName: 'oracle.jdbc.OracleDriver',
    urlTemplate: 'jdbc:oracle:thin:@localhost:1521:orcl'
  },
  sqlserver: {
    driverClassName: 'com.microsoft.sqlserver.jdbc.SQLServerDriver',
    urlTemplate: 'jdbc:sqlserver://localhost:1433;databaseName=database;encrypt=false'
  },
  dm: {
    driverClassName: 'dm.jdbc.driver.DmDriver',
    urlTemplate: 'jdbc:dm://localhost:5236/database'
  },
  kingbase: {
    driverClassName: 'com.kingbase8.Driver',
    urlTemplate: 'jdbc:kingbase8://localhost:54321/database'
  },
  gbase: {
    driverClassName: 'com.gbase.jdbc.Driver',
    urlTemplate: 'jdbc:gbase://localhost:5258/database'
  },
  oceanbase: {
    driverClassName: 'com.oceanbase.jdbc.Driver',
    urlTemplate: 'jdbc:oceanbase://localhost:2881/database'
  },
  tidb: {
    driverClassName: 'com.mysql.cj.jdbc.Driver',
    urlTemplate: 'jdbc:mysql://localhost:4000/database?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai'
  },
  opengauss: {
    driverClassName: 'org.opengauss.Driver',
    urlTemplate: 'jdbc:opengauss://localhost:5432/database'
  },
  gaussdb: {
    driverClassName: 'com.huawei.gaussdb.jdbc.Driver',
    urlTemplate: 'jdbc:gaussdb://localhost:5432/database'
  },
  highgo: {
    driverClassName: 'com.highgo.jdbc.Driver',
    urlTemplate: 'jdbc:highgo://localhost:5866/database'
  }
}

// 处理数据库类型变化
const handleDbTypeChange = (dbType: string) => {
  const config = dbTypeConfig[dbType]
  if (config) {
    formData.driverClassName = config.driverClassName
    formData.url = config.urlTemplate
  }
  // 新增时自动更新编码
  if (!formData.id) {
    formData.code = generateCode(dbType)
  }
  // 有数据库名称时更新URL
  if (formData.dbName) {
    updateUrlByDbName()
  }
}

// 根据数据库名称更新连接URL
const updateUrlByDbName = () => {
  if (!formData.dbName || !formData.dbType) return
  const config = dbTypeConfig[formData.dbType]
  if (config) {
    formData.url = config.urlTemplate.replace('database', formData.dbName)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await getDataSourcePage(pagination.current, pagination.size, searchForm.keyword)
    // 响应拦截器已经解包，res 就是 PageResult
    tableData.value = res.records
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
  searchForm.keyword = ''
  handleSearch()
}


// 生成随机编码
const generateCode = (dbType: string) => {
  const timestamp = Date.now().toString(36).substring(0, 6)
  const random = Math.random().toString(36).substring(2, 6)
  return `${dbType}_${timestamp}${random}`
}

// 新增
const handleAdd = () => {
  dialogTitle.value = '新增数据源'
  showPassword.value = false
  formData.code = generateCode(formData.dbType!)  // 自动生成编码
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row: DataSourceConfig) => {
  dialogTitle.value = '编辑数据源'
  Object.assign(formData, row)
  // 编辑时不显示密码，留空表示不修改
  formData.password = ''
  showPassword.value = false
  dialogVisible.value = true
}

// 删除
const handleDelete = async (row: DataSourceConfig) => {
  try {
    await ElMessageBox.confirm('确定要删除该数据源吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    await deleteDataSource(row.id!)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败')
    }
  }
}

// 测试连接（表格中）
const handleTestConnection = async (row: DataSourceConfig) => {
  try {
    // 先获取完整的数据源信息（包含加密的密码）
    const { getDataSourceById } = await import('@/api/datasource')
    const fullConfig = await getDataSourceById(row.id!)
    
    // 响应拦截器已经解包，fullConfig 就是 DataSourceConfig 对象
    if (fullConfig) {
      // 使用完整配置测试连接
      const result = await testConnection(fullConfig)
      // result 已经是解包后的 boolean 值
      if (result) {
        ElMessage.success('连接成功')
      } else {
        ElMessage.error('连接失败')
      }
    }
  } catch (error) {
    console.error('连接测试失败:', error)
    ElMessage.error('连接测试失败')
  }
}

// 测试连接（对话框中）
const handleTestConnectionInDialog = async () => {
  if (!formRef.value) return
  
  // 先验证表单
  await formRef.value.validate(async (valid) => {
    if (!valid) {
      ElMessage.warning('请先填写完整的表单信息')
      return
    }
    
    testLoading.value = true
    try {
      const result = await testConnection(formData)
      // result 已经是解包后的 boolean 值
      if (result) {
        ElMessage.success('连接成功')
      } else {
        ElMessage.error('连接失败')
      }
    } catch (error) {
      console.error('连接测试失败:', error)
      ElMessage.error('连接测试失败')
    } finally {
      testLoading.value = false
    }
  })
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      if (formData.id) {
        await updateDataSource(formData)
        ElMessage.success('更新成功')
      } else {
        await createDataSource(formData)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadData()
    } catch (error) {
      ElMessage.error('操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 关闭对话框
const handleDialogClose = () => {
  formRef.value?.resetFields()
  Object.assign(formData, {
    id: undefined,
    name: '',
    code: '',
    dbType: 'mysql',
    dbName: 'easy_lowcode',
    url: 'jdbc:mysql://localhost:3306/easy_lowcode?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=Asia/Shanghai',
    username: '',
    password: '',
    driverClassName: 'com.mysql.cj.jdbc.Driver',  // 默认MySQL驱动
    status: 1,
    remark: '',
  })
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

// 获取数据库类型标签
const getDbTypeLabel = (dbType: string) => {
  const map: Record<string, string> = {
    mysql: 'MySQL',
    postgresql: 'PostgreSQL',
    oracle: 'Oracle',
    sqlserver: 'SQL Server',
    dm: '达梦 DM',
    kingbase: '人大金仓',
    gbase: '南大通用 GBase',
    oceanbase: 'OceanBase',
    tidb: 'TiDB',
    opengauss: 'openGauss',
    gaussdb: '华为 GaussDB',
    highgo: '瀚高 HighGo',
  }
  return map[dbType] || dbType
}

// 获取数据库类型标签颜色
const getDbTypeTag = (dbType: string) => {
  const map: Record<string, any> = {
    mysql: '',
    postgresql: 'success',
    oracle: 'warning',
    sqlserver: 'danger',
    dm: 'info',
    kingbase: 'info',
    gbase: 'info',
    oceanbase: 'info',
    tidb: 'info',
    opengauss: 'info',
    gaussdb: 'info',
    highgo: 'info',
  }
  return map[dbType] || ''
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.datasource-management {
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

.dialog-content {
  overflow: hidden;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}
</style>
