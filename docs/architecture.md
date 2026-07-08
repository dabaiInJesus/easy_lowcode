# Easy Lowcode — 技术架构文档

> 版本：1.0.0-SNAPSHOT
> 最后更新：2026-07-08

## 1. 总体架构

### 1.1 系统拓扑

```
┌─────────────────────────────────────────────────────────┐
│                    前端 (Vue 3 SPA)                      │
│  Element Plus · Pinia · Vue Router · Axios · Vite       │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / JSON
                       │ Authorization: Bearer {token}
                       ▼
┌─────────────────────────────────────────────────────────┐
│               API Gateway (可选独立部署)                   │
│              Spring Cloud Gateway / Startup              │
└──────────┬──────────┬──────────┬──────────┬─────────────┘
           │          │          │          │
           ▼          ▼          ▼          ▼
┌─────auth────┐ ┌─resource─┐ ┌dashboard─┐ ┌─collector──┐
│ 用户/角色    │ │ 数据源    │ │ 大屏设计  │ │ 同步管理    │
│ 菜单/部门    │ │ 资源配置  │ │ 图表管理  │ │ MeiliSearch │
│ RBAC 权限   │ │ API注册   │ │ SQL引擎  │ │ MinIO 存储  │
│ Spring      │ │ 查询服务  │ │ Text2SQL │ │ RocketMQ    │
│ Security    │ │ 处理器链  │ │ AI推荐   │ │ Tika 解析   │
└──────┬──────┘ └─────┬─────┘ └────┬─────┘ └──────┬──────┘
       │              │             │              │
       └──────────────┼─────────────┼──────────────┘
                      │             │
                      ▼             ▼
          ┌───────────────────────────────┐
          │           PostgreSQL           │
          │    pgvector · Liquibase · SQL  │
          └───────────────────────────────┘
                      │
          ┌───────────┴───────────┐
          ▼                       ▼
       Redis                  RocketMQ
   (Token黑名单/缓存)       (数据同步消息)
```

### 1.2 模块依赖关系

```
easy-lowcode-common           # 基础：工具类、常量、异常、Result、JWT、安全
    ↑
easy-lowcode-database         # MyBatis Plus 配置、Liquibase、BaseEntity/BaseMapper
    ↑
    ├── easy-lowcode-auth      # Spring Security、RBAC、Flowable
    ├── easy-lowcode-resource  # configJson 配置、处理器链、查询引擎 (依赖 collector)
    ├── easy-lowcode-dashboard # 大屏设计器、图表引擎、SqlEngine (依赖 collector)
    ├── easy-lowcode-collector # 数据源管理、全文检索、存储抽象 (无业务模块依赖)
    └── easy-lowcode-etl       # Spring Batch 任务引擎 (依赖 collector)
    ↑
easy-lowcode-startup          # 整合所有业务模块，单一可执行 JAR

easy-lowcode-gateway          # 独立部署，Spring Cloud Gateway + JWT 鉴权
```

### 1.3 请求处理链路

```
用户操作 → Vue Router (动态路由) → Pinia Store → Axios 请求
    → 请求拦截器 (注入 Token) → Gateway/Startup → Controller
    → @PreAuthorize (权限检查) → Service (业务逻辑)
    → Mapper (MyBatis Plus) → PostgreSQL → 响应 → 处理器链
    → Controller 返回 Result → Axios 响应拦截器 (解包/401跳转)
    → 组件渲染 (Element Plus table/form)
```

---

## 2. 技术栈

### 2.1 后端

| 组件 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 运行环境 |
| Spring Boot | 3.5.5 | 应用框架 |
| MyBatis Plus | 3.5.5 | ORM，简化数据访问 |
| PostgreSQL | 17 | 主数据库，支持 pgvector |
| Liquibase | 4.25.1 | 数据库版本管理 |
| Spring Security | 6.x | 认证授权 |
| jjwt | 0.12.5 | JWT Token 签发验证 |
| Redis | 6+ | Token 黑名单、数据缓存 |
| RocketMQ | 5.3 | 数据同步消息 |
| Spring Batch | 5.x | ETL 批处理任务 |
| Flowable | 7.0.1 | 工作流引擎 |
| Spring AI | 1.1.2 | AI Provider 统一接入 |
| SpringDoc | 2.8.9 | OpenAPI 文档 |

### 2.2 前端

| 组件 | 版本 | 用途 |
|------|------|------|
| Vue | 3.4+ | 前端框架 |
| TypeScript | 5.x | 类型安全 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| Pinia | 2.x | 状态管理 |
| Vue Router | 4.x | 路由管理 |
| Axios | 1.x | HTTP 请求 |
| ECharts | 5.x | 图表渲染 |
| Monaco Editor | - | SQL 编辑器 |
| dayjs | - | 日期处理 |
| vue-grid-layout | - | 大屏拖拽布局 |

### 2.3 基础设施

| 组件 | 版本 | 用途 |
|------|------|------|
| Meilisearch | 1.12+ | 全文搜索引擎（默认） |
| MinIO | - | 文档文件存储（默认 S3 协议） |
| Docker | - | 容器化部署 |

---

## 3. 核心设计

### 3.1 configJson 配置体系

configJson 是整个资源配置的核心数据结构，存储于 `table_resource.config_json` 字段。

#### 3.1.1 数据结构

```json
{
  "fields": [
    {
      "fieldName": "order_id",
      "displayName": "订单ID",
      "dataType": "VARCHAR",
      "queryable": true,
      "returnable": true,
      "sortable": true,
      "queryWidget": "input"
    }
  ],
  "parameterProcessors": [
    {
      "type": "default_value",
      "order": 1,
      "enabled": true,
      "config": { "field": "status", "value": "1" }
    },
    {
      "type": "param_mapping",
      "order": 2,
      "enabled": true,
      "config": { "mappings": { "name": "username" } }
    },
    {
      "type": "param_validator",
      "order": 3,
      "enabled": true,
      "config": { "rules": { "pageSize": { "max": 100 } } }
    }
  ],
  "resultProcessors": [
    {
      "type": "field_filter",
      "order": 1,
      "enabled": true,
      "config": { "fields": ["order_id", "amount"] }
    },
    {
      "type": "data_masking",
      "order": 2,
      "enabled": true,
      "config": { "fields": { "phone": "middle" } }
    },
    {
      "type": "enum_mapping",
      "order": 3,
      "enabled": true,
      "config": {
        "field": "status",
        "mappings": { "0": "禁用", "1": "启用" }
      }
    },
    {
      "type": "date_format",
      "order": 4,
      "enabled": true,
      "config": { "fields": { "createTime": "yyyy-MM-dd HH:mm:ss" } }
    }
  ],
  "queryTemplates": [
    {
      "name": "default",
      "sql": "SELECT * FROM orders {{#if status}}WHERE status = {{status}}{{/if}} ORDER BY create_time DESC LIMIT {{pageSize}} OFFSET {{offset}}",
      "description": "默认查询模板"
    }
  ],
  "displaySettings": {
    "listFields": [
      { "field": "order_id", "width": 180, "sortable": true },
      { "field": "amount", "width": 120, "sortable": true }
    ],
    "detailFields": [
      { "field": "order_id", "label": "订单编号" },
      { "field": "amount", "label": "金额" }
    ],
    "pageSize": 20
  }
}
```

#### 3.1.2 生命周期

```
配置时                             运行时
┌──────────┐    ┌───────────┐    ┌────────────┐
│ 字段配置   │ →  │ 处理器配置  │ →  │ 查询模板配置 │
│ fields    │    │ processors │    │ templates  │
└──────────┘    └───────────┘    └────────────┘
                                          │
                                          ▼
                                    ┌────────────┐
                                    │ 显示配置     │
                                    │ display     │
                                    └────────────┘

执行时：
用户输入 → 参数处理器链 → SQL 模板渲染 → 数据库执行
    → 结果处理器链 → 显示配置格式化 → 前端渲染
```

### 3.2 处理器链 (Processor Chain)

#### 3.2.1 架构

```
┌──────────────── Processor<T> ─────────────────┐
│  - type(): String          // 处理器类型标识    │
│  - order(): int            // 执行顺序          │
│  - enabled(): boolean      // 是否启用          │
│  - process(T input, Map<String,Object> config) │
└──────────────────────────────────────────────┘
                    ↑
        ┌───────────┴───────────┐
        │                       │
┌───────┴──────────┐   ┌───────┴──────────┐
│ ParameterProcessor│   │ ResultProcessor  │
│  T = Map<String,  │   │  T = List<Map<   │
│   Object>         │   │   String,Object>>│
└──────────────────┘   └──────────────────┘

┌─────── ProcessorRegistry ───────┐
│  注册所有内建处理器               │
│  getProcessor(type) → Processor  │
└──────────────────────────────────┘

┌─────── ProcessorChain<T> ───────┐
│  按 order 排序                   │
│  只执行 enabled = true 的        │
│  链式传递 T                      │
└──────────────────────────────────┘
```

#### 3.2.2 内建处理器

| 处理器 | 类型 | 说明 |
|--------|------|------|
| `DefaultValueProcessor` | 参数 | 参数为空时填充默认值 |
| `ParamMappingProcessor` | 参数 | 将查询参数名映射为字段名 |
| `ParamValidatorProcessor` | 参数 | 参数校验（最大值、最小值、必填等） |
| `FieldFilterProcessor` | 结果 | 只返回指定字段 |
| `DataMaskingProcessor` | 结果 | 数据脱敏（手机号中间四位、邮箱前缀等） |
| `EnumMappingProcessor` | 结果 | 枚举值映射为显示文本 |
| `DateFormatProcessor` | 结果 | 日期字段格式化 |

#### 3.2.3 自定义处理器开发步骤

```java
@Component
public class CustomProcessor implements ParameterProcessor {
    @Override
    public String type() { return "custom"; }

    @Override
    public int order() { return 10; }

    @Override
    public boolean enabled() { return true; }

    @Override
    public Map<String, Object> process(Map<String, Object> params,
                                        Map<String, Object> config) {
        // 自定义处理逻辑
        return params;
    }
}
```

### 3.3 模板引擎

#### 3.3.1 占位符语法

| 语法 | 说明 | 示例 |
|------|------|------|
| `{{field}}` | 参数替换 | `WHERE id = {{id}}` |
| `{{#if field}}...{{/if}}` | 条件包含 | `{{#if status}}AND status={{status}}{{/if}}` |
| `{{#unless field}}...{{/unless}}` | 条件排除 | `{{#unless status}}AND status IS NULL{{/unless}}` |

#### 3.3.2 SQL 注入防护

```
用户输入
    │
    ▼
┌─────────────────────────────────────┐
│ 1. 关键词黑名单检查                   │
│    DROP, ALTER, DELETE, TRUNCATE,   │
│    EXEC, UNION, INSERT, UPDATE,     │
│    pg_sleep, information_schema     │
└──────────────┬──────────────────────┘
               │ 通过
               ▼
┌─────────────────────────────────────┐
│ 2. 列白名单检查                      │
│    只允许 configJson.fields 中定义   │
│    的字段名出现在 SQL 中              │
└──────────────┬──────────────────────┘
               │ 通过
               ▼
┌─────────────────────────────────────┐
│ 3. 参数化绑定                        │
│    使用 PreparedStatement 占位符     │
└─────────────────────────────────────┘
```

### 3.4 搜索引擎抽象

#### 3.4.1 架构

```
┌────── SearchService ──────┐
│  search(index, query)     │
│  indexDocument(index, doc)│
│  deleteDocument(index, id)│
│  createIndex(name, config)│
└───────────┬───────────────┘
            │
    ┌───────┴───────┐
    │               │
    ▼               ▼
┌──────────┐  ┌──────────┐
│ Meiliscar│  │ Elasticsc│
│ hSearch  │  │ archSearc│
│ Service  │  │ hService │
│ (默认)    │  │ (可选)    │
└──────────┘  └──────────┘
```

#### 3.4.2 配置切换

```yaml
fulltext:
  search:
    type: meilisearch   # 或 elasticsearch
    meilisearch:
      host: http://localhost:7700
      api-key: ""
    elasticsearch:
      host: http://localhost:9200
      api-key: ""
```

### 3.5 存储抽象

#### 3.5.1 架构

```
┌────── StorageService ─────┐
│  upload(file) → String    │
│  download(path) → byte[]  │
│  delete(path) → void      │
│  getUrl(path) → String    │
└───────────┬───────────────┘
            │
    ┌───────┼───────┐
    │       │       │
    ▼       ▼       ▼
┌────────┐┌──────┐┌──────┐
│ MinIO  ││ AWS  ││Local │
│ (默认)  ││ S3   ││File  │
└────────┘└──────┘└──────┘
```

### 3.6 SQL 引擎 (Dashboard)

#### 3.6.1 架构

```
┌────── SqlEngine ──────┐
│  execute(datasourceId, │
│    sql, params)        │
│  testConnection(ds)    │
│  getDialect()          │
│  getColumns(ds, table) │
└───────────┬───────────┘
            │
    ┌───────┴───────┐
    │               │
    ▼               ▼
┌──────────┐  ┌──────────┐
│ JdbcSql  │  │ HiveSql  │
│ Engine   │  │ Engine   │
│ (MySQL/  │  │ (Hive/   │
│ PG/      │  │ Kerberos)│
│ Oracle/  │  │          │
│ SQLServer│  │          │
└──────────┘  └──────────┘
         │
         ▼
┌──────────────┐
│ SqlEngine    │
│ Factory      │
│ (按dsId缓存)  │
└──────────────┘
```

---

## 4. 数据库设计

### 4.1 核心表清单

#### 4.1.1 系统管理 (auth)

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `sys_user` | 系统用户 | username, password, nickname, phone, email, dept_id, status |
| `sys_role` | 角色 | role_code(唯一), role_name, status, sort |
| `sys_user_role` | 用户-角色关联 | user_id, role_id |
| `sys_menu` | 菜单树 | menu_name, parent_id, path, component, icon, sort, permission, menu_type |
| `sys_role_menu` | 角色-菜单关联 | role_id, menu_id |
| `sys_dept` | 部门树 | dept_name, parent_id, leader, sort |

#### 4.1.2 数据采集 (collector)

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `collector_datasource` | 数据源配置 | ds_name, ds_code, db_type, host, port, db_name, username, password(加密) |
| `collector_sync_config` | 同步配置 | source_ds_id, target_ds_id, table_name, sync_strategy, cron |
| `collector_unified_key_mapping` | 统一Key映射 | key_name, source_resource, source_field, target_resource, target_field |
| `collector_fulltext_document` | 全文检索文档 | file_name, file_type, file_size, storage_path, content_text, indexed |
| `collector_fulltext_index_config` | 索引配置 | config_name, search_engine_type, host, api_key, index_name |

#### 4.1.3 资源管理 (resource)

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `table_resource` | 表资源定义 | resource_name, resource_code, datasource_id, table_name, config_json |
| `table_resource_field` | 表资源字段 | resource_id, field_name, data_type, comment |
| `resource_api` | API 注册 | api_name, api_path, method, resource_id, param_mapping, enabled |
| `sys_resource` | 资源权限 | 对应菜单/按钮的权限定义 |
| `sys_role_resource` | 角色-资源关联 | role_id, resource_id |

#### 4.1.4 可视化 (dashboard)

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `dashboard` | 大屏定义 | name, code, width, height, background, layout_json, refresh_interval, status |
| `dashboard_chart` | 图表定义 | title, chart_type, datasource_id, sql_text, x_field, y_field, group_field, filters, options, x, y, w, h |
| `chart_data_source` | 图表备用数据源 | chart_id, datasource_id, sql_text, transform_script, cache_ttl |

#### 4.1.5 ETL (etl)

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `etl_task` | ETL 任务 | task_name, task_code, source_ds_id, target_ds_id, source_table, target_table, transform_rules |
| `etl_task_log` | 任务日志 | task_id, status, started_at, finished_at, records_processed, error_message |

#### 4.1.6 AI

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `ai_config` | AI Provider 配置 | provider, api_key(加密), model, temperature, enabled |
| `ai_agent` | AI Agent 定义 | name, code, system_prompt, provider, model |

### 4.2 ER 关系概览

```
sys_user ──< sys_user_role >── sys_role ──< sys_role_menu >── sys_menu
  │                              │
  │                              └──< sys_role_resource >── sys_resource
  │
  └── sys_dept

collector_datasource ──< table_resource ──< table_resource_field
                      │       │
                      │       └── resource_api
                      │
                      ├──< collector_sync_config
                      │
                      └──< dashboard_chart ──< chart_data_source

dashboard ──< dashboard_chart

etl_task ──< etl_task_log

collector_unified_key_mapping (关联不同resource)
collector_fulltext_document (独立)
collector_fulltext_index_config (独立)
```

### 4.3 数据库约定

- 主键策略：雪花算法 (`IdType.ASSIGN_ID`)
- 逻辑删除：`deleted` 字段（0=未删除, 1=已删除）
- 自动填充：`create_time`, `update_time`, `create_by`, `update_by`
- 命名约定：表名/字段名使用 `snake_case`，MyBatis Plus 自动驼峰转换
- 迁移管理：Liquibase XML，变更集按序号递增（031, 032, ...）

---

## 5. API 设计

### 5.1 通用规范

- **基础路径**：`/api/{module}/{resource}`
- **请求方法**：GET（查询）、POST（创建）、PUT（更新）、DELETE（删除）
- **认证方式**：`Authorization: Bearer {token}`
- **内容类型**：`application/json`

### 5.2 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890
}
```

| code | 说明 |
|------|------|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未认证/Token 过期 |
| 403 | 无权限 |
| 500 | 服务器内部错误 |

### 5.3 分页响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {
    "records": [],
    "total": 100,
    "size": 10,
    "current": 1,
    "pages": 10
  },
  "timestamp": 1234567890
}
```

### 5.4 核心 API 端点

#### 系统管理

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/login` | 登录 |
| POST | `/api/auth/logout` | 登出 |
| GET | `/api/auth/user/page` | 用户分页 |
| POST | `/api/auth/user` | 新增用户 |
| PUT | `/api/auth/user` | 编辑用户 |
| DELETE | `/api/auth/user/{id}` | 删除用户 |
| GET | `/api/auth/role/page` | 角色分页 |
| POST | `/api/auth/role` | 新增角色 |
| POST | `/api/auth/role/{roleId}/menus` | 角色分配菜单 |
| GET | `/api/auth/menu/tree` | 菜单树 |
| GET | `/api/auth/dept/tree` | 部门树 |

#### 数据采集

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/collector/datasource/page` | 数据源分页 |
| POST | `/api/collector/datasource` | 新增数据源 |
| POST | `/api/collector/datasource/test` | 测试连接 |
| GET | `/api/collector/datasource/{id}/tables` | 扫描表列表 |
| GET | `/api/collector/sync-config/page` | 同步配置分页 |

#### 资源管理

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/resource/table-resource/page` | 表资源分页 |
| POST | `/api/resource/table-resource` | 注册表资源 |
| GET | `/api/resource/table-resource/{id}/fields` | 获取字段+configJson |
| POST | `/api/resource/table-resource/{id}/sync-fields` | 同步字段 |
| POST | `/api/resource/table-resource/{id}/generate-api` | 生成API |
| GET | `/api/resource/api/page` | API 分页 |

#### 资源查询

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/resource/search/single` | 单资源查询 |
| POST | `/api/resource/search/single/{resourceCode}` | 单资源GET查询 |
| POST | `/api/resource/search/multi` | 多资源统一查询 |
| POST | `/api/resource/search/fulltext` | 全文检索 |
| POST | `/api/resource/search/keyword` | 关键词搜索 |
| GET | `/api/resource/search/fields/{resourceCode}` | 获取字段配置 |

#### 大屏

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/dashboard/dashboard/page` | 大屏分页 |
| POST | `/api/dashboard/dashboard` | 创建大屏 |
| GET | `/api/dashboard/dashboard/{id}` | 大屏详情（含图表） |
| PUT | `/api/dashboard/dashboard/{id}/publish` | 发布大屏 |
| POST | `/api/dashboard/chart` | 创建图表 |
| POST | `/api/dashboard/data/query` | 图表数据查询 |
| POST | `/api/dashboard/data/text-to-sql` | AI Text-to-SQL |
| POST | `/api/dashboard/data/explain` | SQL 解释 |

---

## 6. 安全架构

### 6.1 认证流程

```
用户 → 登录 → 后端验证用户名密码(BCrypt)
  → 生成 JWT Token (含 userId, roles, permissions)
  → 返回 Token → 前端存储到 localStorage + Pinia
  → 后续请求通过 Authorization Header 携带 Token
  → JwtAuthenticationFilter 解析 Token
  → 设置 SecurityContextHolder
  → 放行到 Controller
  → @PreAuthorize 注解检查权限
```

### 6.2 RBAC 权限模型

```
用户 ──< 用户角色关联 >── 角色 ──< 角色菜单关联 >── 菜单(按钮)
                          │
                          └──< 角色资源关联 >── 资源(数据权限)
```

### 6.3 安全措施

- **JWT Secret**：通过环境变量 `JWT_SECRET` 注入，禁止硬编码
- **Token 黑名单**：登出后将 Token 加入 Redis 黑名单，TTL 等于 Token 过期时间
- **密码安全**：BCrypt 加密，禁止日志输出
- **数据加密**：敏感字段（手机号、身份证）使用 AES 加密存储
- **SQL 注入防护**：参数化查询 + 关键词黑名单 + 列白名单
- **Long 精度**：`@JsonSerialize(ToStringSerializer.class)` 防止前端 JavaScript 精度丢失

---

## 7. 前端架构

### 7.1 状态管理 (Pinia Store)

```
useUserStore        ─ Token 持久化、登录/登出、用户信息
useAppStore         ─ 侧边栏折叠、主题、语言
useMenuStore        ─ 从后端菜单树动态生成路由、组件映射懒加载
```

### 7.2 动态路由机制

```
用户登录 → 获取菜单树 (GET /api/auth/menu/tree)
  → menuStore.buildRoutes() 遍历菜单树
  → menuStore.componentMap 中查找 component 路径
  → 懒加载对应页面组件
  → router.addRoute() 动态添加路由
```

### 7.3 可复用组合函数 (Composables)

| Composable | 用途 |
|------------|------|
| `useCommon` | CRUD 基础操作（列表/新增/编辑/删除 + 搜索） |
| `usePagination` | 分页状态管理 |
| `useTable` | 表格选择 + 排序 |
| `useCrudDialog` | 弹窗打开/关闭 + 表单重置 |
| `useCodeGenerator` | 按模块前缀生成编码 |
| `useDateFormat` | 日期格式化 |

### 7.4 请求拦截器

```
请求拦截器:
  从 useUserStore 获取 token
  如果存在 → 设置 Authorization Header
  如果不存在 → 放行（仅登录等公开接口）

响应拦截器:
  解包 res.data → 直接返回 data
  如果 code ≠ 200:
    code=401 → 清除用户信息 → 跳转登录页
    其他 → silentError 模式控制是否弹出 ElMessage.error
```

---

## 8. 部署架构

### 8.1 单体模式 (默认)

```
┌───────────┐  ┌───────────┐  ┌───────────┐  ┌───────────┐
│ PostgreSQL │  │   Redis   │  │ RocketMQ  │  │ Meilisearc│
└─────┬─────┘  └─────┬─────┘  └─────┬─────┘  └─────┬─────┘
      │               │              │               │
      └───────────────┼──────────────┼───────────────┘
                      │              │
                      ▼              ▼
              ┌──────────────────────────┐
              │ easy-lowcode-startup.jar │
              │     (内置所有模块)         │
              │      端口: 8081          │
              └──────────────────────────┘
```

### 8.2 微服务模式 (独立部署)

```
┌─────────────────────────────────────────────────┐
│             easy-lowcode-gateway                  │
│          Spring Cloud Gateway                    │
│       端口: 8080 | CORS | 统一认证                │
└──┬──────────┬──────────┬──────────┬──────────────┘
   │          │          │          │
   ▼          ▼          ▼          ▼
  auth      resource  dashboard  collector
  :8082     :8083     :8084     :8085
```

### 8.3 Docker Compose 服务

```yaml
services:
  postgres:     # pgvector/pg17
  redis:        # 8.6.2
  rocketmq-namesrv:  # 5.3.0
  rocketmq-broker:   # 5.3.0
  app:          # easy-lowcode-startup 镜像
```

---

## 9. 可扩展性设计

### 9.1 处理器扩展点

| 扩展点 | 实现方式 | 注册方式 |
|--------|---------|---------|
| 参数处理器 | 实现 `ParameterProcessor` | `@Component` 自动扫描 |
| 结果处理器 | 实现 `ResultProcessor` | `@Component` 自动扫描 |

### 9.2 数据源扩展点

| 扩展点 | 实现方式 |
|--------|---------|
| 新增数据库支持 | 添加 JDBC 驱动依赖 |
| 新增方言 | 在 `SqlEngineFactory` 注册新 dbType 到 JdbcSqlEngine |

### 9.3 搜索引擎扩展点

| 扩展点 | 实现方式 |
|--------|---------|
| 新增搜索引擎 | 实现 `SearchService` 接口，在 `FulltextDocumentServiceImpl` 中注册 |

### 9.4 存储后端扩展点

| 扩展点 | 实现方式 |
|--------|---------|
| 新增存储后端 | 实现 `StorageService` 接口，在 `FulltextDocumentServiceImpl` 中注册 |

### 9.5 AI Provider 扩展点

| 扩展点 | 实现方式 |
|--------|---------|
| 新增 AI Provider | 添加 Spring AI 的对应依赖，在配置文件中添加配置 |

---

## 10. 关键类图

### 10.1 configJson 模型类

```
ConfigJson
 ├── fields: List<FieldConfig>
 │    ├── fieldName, displayName, dataType
 │    └── queryable, returnable, sortable, queryWidget
 ├── parameterProcessors: List<ProcessorConfig>
 │    └── type, order, enabled, config(Map)
 ├── resultProcessors: List<ProcessorConfig>
 │    └── type, order, enabled, config(Map)
 ├── queryTemplates: List<QueryTemplate>
 │    └── name, sql, description
 └── displaySettings: DisplaySettings
      ├── listFields: List<DisplayFieldSetting>
      └── detailFields: List<DisplayFieldSetting>
```

### 10.2 资源搜索服务类

```
ResourceSearchService (接口)
  ├── search(SearchRequest) → SearchResponse
  ├── unifiedKeySearch(UnifiedKeyRequest) → Map<Resource, List<Map>>
  ├── fulltextSearch(FulltextRequest) → FulltextResponse
  └── getResourceFieldInfo(resourceCode) → Map<String,Object>

ResourceSearchServiceImpl (实现)
  ├── 依赖 ResourceCacheManager
  ├── 依赖 ResourceSchemaService
  ├── 依赖 SqlBuilderService
  └── 依赖 ResourceResultProcessor

ResourceCacheManager ─ 缓存表资源和字段配置 (LRU)
ResourceSchemaService ─ INFORMATION_SCHEMA 元数据解析
SqlBuilderService     ─ SQL 模板渲染 + 防注入
ResourceResultProcessor ─ 处理器链执行 + 显示格式
```

### 10.3 搜索 + 存储抽象

```
SearchService (接口)
  ├── MeilisearchSearchService (实现)
  └── ElasticsearchSearchService (实现)

StorageService (接口)
  ├── MinioStorageService (实现)
  ├── S3StorageService (实现)
  └── LocalStorageService (实现)

FulltextDocumentServiceImpl
  ├── 依赖 SearchService (搜索)
  ├── 依赖 StorageService (存文件)
  └── 依赖 TikaContentExtractor (解析内容)
```

---

## 11. 开发环境

### 11.1 环境要求

| 工具 | 版本 |
|------|------|
| JDK | 21+ |
| Maven | 3.9+ |
| Node.js | 18+ |
| PostgreSQL | 17 |
| Redis | 6+ |
| RocketMQ | 5.3 |
| Meilisearch | 1.12+ (全文检索) |
| MinIO | 最新 (文件存储) |

### 11.2 启动依赖服务

```bash
docker-compose up -d postgres redis rocketmq-namesrv rocketmq-broker

# 全文检索（如需要）
docker run -d -p 7700:7700 getmeili/meilisearch

# 文件存储（如需要）
docker run -d -p 9000:9000 -p 9001:9001 minio/minio
```

### 11.3 环境变量

| 变量 | 必须 | 说明 |
|------|------|------|
| `POSTGRES_PASSWORD` | ✅ | PostgreSQL 密码 |
| `JWT_SECRET` | ✅ | JWT 签名密钥 |
| `ENCRYPT_AES_KEY` | ✅ | AES 加密密钥 |
| `DASHSCOPE_API_KEY` | ⭐ | 通义千问 API Key |
| `OPENAI_API_KEY` | ⭐ | OpenAI API Key |
| `DEEPSEEK_API_KEY` | ⭐ | DeepSeek API Key |
| `MINIMAX_API_KEY` | ⭐ | MiniMax API Key |
| `REDIS_PASSWORD` | ❌ | Redis 密码 |
| `ROCKETMQ_NAMESERVER` | ❌ | RocketMQ 地址 |

