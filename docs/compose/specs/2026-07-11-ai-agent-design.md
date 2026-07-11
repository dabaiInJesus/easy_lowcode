# AI 智能体（Agent）设计文档

## [S1] 问题与目标

当前 AI 模块只有简单的对话功能，无法执行工具调用、检索知识库或自主规划。需要构建类似 Hermes Agent 的智能体能力。

**目标**：构建完整的 AI 智能体系统，支持工具调用、多轮对话+记忆、知识库检索（RAG）、自主规划执行。

## [S2] 架构设计

### 2.1 核心概念

```
Agent（智能体）
  ├── System Prompt：系统提示词（定义 Agent 角色和能力）
  ├── Tools（工具集）：Agent 可调用的工具列表
  ├── Knowledge（知识库）：Agent 可检索的文档
  ├── Memory（记忆）：对话历史 + 长期记忆
  └── Planning（规划）：自主规划执行步骤

执行循环（ReAct 模式）：
  用户输入 → LLM 思考 → 决定是否调用工具 → 执行工具 → 将结果反馈给 LLM → LLM 继续思考或返回最终答案
```

### 2.2 工具系统

```java
// 工具接口
public interface AgentTool {
    String getName();           // 工具名称
    String getDescription();    // 工具描述（给 LLM 看）
    Map<String, Object> getParametersSchema();  // 参数 Schema
    Object execute(Map<String, Object> params); // 执行工具
}

// 内置工具
- WebSearchTool: 网络搜索
- CalculatorTool: 数学计算
- DateTimeTool: 日期时间操作
- HttpRequestTool: HTTP API 调用
- DatabaseQueryTool: 数据库查询
- WorkflowTool: 调用已发布的工作流
```

### 2.3 知识库（RAG）

```
文档上传 → 文档解析（Tika）→ 文本分块 → 向量化 → 存储到向量数据库
用户提问 → 向量化 → 相似度搜索 → 获取相关文档片段 → 作为上下文注入 Prompt
```

### 2.4 记忆系统

```
短期记忆：当前会话的对话历史（Redis + 降级内存）
长期记忆：跨会话的重要信息摘要（DB 存储）
工作记忆：Agent 当前任务的中间状态（内存）
```

### 2.5 自主规划

```
ReAct 循环：
1. LLM 接收用户输入 + 系统提示 + 工具描述
2. LLM 输出思考过程（Thought）和行动决策（Action）
3. 如果是工具调用 → 执行工具 → 将结果反馈
4. 如果是最终回答 → 返回给用户
5. 重复直到完成或达到最大轮次
```

### 2.6 数据模型扩展

```sql
-- 扩展 ai_agent 表
ALTER TABLE ai_agent ADD COLUMN tools_config TEXT;        -- 工具配置JSON
ALTER TABLE ai_agent ADD COLUMN knowledge_ids TEXT;       -- 关联知识库ID列表
ALTER TABLE ai_agent ADD COLUMN max_iterations INT DEFAULT 10;  -- 最大执行轮次
ALTER TABLE ai_agent ADD COLUMN enable_planning INT DEFAULT 0;  -- 是否启用自主规划

-- 新增：Agent 工具表
CREATE TABLE ai_agent_tool (
    id BIGINT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_type VARCHAR(50) NOT NULL,   -- web_search/calculator/http/database/workflow/custom
    config_json TEXT,                  -- 工具配置
    enabled INT DEFAULT 1,
    sort_order INT DEFAULT 0
);

-- 新增：Agent 知识库表
CREATE TABLE ai_agent_knowledge (
    id BIGINT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    knowledge_name VARCHAR(100) NOT NULL,
    knowledge_type VARCHAR(50),        -- document/faq/webpage
    content TEXT,
    embedding JSON,                    -- 向量（可选）
    metadata JSON,
    status INT DEFAULT 1
);

-- 新增：Agent 执行日志表
CREATE TABLE ai_agent_execution_log (
    id BIGINT PRIMARY KEY,
    agent_id BIGINT NOT NULL,
    session_id VARCHAR(100),
    role VARCHAR(20),                  -- user/assistant/tool/system
    content TEXT,
    tool_name VARCHAR(100),
    tool_input TEXT,
    tool_output TEXT,
    iteration INT DEFAULT 0,
    token_usage INT DEFAULT 0,
    create_time TIMESTAMP
);
```

### 2.7 前端架构

```
Agent 管理页面
├── Agent 列表（创建/编辑/删除/发布）
├── Agent 编辑器
│   ├── 基本信息（名称/描述/头像）
│   ├── 模型配置（Provider/Model/Temperature）
│   ├── 系统提示词编辑
│   ├── 工具配置（选择启用的工具）
│   ├── 知识库管理（上传文档/查看片段）
│   └── 高级配置（最大轮次/是否启用规划）
└── Agent 对话测试
    ├── 多轮对话界面
    ├── 工具调用过程可视化
    └── 知识库检索结果展示
```

## [S3] 技术选型

- **工具调用**：Spring AI 的 Function Calling 机制
- **知识库检索**：复用现有 Meilisearch 全文检索
- **记忆系统**：复用现有 SessionManager（Redis + 内存）
- **自主规划**：ReAct 循环（LLM + 工具调用迭代）
- **前端对话**：SSE 流式输出 + 工具调用状态展示

## [S4] API 设计

```
POST   /api/ai/agent                 创建Agent
PUT    /api/ai/agent                 更新Agent
GET    /api/ai/agent/{id}            获取Agent详情
DELETE /api/ai/agent/{id}            删除Agent
GET    /api/ai/agent/page            分页查询
POST   /api/ai/agent/{id}/publish    发布Agent

POST   /api/ai/agent/{id}/chat       对话（SSE流式）
GET    /api/ai/agent/{id}/history    获取对话历史
DELETE /api/ai/agent/{id}/session    清除会话

GET    /api/ai/agent/tools           获取可用工具列表
POST   /api/ai/agent/{id}/tools      配置Agent工具
POST   /api/ai/agent/{id}/knowledge  上传知识库文档
GET    /api/ai/agent/{id}/knowledge  获取知识库列表
DELETE /api/ai/agent/{id}/knowledge/{kid}  删除知识库文档
```
