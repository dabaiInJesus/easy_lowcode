# AI 工作流引擎设计文档

## [S1] 问题与目标

当前 AI 模块只有简单的对话功能，无法构建复杂的 AI 处理流程。用户需要一个类似 Dify 的可视化工作流编排能力，通过拖拽节点构建 AI 处理管道。

**目标**：构建完整的 AI 工作流引擎，支持拖拽式画布编排、多种节点类型、实时执行与可视化。

## [S2] 架构设计

### 2.1 核心概念

```
Workflow（工作流）
  ├── Node（节点）：开始 / LLM / 代码 / HTTP / 条件 / 变量 / 结束
  ├── Edge（连线）：节点间的数据流向
  └── Variable（变量）：节点间传递的数据

执行模型：
  开始节点 → 按拓扑顺序执行各节点 → 每个节点接收上游输出作为输入 → 结束节点输出最终结果
```

### 2.2 节点类型

| 节点类型 | 说明 | 输入 | 输出 |
|---------|------|------|------|
| **Start** | 流程入口，定义输入变量 | 用户输入 | 变量列表 |
| **LLM** | 调用 AI 模型 | prompt 模板 + 变量 | model 输出 |
| **Code** | 执行 Python/JS 代码 | 变量列表 | 代码返回值 |
| **HTTP** | 调用外部 API | URL + 方法 + 参数 | 响应数据 |
| **Condition** | 条件分支 | 条件表达式 | true/false 分支 |
| **Variable** | 变量赋值/转换 | 源变量 + 转换规则 | 新变量值 |
| **End** | 流程出口，定义输出变量 | 最终变量 | 最终结果 |

### 2.3 数据模型

```sql
ai_workflow（工作流定义表）
├── id, workflow_name, workflow_code, description
├── nodes_json（JSON: 节点列表）
├── edges_json（JSON: 连线列表）
├── variables_json（JSON: 全局变量定义）
├── status（DRAFT/PUBLISHED）
├── create_time, update_time, deleted

ai_workflow_execution（执行记录表）
├── id, workflow_id, exec_status（RUNNING/SUCCESS/FAILED）
├── start_time, end_time
├── input_variables（JSON: 输入变量）
├── output_variables（JSON: 输出变量）
├── node_executions（JSON: 各节点执行详情）
├── error_message

ai_workflow_node_execution（节点执行记录表）
├── id, execution_id, node_id, node_type, node_name
├── exec_status, start_time, end_time
├── input_data（JSON）, output_data（JSON）
├── error_message, token_usage
```

### 2.4 前端架构

```
┌──────────────────────────────────────────────────────┐
│  工具栏：保存 | 发布 | 执行 | 版本历史                  │
├──────────┬──────────────────────────┬────────────────┤
│ 节点面板  │                          │  属性面板      │
│          │    Vue Flow 画布          │  (节点配置)    │
│ 开始: ●  │                          │                │
│ LLM: 🤖  │  [Start]→[LLM]→[End]   │  (变量配置)    │
│ 代码: ⌨️ │                          │                │
│ HTTP: 🌐 │                          │                │
│ 条件: ◇  │                          │                │
│ 变量: 📝 │                          │                │
│ 结束: ◉  │                          │                │
└──────────┴──────────────────────────┴────────────────┘
```

### 2.5 执行引擎

```java
// 后端执行流程
1. 解析工作流 JSON（nodes + edges）
2. 拓扑排序确定执行顺序
3. 从 Start 节点开始，按顺序执行每个节点
4. 每个节点接收上游输出，执行后产生输出
5. 通过 SSE 实时推送执行状态给前端
6. 前端实时更新节点状态（等待→执行中→完成/失败）
```

### 2.6 SSE 实时推送

```
前端订阅 → /api/ai/workflow/execute/{id}/stream
后端推送 →
  { type: "node_start", nodeId: "xxx", timestamp: ... }
  { type: "node_output", nodeId: "xxx", data: "..." }
  { type: "node_complete", nodeId: "xxx", status: "success" }
  { type: "workflow_complete", output: {...} }
```

## [S3] 技术选型

- **前端画布**：Vue Flow（与数据采集流程共用）
- **后端执行**：自研轻量引擎（非 Spring Batch，工作流是图执行不是批处理）
- **实时通信**：SSE（Server-Sent Events）
- **AI 调用**：复用现有 AiServiceFactory
- **代码执行**：ScriptEngine（Nashorn/GraalJS）或限制性沙箱

## [S4] API 设计

```
POST   /api/ai/workflow              创建工作流
PUT    /api/ai/workflow              更新工作流（含节点/连线）
GET    /api/ai/workflow/{id}         获取工作流详情
DELETE /api/ai/workflow/{id}         删除工作流
GET    /api/ai/workflow/page         分页查询
POST   /api/ai/workflow/{id}/publish 发布工作流
POST   /api/ai/workflow/{id}/execute 执行工作流（SSE流式返回）
GET    /api/ai/workflow/{id}/history 执行历史
```
