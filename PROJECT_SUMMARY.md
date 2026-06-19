# 低代码平台项目总结

## 项目概述

基于 Spring Boot 3 + Vue 3 的前后端分离低代码开发平台，支持多数据源对接配置及展示、接口对接配置及展示、ETL接入配置、数据大屏配置展示和AI智能集成。

## 技术栈

| 层级 | 技术 | 版本 |
|---|---|---|
| 核心框架 | Spring Boot | 3.5.5 |
| ORM框架 | MyBatis Plus | 3.5.5 |
| 权限认证 | Sa-Token | 1.37.0 |
| 工作流引擎 | Flowable | 7.0.1 |
| AI集成 | Spring AI / DashScope / DeepSeek / Ollama | 1.1.4 |
| 数据库 | PostgreSQL 17（+ MySQL/Oracle/SQL Server/达梦等12种） | - |
| 消息队列 | RocketMQ | 5.3.0 |
| 批处理 | Spring Batch | - |
| 前端 | Vue 3 + Vite + Element Plus + TypeScript | - |
| 图表 | ECharts | 5.x |
| 构建工具 | Maven | 3.6+ |
| 数据库版本管理 | Liquibase | 4.25.1 |

## 模块架构

```
easy-lowcode-common      ← 公共模块（工具类、异常、统一返回、拦截器、线程池）
    ↑
easy-lowcode-database    ← 数据库模块（通用实体、Mapper、MyBatis配置、Liquibase）
    ↑
    ├── easy-lowcode-auth        ← 认证授权（用户/角色/菜单/部门CRUD，Sa-Token）
    ├── easy-lowcode-collector   ← 数据采集（多数据源配置/测试/扫描，表资源注册，API管理）
    ├── easy-lowcode-resource    ← 资源查询（动态数据查询，字段白名单安全加固）
    ├── easy-lowcode-etl         ← ETL配置（源→目标数据抽取/转换/加载，异步执行）
    ├── easy-lowcode-dashboard   ← 数据大屏（大屏/图表CRUD，设计器，实时数据查询）
    └── easy-lowcode-ai          ← AI集成（多供应商对话，Agent，SSE流式，AI配置管理）
    ↑
easy-lowcode-startup     ← 启动模块（整合所有模块，Liquibase入口）
easy-lowcode-gateway     ← 网关模块（独立部署，认证+日志过滤）
easy-lowcode-frontend    ← Vue 3前端（TypeScript，Element Plus，ECharts）
```

## ✅ 已完成的功能清单

### 1. 多数据源对接配置及展示
- **支持12种数据库**：MySQL、PostgreSQL、Oracle、SQL Server、达梦DM、人大金仓Kingbase、南大通用GBase、OceanBase、TiDB、openGauss、华为GaussDB、瀚高HighGo
- **数据源管理**：CRUD、密码AES加密、测试连接、扫描表列表、获取表字段结构
- **前端页面**：完整CRUD + 搜索 + 分页 + 测试连接

### 2. 接口对接配置及展示
- **表资源注册**：三步向导（选数据源→选表→配置接口），字段级精确/模糊查询配置
- **API管理**：表资源API自动生成，外部接口手动注册（支持Query Params/Headers/Body详细配置）
- **动态数据查询**：按资源编码/ID查询，支持分页、排序、条件过滤（eq/like/gt/gte/lt/lte/in）
- **安全加固**：字段白名单校验，防止SQL注入
- **API限流**：基于数据库配置的 rateLimit 字段，固定窗口限流策略

### 3. ETL接入配置
- **ETL任务管理**：三步向导式任务配置（基本信息→字段映射→调度配置）
- **源数据读取**：支持全表读取和自定义SQL两种模式
- **目标写入**：支持INSERT追加、MERGE合并、TRUNCATE清空后插入三种模式
- **字段映射**：源→目标字段映射配置，支持函数转换和表达式转换
- **任务调度**：手动执行、CRON定时、间隔执行三种调度方式
- **异步执行**：基于@Async线程池，不阻塞主线程
- **执行监控**：记录每次执行的日志（状态、读写计数、错误信息）

### 4. 数据大屏配置展示
- **大屏管理**：卡片式列表，支持新建/编辑/发布/复制/下线
- **图表配置**：16种图表类型（bar/line/pie/scatter/map/radar/gauge/text/number/table/iframe等）
- **数据源绑定**：支持数据源+SQL直接查询、表资源、API三种模式
- **ECharts渲染**：柱状图/折线图/饼图/散点图自动渲染，支持多Y轴、分组、自定义option合并
- **设计器**：侧栏面板+12列网格画布，所见即所得的图表预览
- **全屏展示**：数字/表格/文本/ECharts图表渲染，自动刷新，全屏模式
- **自适应**：ResizeObserver + window.resize 双重自适应

### 5. AI集成
- **多供应商**：OpenAI、DashScope(通义千问)、DeepSeek、Ollama、Minimax
- **对话接口**：POST /api/ai/chat、/api/ai/chat/{provider}、/api/ai/simple-chat
- **流式聊天**（已实现）：SSE (Server-Sent Events) 端点 POST /api/ai/chat/stream
- **AI Agent**（进行中）：执行Agent任务、列表、自定义创建
- **AI配置管理**：数据库表存储供应商配置，支持前端UI管理

### 6. 前端基础架构
- Vue 3 + Vite + TypeScript + Element Plus + TailwindCSS
- 登录/登出、路由守卫、布局（侧边栏+顶栏+内容区）
- Pinia 状态管理、Axios 请求封装
- 用户管理、角色管理、菜单管理、部门管理、授权管理、应用管理

### 7. 基础设施
- Docker Compose：PostgreSQL 17 + pgvector + Redis + RocketMQ
- Liquibase：17个数据库表变更集，覆盖所有模块
- Actuator 监控端点
- 异步线程池（通用 + ETL专用）

## 📊 项目统计

| 指标 | 数值 |
|------|------|
| Maven模块 | 10个 |
| Java文件 | 80+个 |
| Vue/TS文件 | 20+个 |
| 代码行数 | ~15,000行 |
| 数据库表 | 17+张 |
| REST API端点 | 80+个 |
| 测试用例 | 30+个 |
| Liquibase变更集 | 17+ 个 |

## 🗺️ API 接口总览

### 数据源管理：`/api/collector/datasource`
- `GET /page` 分页查询 | `GET /{id}` 详情
- `POST /` 创建 | `PUT /` 更新 | `DELETE /{id}` 删除
- `POST /test-connection` 测试连接
- `GET /{id}/tables` 扫描表 | `GET /{id}/table/{name}/columns` 获取列结构

### 表资源管理：`/api/collector/table-resource`
- `GET /page` 分页查询 | `GET /{id}` 详情
- `POST /` 注册 | `PUT /` 更新 | `DELETE /{id}` 删除
- `POST /{id}/generate-api` 生成API
- `GET /{id}/preview` 数据预览

### API管理：`/api/collector/api-management`
- `GET /page` 分页查询 | `GET /{id}` 详情
- `POST /register-external` 注册外部接口
- `PUT /{id}` 更新 | `DELETE /{id}` 删除
- `PUT /{id}/status` 启用/禁用
- `DELETE /batch` 批量删除

### 动态数据查询：`/api/resource`
- `GET /data/{resourceCode}` 按编码查询（支持条件过滤）

### ETL任务：`/api/etl/task`
- `GET /page` 分页查询 | `GET /{id}` 详情
- `POST /` 创建 | `PUT /` 更新 | `DELETE /{id}` 删除
- `POST /{id}/execute` 执行 | `POST /{id}/stop` 停止
- `GET /{id}/history` 执行历史
- `GET /{id}/source-columns` 源表字段 | `GET /{id}/target-columns` 目标表字段
- `GET /{id}/preview` 预览源数据
- `GET /datasources` 数据源列表

### 数据大屏：`/api/dashboard`
- `GET /page` 分页查询 | `GET /list` 列表 | `GET /{id}` 详情
- `POST /` 创建 | `PUT /` 更新 | `DELETE /{id}` 删除
- `POST /{id}/publish` 发布 | `POST /{id}/copy` 复制 | `POST /{id}/offline` 下线
- `GET /{dashboardId}/charts` 图表列表
- `POST /chart` 添加 | `PUT /chart` 更新 | `DELETE /chart/{id}` 删除
- `PUT /charts/positions` 批量更新位置
- `GET /chart/{id}/data` 查询图表数据
- `GET /{id}/preview` 大屏预览

### AI：`/api/ai`
- `POST /chat` 聊天 | `POST /chat/{provider}` 指定供应商 | `POST /chat/stream` SSE流式
- `POST /simple-chat` 简单文本
- `GET /providers` 供应商列表
- `POST /agent/execute` 执行Agent | `GET /agent/list` Agent列表 | `POST /agent/create` 创建Agent
- `GET /config/page` 配置列表 | `POST /config` 保存 | `PUT /config` 更新 | `DELETE /config/{id}` 删除 | `GET /config/list` 启用列表

## 🔧 技术要点

1. **安全防护**
   - 字段白名单：所有动态查询的参数和排序字段经过白名单校验
   - 密码加密存储：AES加密数据库密码和AI API Key
   - API限流：基于数据库配置的固定窗口限流

2. **性能优化**
   - 异步线程池：ETL任务使用专用线程池，隔离资源
   - 字段白名单缓存：ConcurrentHashMap缓存表结构，减少扫描开销

3. **可扩展性**
   - 工厂模式：AiServiceFactory 支持动态添加AI供应商
   - 数据库类型模板模式：12种数据库的SQL方言统一管理
   - Liquibase 数据库版本管理：17个变更集可追溯

## ⏳ 后续规划

1. **ECharts按需导入**：优化打包体积（目前全量导入约1.1MB）
2. **大屏内置模板**：基于 isBuiltin 字段的内置大屏模板
3. **ETL Cron调度**：结合Spring Schedule实现定时执行
4. **数据导出**：动态查询结果导出Excel/CSV
5. **国际化**：i18n多语言支持
6. **更多图表类型**：地图、雷达图、仪表盘等
