# 可视化数据采集流程编排 实现方案

> **For agentic workers:** REQUIRED SUB-SKILL: Use compose:subagent (recommended) or compose:execute to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将现有 ETL 模块替换为基于 Vue Flow 的可视化画布流程编排平台，支持拖拽式构建数据采集管道（读取源 → 转换处理 → 写入目标），后端使用 Spring Batch 执行。

**Architecture:** 前端使用 Vue Flow 实现节点拖拽画布，每个节点类型（数据源/转换/目标）映射到 Spring Batch 的 Reader/Processor/Writer。流程定义以 JSON 存储，执行时动态构建 Spring Batch Job。监控面板实时展示执行状态和数据量。

**Tech Stack:** Vue 3 + Vue Flow + Element Plus | Spring Boot 3 + Spring Batch + MyBatis Plus + PostgreSQL

---

## 一、架构设计

### 1.1 核心概念

```
Flow（流程）
  ├── Node（节点）：数据源节点 / 转换节点 / 目标节点
  ├── Edge（连线）：节点间的数据流向
  └── Config（配置）：每个节点的参数配置

执行映射：
  数据源节点 → Spring Batch ItemReader（从各种数据源读取）
  转换节点   → Spring Batch ItemProcessor（字段映射/过滤/转换）
  目标节点   → Spring Batch ItemWriter（写入各种数据源）
```

### 1.2 数据模型

```
etl_flow（流程定义表）
├── id, flow_name, flow_code, description
├── nodes_json（JSON: 节点列表）
├── edges_json（JSON: 连线列表）
├── status（DRAFT/RUNNING/COMPLETED/FAILED）
├── schedule_type, cron_expression, interval_seconds
└── create_time, update_time, create_by, update_by, deleted

etl_flow_execution（执行记录表）
├── id, flow_id, exec_status（RUNNING/SUCCESS/FAILED/STOPPED）
├── start_time, end_time
├── read_count, write_count, skip_count, error_count
├── error_message, exec_detail（JSON: 各节点执行详情）
└── create_time

etl_node_config（节点配置表，冗余存储便于查询）
├── id, flow_id, node_id（前端生成的节点ID）
├── node_type（SOURCE/TRANSFORM/TARGET）
├── node_sub_type（mysql/csv/ftp/filter/map 等）
├── config_json（节点参数配置）
└── position_x, position_y（画布坐标）
```

### 1.3 节点类型体系

| 节点分类 | 子类型 | 说明 |
|---------|--------|------|
| **SOURCE（读取源）** | mysql, postgresql, oracle, csv, excel, ftp, sftp, hive, json | 从各种数据源读取数据 |
| **TRANSFORM（转换）** | filter, map, merge, split, dedupe, aggregate, custom_sql | 数据转换处理 |
| **TARGET（写入目标）** | mysql, postgresql, oracle, csv, excel, ftp, sftp, hive, json | 写入各种目标数据源 |

### 1.4 前端画布布局

```
┌──────────────────────────────────────────────────────┐
│  工具栏：保存 | 执行 | 停止 | 调度配置 | 监控          │
├──────────┬──────────────────────────┬────────────────┤
│ 节点面板  │                          │  属性面板      │
│          │     Vue Flow 画布         │  (选中节点的    │
│ 数据源:  │                          │   配置表单)    │
│  ○ MySQL │    [Source] ──→ [Transform] ──→ [Target]  │
│  ○ CSV   │                          │               │
│  ○ FTP   │                          │               │
│ 转换:    │                          │               │
│  ○ Filter│                          │               │
│  ○ Map   │                          │               │
│ 目标:    │                          │               │
│  ○ MySQL │                          │               │
│  ○ CSV   │                          │               │
└──────────┴──────────────────────────┴────────────────┘
```

---

## 二、任务分解

### Phase 1: 数据模型与后端基础（Task 1-3）

#### Task 1: 数据库表结构

**Files:**
- Create: `easy-lowcode-etl/src/main/resources/db/changelog/etl-flow-tables.xml`

**内容:** 创建 `etl_flow`、`etl_flow_execution`、`etl_node_config` 三张表，通过 Liquibase 管理。

#### Task 2: 流程实体类与 Mapper

**Files:**
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/entity/EtlFlow.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/entity/EtlFlowExecution.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/entity/EtlNodeConfig.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/mapper/EtlFlowMapper.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/mapper/EtlFlowExecutionMapper.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/mapper/EtlNodeConfigMapper.java`

#### Task 3: 流程 CRUD 服务与控制器

**Files:**
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/EtlFlowService.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/impl/EtlFlowServiceImpl.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/controller/EtlFlowController.java`

**接口:** CRUD + 保存画布（nodes/edges JSON）+ 获取流程详情

---

### Phase 2: Spring Batch 执行引擎（Task 4-7）

#### Task 4: 节点执行器接口与注册中心

**Files:**
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/NodeExecutor.java`（接口）
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/NodeExecutorRegistry.java`（注册中心）
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/FlowExecutionEngine.java`（流程执行引擎）

**接口定义:**
```java
public interface NodeExecutor {
    String getNodeType();      // "mysql", "csv", "filter" 等
    String getCategory();      // "SOURCE", "TRANSFORM", "TARGET"
    ItemReader<?> createReader(Map<String, Object> config);
    ItemProcessor<?, ?> createProcessor(Map<String, Object> config);
    ItemWriter<?> createWriter(Map<String, Object> config);
}
```

#### Task 5: 数据源节点执行器（SOURCE）

**Files:**
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/MysqlSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/PostgresqlSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/OracleSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/CsvSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/ExcelSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/FtpSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/SftpSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/HiveSourceExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/source/JsonSourceExecutor.java`

#### Task 6: 转换节点执行器（TRANSFORM）

**Files:**
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/FilterProcessorExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/MapProcessorExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/MergeProcessorExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/SplitProcessorExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/DedupeProcessorExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/AggregateProcessorExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/transform/CustomSqlProcessorExecutor.java`

#### Task 7: 目标节点执行器（TARGET）

**Files:**
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/MysqlTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/PostgresqlTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/OracleTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/CsvTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/ExcelTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/FtpTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/SftpTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/HiveTargetExecutor.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/engine/target/JsonTargetExecutor.java`

---

### Phase 3: 前端画布（Task 8-11）

#### Task 8: 安装依赖与基础组件

**Files:**
- Modify: `easy-lowcode-frontend/package.json`（添加 @vue-flow/core, @vue-flow/background, @vue-flow/controls, @vue-flow/minimap）
- Create: `easy-lowcode-frontend/src/views/etl/components/FlowCanvas.vue`（主画布组件）
- Create: `easy-lowcode-frontend/src/views/etl/components/NodePalette.vue`（左侧节点面板）
- Create: `easy-lowcode-frontend/src/views/etl/components/PropertyPanel.vue`（右侧属性面板）
- Create: `easy-lowcode-frontend/src/views/etl/components/FlowToolbar.vue`（顶部工具栏）

#### Task 9: 自定义节点组件

**Files:**
- Create: `easy-lowcode-frontend/src/views/etl/components/nodes/SourceNode.vue`（数据源节点）
- Create: `easy-lowcode-frontend/src/views/etl/components/nodes/TransformNode.vue`（转换节点）
- Create: `easy-lowcode-frontend/src/views/etl/components/nodes/TargetNode.vue`（目标节点）
- Create: `easy-lowcode-frontend/src/views/etl/components/nodes/nodeTypes.ts`（节点类型注册）

#### Task 10: 节点配置表单

**Files:**
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/DatabaseSourceConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/FileSourceConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/FtpSourceConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/HiveSourceConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/FilterConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/MapConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/AggregateConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/DatabaseTargetConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/FileTargetConfig.vue`
- Create: `easy-lowcode-frontend/src/views/etl/components/forms/FtpTargetConfig.vue`

#### Task 11: 流程管理页面与 API

**Files:**
- Create: `easy-lowcode-frontend/src/views/etl/FlowManagement.vue`（流程列表页）
- Create: `easy-lowcode-frontend/src/views/etl/FlowDesigner.vue`（画布设计页）
- Create: `easy-lowcode-frontend/src/api/flow.ts`（流程 API）
- Create: `easy-lowcode-frontend/src/types/flow.ts`（流程类型定义）

---

### Phase 4: 监控与调度（Task 12-13）

#### Task 12: 执行监控

**Files:**
- Create: `easy-lowcode-frontend/src/views/etl/FlowMonitor.vue`（执行监控面板）
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/FlowExecutionService.java`
- Create: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/impl/FlowExecutionServiceImpl.java`

**功能:** 实时查看执行状态、数据量统计、错误日志、节点执行详情

#### Task 13: 调度集成

**Files:**
- Modify: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/service/impl/ScheduleServiceImpl.java`（扩展支持流程调度）
- Modify: `easy-lowcode-etl/src/main/java/com/dabai/easy_lowcode/etl/controller/EtlFlowController.java`（添加调度接口）

**功能:** 支持 CRON/INTERVAL 调度，定时执行流程

---

### Phase 5: 数据源管理集成（Task 14）

#### Task 14: 数据源管理与流程画布联动

**Files:**
- Modify: `easy-lowcode-frontend/src/views/etl/components/forms/DatabaseSourceConfig.vue`（下拉选择已有数据源）
- Modify: `easy-lowcode-frontend/src/views/etl/components/forms/DatabaseTargetConfig.vue`（下拉选择已有数据源）
- Modify: `easy-lowcode-frontend/src/views/etl/FlowDesigner.vue`（集成数据源选择器）

**功能:** 节点配置时可直接选择已注册的数据源，自动填充连接信息

---

## 三、执行顺序

```
Phase 1 (基础)    → Phase 2 (引擎) → Phase 3 (画布) → Phase 4 (监控) → Phase 5 (集成)
Task 1-3          → Task 4-7       → Task 8-11      → Task 12-13     → Task 14
```

Phase 1-2 为后端基础，可并行开发。Phase 3 为前端画布，依赖 Phase 1 的 API。Phase 4-5 为增强功能。

---

## 四、验证方式

1. **Phase 1 验证:** 启动后端，通过 Swagger 调用流程 CRUD API
2. **Phase 2 验证:** 单元测试各 NodeExecutor 的 createReader/Processor/Writer
3. **Phase 3 验证:** 启动前端，拖拽节点构建流程，保存到后端
4. **Phase 4 验证:** 执行一个简单流程（CSV → MySQL），查看监控面板
5. **Phase 5 验证:** 在画布中选择已有数据源，自动填充配置
