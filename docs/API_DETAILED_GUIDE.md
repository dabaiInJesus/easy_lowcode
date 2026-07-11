# API 详细使用指南

## 📋 目录

1. [通用规范](#通用规范)
2. [分页查询标准](#分页查询标准)
3. [排序和过滤](#排序和过滤)
4. [幂等性与重复提交防护](#幂等性与重复提交防护)
5. [权限与数据隐藏](#权限与数据隐藏)
6. [模块API详解](#模块api详解)

---

## 🎯 通用规范

### 请求格式

所有API请求都应遵循以下格式：

```http
POST /api/{module}/{resource}
Content-Type: application/json
Authorization: Bearer {jwt_token}
X-Request-ID: {uuid}

{
  "fieldName": "value"
}
```

### 响应格式 (统一)

所有API响应都遵循以下结构：

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890000
}
```

**响应字段说明**:

| 字段 | 类型 | 说明 | 示例 |
|------|------|------|------|
| `code` | Integer | 状态码（0表示成功） | 200 |
| `message` | String | 消息提示 | "操作成功" / "用户不存在" |
| `data` | Object/Array/null | 返回的业务数据 | 见各API详解 |
| `timestamp` | Long | 服务器时间戳（毫秒） | 1720700000000 |

### 通用请求头

| Header | 必须 | 说明 | 示例 |
|--------|------|------|------|
| `Authorization` | ✅ | JWT认证Token | `Bearer eyJhbGc...` |
| `Content-Type` | ✅ | 请求类型 | `application/json` |
| `X-Request-ID` | ⭐ | 请求唯一标识(防重复) | `550e8400-e29b-41d4-a716-446655440000` |
| `X-Client-Version` | ❌ | 客户端版本(用于兼容) | `1.0.0` |
| `X-Device-ID` | ❌ | 设备标识 | `device_abc123` |

### CORS处理

后端已在Gateway中配置CORS，前端无需额外处理。允许的来源、方法、请求头已预配置。

---

## 📄 分页查询标准

### 分页请求格式

```http
GET /api/{module}/{resource}/page?current=1&size=20&sort=createTime,desc

或

POST /api/{module}/{resource}/search
Content-Type: application/json

{
  "current": 1,
  "size": 20,
  "sort": "createTime,desc"
}
```

### 分页响应格式

```json
{
  "code": 200,
  "data": {
    "current": 1,
    "size": 20,
    "total": 100,
    "pages": 5,
    "records": [
      { "id": 1, "name": "User 1", "createTime": "2024-01-01 10:00:00" },
      { "id": 2, "name": "User 2", "createTime": "2024-01-02 10:00:00" }
    ]
  }
}
```

**分页字段说明**:

| 字段 | 类型 | 说明 |
|------|------|------|
| `current` | Integer | 当前页码 (从1开始) |
| `size` | Integer | 每页记录数 |
| `total` | Long | 总记录数 |
| `pages` | Integer | 总页数 |
| `records` | Array | 当前页的记录列表 |

### 分页参数范围

```
最小: size = 1
最大: size = 1000 (防止一次加载过多数据)
默认: size = 20

current范围: >= 1
```

### 分页最佳实践

```typescript
// ✅ 正确
const { current, size, total } = pageResult.value;
const pages = Math.ceil(total / size);
onPageChange = (newPage) => {
  fetchData({ current: newPage, size });
};

// ❌ 错误
const pages = pageResult.value.pages; // 不要依赖后端的pages，自己计算
pageResult.value.total = 0; // 不要修改total，重新查询
```

---

## 🔍 排序和过滤

### 单字段排序

```
GET /api/datasource/list?sort=createTime,desc
GET /api/datasource/list?sort=sourceName,asc
```

**格式**: `{字段名},{排序方向}`

**排序方向** (case-insensitive):
- `asc` - 升序 (默认)
- `desc` - 降序

### 多字段排序

```
GET /api/role/page?sort=status,desc&sort=createTime,desc
```

执行顺序: 先按status降序，再按createTime降序

### 过滤条件

#### 方式1: 查询参数（简单过滤）

```
GET /api/datasource/list?sourceType=mysql&status=1
```

#### 方式2: 请求体（复杂过滤）

```json
POST /api/datasource/search
{
  "current": 1,
  "size": 20,
  "filters": {
    "sourceType": "mysql",
    "status": 1,
    "sourceName": {"$contains": "test"} // 模糊搜索
  },
  "sort": "createTime,desc"
}
```

### 支持的过滤操作符

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `$eq` | 等于 | `{"status": {"$eq": 1}}` 等同于 `{"status": 1}` |
| `$ne` | 不等于 | `{"status": {"$ne": 0}}` |
| `$gt` | 大于 | `{"port": {"$gt": 1000}}` |
| `$gte` | 大于等于 | `{"port": {"$gte": 1000}}` |
| `$lt` | 小于 | `{"port": {"$lt": 9999}}` |
| `$lte` | 小于等于 | `{"port": {"$lte": 9999}}` |
| `$in` | 在列表中 | `{"status": {"$in": [0, 1]}}` |
| `$nin` | 不在列表中 | `{"status": {"$nin": [2, 3]}}` |
| `$contains` | 包含子串(模糊) | `{"sourceName": {"$contains": "mysql"}}` |
| `$startsWith` | 以...开头 | `{"username": {"$startsWith": "admin"}}` |
| `$endsWith` | 以...结尾 | `{"email": {"$endsWith": "@example.com"}}` |
| `$between` | 范围查询 | `{"createTime": {"$between": ["2024-01-01", "2024-12-31"]}}` |
| `$exists` | 字段存在 | `{"phone": {"$exists": true}}` |

### 高级查询示例

```json
// 查询所有启用的MySQL数据源，按创建时间降序
{
  "current": 1,
  "size": 20,
  "filters": {
    "sourceType": "mysql",
    "status": 1,
    "sourceName": {"$contains": "prod"}
  },
  "sort": "createTime,desc"
}

// 查询本周创建的资源
{
  "filters": {
    "createTime": {
      "$between": ["2024-07-01T00:00:00", "2024-07-07T23:59:59"]
    }
  }
}

// 查询没有绑定任何大屏的资源
{
  "filters": {
    "dashboardId": {"$exists": false}
  }
}
```

---

## 🔐 幂等性与重复提交防护

### 问题场景

```
用户点击"保存"按钮
  ↓
请求发送到服务器，但网络超时
  ↓
用户看不到响应，再次点击"保存"
  ↓
同一条记录被创建2次，或被更新2次！
```

### 解决方案: X-Request-ID

**所有修改类操作（POST/PUT/DELETE）必须**:

1. **前端生成唯一ID**:
   ```typescript
   const requestId = generateUUID(); // 如: '550e8400-e29b-41d4-a716-446655440000'
   ```

2. **发送请求时附加ID**:
   ```typescript
   const response = await axios.post('/api/datasource/create', 
     { sourceName: 'mysql_prod' },
     {
       headers: {
         'X-Request-ID': requestId
       }
     }
   );
   ```

3. **后端幂等处理**:
   ```java
   // 后端自动检查（无需开发者实现）
   // 如果相同requestId在1小时内重复提交
   // → 返回上次的结果，不重复执行
   ```

### 使用场景

| 操作 | 需要幂等保护 | 说明 |
|------|-----------|------|
| 创建数据源 | ✅ 是 | 重复创建会产生重复记录 |
| 编辑数据源 | ✅ 是 | 重复更新可能产生版本混乱 |
| 删除数据源 | ⚠️ 部分 | 第二次删除返回"已删除"即可 |
| 查询数据 | ❌ 否 | 读操作天然幂等 |
| 测试连接 | ❌ 否 | 重复测试不影响 |
| ETL执行 | ✅ 是 | 防止数据重复导入 |

---

## 🛡️ 权限与数据隐藏

### 敏感字段隐藏

某些字段会根据用户权限自动隐藏：

| 字段 | 场景 | 权限要求 |
|------|------|---------|
| `password` | 数据源密码 | 需要 `datasource:view-secret` 权限 |
| `apiKey` | 第三方API Key | 需要 `ai:view-secret` 权限 |
| `phone` | 用户手机号 | 需要 `user:view-phone` 权限 |
| `email` | 用户邮箱 | 需要 `user:view-email` 权限 (可选) |

**响应示例**:

```json
// 有权限时
{
  "id": 1,
  "sourceName": "mysql_prod",
  "password": "p@ssw0rd123"  // ✅ 显示密码
}

// 无权限时
{
  "id": 1,
  "sourceName": "mysql_prod",
  "password": "***"  // ❌ 显示掩码
}
```

### 数据访问控制

#### 组织级权限

```
用户A (部门: 技术部)
  ↓
查询资源 → 只返回技术部创建的资源
  ↓
不返回其他部门的资源（即使查询参数包含）
```

#### 用户级权限

```
普通用户
  ↓
查询自己的配置 → 返回
  ↓
查询其他用户的配置 → 403 Forbidden
  ↓
管理员可以查询所有用户的配置
```

### 权限字符串格式

标准格式: `{module}:{resource}:{operation}`

```
datasource:list         // 数据源列表查询
datasource:view        // 数据源详情查询
datasource:create      // 创建数据源
datasource:update      // 编辑数据源
datasource:delete      // 删除数据源
datasource:test        // 测试连接

resource:query         // 资源查询执行
resource:design        // 资源设计编辑

dashboard:list         // 大屏列表
dashboard:design       // 大屏设计编辑
dashboard:publish      // 大屏发布

ai:chat                // AI对话
ai:config              // AI配置管理

etl:schedule           // ETL任务调度
etl:execute            // ETL任务执行
```

---

## 📡 模块API详解

### 1. 认证模块 (/api/auth)

#### 登录

```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "123456"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIs...",
    "expiresIn": 3600,
    "tokenType": "Bearer"
  }
}
```

#### 刷新Token

```http
POST /api/auth/refresh
Authorization: Bearer {refresh_token}
```

#### 登出

```http
POST /api/auth/logout
Authorization: Bearer {token}
```

---

### 2. 数据源模块 (/api/datasource)

#### 创建数据源

```http
POST /api/datasource/create
Authorization: Bearer {token}
X-Request-ID: {uuid}

{
  "sourceName": "mysql_prod",
  "sourceType": "mysql",
  "host": "192.168.1.100",
  "port": 3306,
  "databaseName": "easy_lowcode",
  "username": "root",
  "password": "p@ssw0rd123",
  "status": 1
}
```

#### 分页查询数据源

```http
GET /api/datasource/page?current=1&size=20&sourceType=mysql&status=1
Authorization: Bearer {token}
```

**查询参数**:
- `current` - 页码
- `size` - 每页数量
- `sourceType` - 数据库类型 (mysql/postgresql/oracle等)
- `status` - 状态 (0=禁用, 1=启用)

#### 测试连接

```http
POST /api/datasource/test
Authorization: Bearer {token}

{
  "sourceType": "mysql",
  "host": "localhost",
  "port": 3306,
  "databaseName": "test_db",
  "username": "root",
  "password": "123456"
}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "success": true,
    "message": "连接成功"
  }
}
```

#### 扫描表

```http
GET /api/datasource/{datasourceId}/tables
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "data": [
    {
      "tableName": "sys_user",
      "tableComment": "用户表",
      "columnCount": 8,
      "recordCount": 125
    }
  ]
}
```

---

### 3. 资源模块 (/api/resource)

#### 资源查询 - 单资源

```http
POST /api/resource/search/single
Authorization: Bearer {token}

{
  "resourceCode": "user_list",
  "params": {
    "status": 1,
    "deptId": 10
  },
  "templateName": "template_1",
  "current": 1,
  "size": 20
}
```

**params说明**: 与configJson中定义的参数相对应

#### 资源查询 - 多资源统一Key

```http
POST /api/resource/search/multi
Authorization: Bearer {token}

{
  "keyName": "userId",
  "keyValue": "123",
  "resourceCodes": ["user_detail", "user_roles", "user_permissions"],
  "current": 1,
  "size": 20
}
```

返回三个资源的查询结果，使用同一个userId值

#### 资源查询 - 全文检索

```http
POST /api/resource/search/fulltext
Authorization: Bearer {token}

{
  "query": "mysql connection",
  "resourceCodes": ["datasource", "resource"],
  "current": 1,
  "size": 20
}
```

搜索包含"mysql"和"connection"关键词的所有记录

---

### 4. 大屏模块 (/api/dashboard)

#### 创建大屏

```http
POST /api/dashboard/create
Authorization: Bearer {token}
X-Request-ID: {uuid}

{
  "dashboardName": "销售看板",
  "dashboardCode": "sale_dashboard",
  "description": "实时销售数据展示",
  "width": 1920,
  "height": 1080,
  "refreshInterval": 5000,
  "status": 1
}
```

#### 查询大屏详情

```http
GET /api/dashboard/{dashboardId}
Authorization: Bearer {token}
```

**响应**:
```json
{
  "code": 200,
  "data": {
    "id": 123,
    "dashboardName": "销售看板",
    "charts": [
      {
        "id": 1,
        "chartType": "line",
        "x": 0,
        "y": 0,
        "width": 500,
        "height": 300,
        "dataSource": "sales_trend",
        "sql": "SELECT DATE_FORMAT(createTime, '%Y-%m-%d') as date, SUM(amount) as total FROM sale_order GROUP BY DATE_FORMAT(createTime, '%Y-%m-%d')"
      }
    ]
  }
}
```

#### 查询大屏数据

```http
GET /api/dashboard/{dashboardId}/data
Authorization: Bearer {token}
```

实时获取大屏所有图表的数据

---

### 5. AI模块 (/api/ai)

#### 创建对话

```http
POST /api/ai/conversation/create
Authorization: Bearer {token}
X-Request-ID: {uuid}

{
  "title": "数据库设计咨询",
  "model": "qwen-plus",
  "provider": "dashscope"
}
```

#### 发送消息

```http
POST /api/ai/conversation/{conversationId}/message
Authorization: Bearer {token}
Content-Type: application/json

{
  "content": "如何设计一个百万级用户的用户表？",
  "attachments": []
}
```

**流式响应** (SSE):

```javascript
const eventSource = new EventSource(
  '/api/ai/conversation/123/stream?token=xxx'
);
eventSource.onmessage = (event) => {
  const chunk = event.data; // AI流式响应的文本片段
  console.log(chunk);
};
```

---

### 6. ETL模块 (/api/etl)

#### 创建ETL任务

```http
POST /api/etl/task/create
Authorization: Bearer {token}
X-Request-ID: {uuid}

{
  "taskName": "日报数据导入",
  "sourceDataSourceId": 1,
  "sourceSQL": "SELECT * FROM daily_report",
  "targetDataSourceId": 2,
  "targetTable": "backup_daily_report",
  "transformSQL": "SELECT *, NOW() as import_time FROM dual",
  "cronExpression": "0 0 1 * * ?",
  "enabled": true
}
```

#### 执行ETL任务

```http
POST /api/etl/task/{taskId}/execute
Authorization: Bearer {token}
X-Request-ID: {uuid}
```

#### 查看执行日志

```http
GET /api/etl/task/{taskId}/logs?current=1&size=20
Authorization: Bearer {token}
```

---

## 🧪 示例代码

### 使用 curl

```bash
# 登录
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 查询数据源
curl -X GET "http://localhost:8081/api/datasource/page?current=1&size=20" \
  -H "Authorization: Bearer <your_token>"

# 创建数据源（带幂等ID）
curl -X POST http://localhost:8081/api/datasource/create \
  -H "Authorization: Bearer <your_token>" \
  -H "X-Request-ID: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{"sourceName":"mysql_prod","sourceType":"mysql",...}'
```

### 使用 JavaScript/Axios

```typescript
// 创建Axios实例
const api = axios.create({
  baseURL: 'http://localhost:8081/api',
  timeout: 30000,
  headers: { 'Content-Type': 'application/json' }
});

// 请求拦截器 - 自动注入Token和请求ID
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  config.headers['X-Request-ID'] = generateUUID();
  return config;
});

// 响应拦截器 - 自动解包data
api.interceptors.response.use(
  (res) => {
    if (res.data.code === 200) return res.data.data;
    throw new Error(res.data.message);
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.removeItem('token');
      router.push('/login');
    }
    throw err;
  }
);

// 使用示例
const result = await api.get('/datasource/page', { 
  params: { current: 1, size: 20 } 
});
```

---

## 📊 性能优化建议

1. **合理的分页大小**: size=20是平衡点（加载快、交互流畅）
2. **使用字段白名单**: 只查询需要的字段，减少网络传输
3. **缓存机制**: 2分钟内相同查询会命中缓存
4. **避免大量数据导出**: size最大1000，如需更多使用文件导出API

---

**文档完成！** 现在前后端有了明确的API契约 🎉
