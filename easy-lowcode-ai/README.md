# Easy Lowcode AI 模块

本模块集成了 Spring AI Alibaba 1.1.2.0，支持多种 AI 模型提供商和 Agent 功能。

## 项目结构

```
easy-lowcode-ai/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dabai/easy_lowcode/ai/
│   │   │       ├── config/          # 配置类
│   │   │       ├── controller/      # REST API 控制器
│   │   │       ├── dto/            # 数据传输对象
│   │   │       ├── entity/         # 实体类
│   │   │       ├── enums/          # 枚举类型
│   │   │       ├── factory/        # 工厂类
│   │   │       ├── mapper/         # MyBatis Mapper
│   │   │       └── service/        # 服务层
│   │   └── resources/              # 资源文件
│   │       ├── application.yaml           # 主配置文件
│   │       ├── application-dev.yaml       # 开发环境配置
│   │       ├── application-prod.yaml      # 生产环境配置
│   │       ├── .env.example               # 环境变量示例
│   │       └── logback-spring.xml         # 日志配置
│   └── test/
│       ├── java/                   # 测试代码
│       └── resources/              # 测试资源
│           └── application-test.yaml      # 测试环境配置
├── pom.xml                         # Maven 配置
├── README.md                       # 项目说明
├── QUICK_START.md                  # 快速开始指南
├── CONFIG_EXAMPLE.yaml             # 配置示例
└── INTEGRATION_SUMMARY.md          # 集成说明
```

## 支持的 AI 提供商

- **OpenAI**: GPT-3.5, GPT-4
- **阿里云通义千问 (DashScope)**: qwen-turbo, qwen-plus, qwen-max
- **百度文心一言**: eb-instant, ernie-bot
- **腾讯混元**: hunyuan
- **智谱清言**: glm
- **Moonshot (Kimi)**: moonshot
- **Ollama**: 本地运行的开源模型（llama2, mistral, codellama, qwen 等）
- **DeepSeek**: deepseek-chat, deepseek-coder
- **Minimax**: abab6-chat 及其他模型

## Spring AI Alibaba Agent 功能

本模块集成了 Spring AI Alibaba 的 Agent 功能，提供：

- **Agent 执行器**: 支持复杂的任务规划和执行
- **记忆功能**: 维护对话上下文
- **工具调用**: 可以集成外部工具和 API
- **自定义 Agent**: 支持创建和注册自定义 Agent

## 快速开始

### 1. 配置 AI 提供商

在 `application.yaml` 中配置您要使用的 AI 提供商：

```yaml
ai:
  provider:
    default: ollama  # 选择默认提供商
  
  # Ollama 配置（本地运行，无需 API Key）
  ollama:
    base-url: http://localhost:11434
    model: llama2
  
  # DeepSeek 配置
  deepseek:
    api-key: your-api-key
    model: deepseek-chat
  
  # Minimax 配置
  minimax:
    api-key: your-api-key
    model: abab6-chat
```

### 2. 使用 Ollama（推荐用于开发）

Ollama 允许您在本地运行大型语言模型，无需 API Key：

```bash
# 安装 Ollama
# 访问 https://ollama.ai/ 下载安装

# 拉取模型
ollama pull llama2

# 启动 Ollama 服务
ollama serve

# 配置应用使用 Ollama
# 在 application.yaml 中设置:
# ai.provider.default=ollama
```

### 3. API 使用示例

#### 聊天接口

```bash
# 发送聊天请求
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己",
    "systemPrompt": "你是一个有用的AI助手",
    "temperature": 0.7,
    "maxTokens": 1000
  }'
```

#### Agent 接口

```bash
# 获取可用的 Agent 列表
curl http://localhost:8080/api/ai/agent/list

# 执行 Agent 任务
curl -X POST http://localhost:8080/api/ai/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "SampleAgent",
    "task": "请帮我分析这段代码的优缺点"
  }'

# 创建自定义 Agent
curl -X POST http://localhost:8080/api/ai/agent/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CodeReviewer",
    "description": "代码审查助手",
    "instructions": "你是一个专业的代码审查员，请仔细分析代码并提供改进建议"
  }'
```

## 架构设计

### 核心组件

1. **AiService**: 统一的 AI 服务接口
   - OpenAiServiceImpl
   - DashScopeServiceImpl
   - OllamaServiceImpl
   - DeepSeekServiceImpl
   - MinimaxServiceImpl

2. **AiServiceFactory**: AI 服务工厂，根据提供商动态选择服务实现

3. **AiAgentService**: Agent 管理服务
   - 执行 Agent 任务
   - 管理 Agent 生命周期
   - 提供 Agent 列表

4. **AiAlibabaAgentConfig**: Spring AI Alibaba Agent 配置
   - 初始化 Agent Executor
   - 创建示例 Agent

### 工作流程

```
用户请求 → AiController → AiServiceFactory → 具体 AiService 实现 → AI 提供商 API
                                      ↓
                              返回统一格式的响应
```

## 配置说明

### 环境变量

可以通过环境变量配置 API Key（推荐用于生产环境）：

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export DEEPSEEK_API_KEY=your-api-key
export MINIMAX_API_KEY=your-api-key
export DASHSCOPE_API_KEY=your-api-key
```

### 模型参数

- **temperature**: 控制输出的随机性（0.0-1.0）
- **maxTokens**: 最大生成 token 数
- **systemPrompt**: 系统提示词，定义 AI 的行为
- **model**: 指定使用的模型名称

## 开发指南

### 添加新的 AI 提供商

1. 在 `AiProvider` 枚举中添加新提供商
2. 在 `AiProperties` 中添加配置类
3. 创建新的 Service 实现类
4. 在 `AiServiceFactory` 中添加路由逻辑
5. 更新配置文件

### 自定义 Agent

```java
@Component
public class CustomAgent {
    
    @Autowired
    private AgentExecutor agentExecutor;
    
    public String executeTask(String task) {
        // 执行任务
        Object result = agentExecutor.execute(task);
        return result.toString();
    }
}
```

## 依赖说明

本项目使用以下主要依赖：

- **spring-ai-alibaba-agent-framework**: 1.1.2.0
- **spring-ai-alibaba-starter-dashscope**: 1.1.2.0
- **spring-ai-starter-model-openai**: 1.1.2
- **spring-ai-starter-model-ollama**: 1.1.2
- **spring-ai-client-chat**: 1.1.2

## 注意事项

1. **API Key 安全**: 不要将 API Key 硬编码在代码中，使用环境变量或配置中心
2. **速率限制**: 注意各提供商的 API 调用限制
3. **成本控制**: 监控 API 调用次数和费用
4. **本地模型**: Ollama 需要足够的内存和计算资源
5. **网络要求**: 确保能够访问相应的 API 端点

## 故障排除

### Ollama 连接失败

```bash
# 检查 Ollama 是否运行
ollama list

# 检查服务状态
curl http://localhost:11434/api/tags

# 重启 Ollama 服务
ollama serve
```

### API 调用失败

1. 检查 API Key 是否正确配置
2. 检查网络连接
3. 查看日志获取详细错误信息
4. 确认模型名称是否正确

### Agent 执行失败

1. 确认 Spring AI Alibaba Agent 已启用
2. 检查 Agent Executor 是否正确初始化
3. 查看日志中的异常信息

## 参考资源

- [Spring AI Alibaba 官方文档](https://github.com/alibaba/spring-ai-alibaba)
- [Ollama 官方网站](https://ollama.ai/)
- [DeepSeek 平台](https://platform.deepseek.com/)
- [Minimax API 文档](https://api.minimax.chat/)
- [阿里云通义千问](https://dashscope.aliyun.com/)

## 许可证

本项目采用 MIT 许可证。
