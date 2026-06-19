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
├── CONFIG_EXAMPLE.yaml             # 配置示例
└── INTEGRATION_SUMMARY.md          # 集成说明
```

## 支持的 AI 提供商

目前已实现 5 个 AI 提供商的服务实现：

| 提供商 | 枚举值 | 服务实现类 | 支持模型 |
|--------|--------|-----------|---------|
| **OpenAI** | `OPENAI` | `OpenAiServiceImpl` | GPT-3.5, GPT-4 |
| **阿里云通义千问 (DashScope)** | `DASHSCOPE` | `DashScopeServiceImpl` | qwen-turbo, qwen-plus, qwen-max |
| **Ollama** | `OLLAMA` | `OllamaServiceImpl` | llama2, mistral, codellama, qwen 等 |
| **DeepSeek** | `DEEPSEEK` | `DeepSeekServiceImpl` | deepseek-chat, deepseek-coder |
| **Minimax** | `MINIMAX` | `MinimaxServiceImpl` | abab6-chat 及其他模型 |

`AiProvider` 枚举中还定义了 `WENXIN`（百度文心一言）、`HUNYUAN`（腾讯混元）、`ZHIPU`（智谱清言）、`MOONSHOT`（Moonshot/Kimi）四个枚举值，对应的服务实现可根据需要后续添加。

## Spring AI Alibaba Agent 功能

> **当前状态**: 基础框架已完成，使用 **模拟实现**，功能完善中

本模块预留了 Spring AI Alibaba 的 Agent 功能框架，提供：

- **Agent 执行器**: 支持复杂的任务规划和执行（模拟实现）
- **记忆功能**: 维护对话上下文（待完善）
- **工具调用**: 可以集成外部工具和 API（待完善）
- **自定义 Agent**: 支持创建和注册自定义 Agent（基础框架已完成）

Agent 的完整集成需要等待 Spring AI Alibaba 具体 API 的稳定可用，当前提供基础框架和模拟实现。

## 快速开始

### 1. 安装和配置 Ollama（推荐用于开发）

Ollama 允许您在本地运行大型语言模型，无需 API Key：

```bash
# 访问 https://ollama.ai/ 下载并安装
# 验证安装
ollama --version

# 拉取模型（约 3.8GB）
ollama pull llama2

# 或者使用更小的模型进行测试
ollama pull mistral    # Mistral 7B
ollama pull codellama  # CodeLlama（适合代码任务）

# 启动 Ollama 服务（保持运行）
ollama serve
```

### 2. 配置应用

编辑 `easy-lowcode-startup/src/main/resources/application.yaml`：

```yaml
ai:
  provider:
    default: ollama  # 设置为 ollama

  ollama:
    base-url: http://localhost:11434
    model: llama2  # 或 mistral, codellama 等
```

### 3. 启动应用

```bash
# 在项目根目录执行
mvn spring-boot:run -pl easy-lowcode-startup
```

### 4. 测试聊天功能

```bash
curl -X POST http://localhost:8081/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己",
    "systemPrompt": "你是一个有用的AI助手",
    "temperature": 0.7
  }'
```

### 使用云端 AI 提供商

#### DeepSeek

```bash
# 1. 注册并获取 API Key：https://platform.deepseek.com/
# 2. 配置环境变量
export DEEPSEEK_API_KEY=your-api-key-here
```

```yaml
ai:
  provider:
    default: deepseek
  deepseek:
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
```

#### Minimax

```bash
# 1. 注册并获取 API Key：https://api.minimax.chat/
# 2. 配置环境变量
export MINIMAX_API_KEY=your-api-key-here
```

```yaml
ai:
  provider:
    default: minimax
  minimax:
    api-key: ${MINIMAX_API_KEY}
    model: abab6-chat
```

#### 通义千问（阿里云）

```bash
# 1. 注册并获取 API Key：https://dashscope.aliyun.com/
# 2. 配置环境变量
export DASHSCOPE_API_KEY=your-api-key-here
```

```yaml
ai:
  provider:
    default: dashscope
  dashscope:
    api-key: ${DASHSCOPE_API_KEY}
    model: qwen-turbo
```

## 启动方式

### 方式一：使用启动脚本（推荐）

```bash
# Linux/macOS
chmod +x start.sh
./start.sh          # 默认 dev 环境
./start.sh prod     # 指定环境

# Windows
start.bat
start.bat prod
```

### 方式二：使用 Maven 命令

```bash
# 开发环境（默认 Ollama）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境
export DASHSCOPE_API_KEY=your-api-key
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 方式三：打包后运行

```bash
mvn clean package -DskipTests
java -jar target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --ai.provider.default=dashscope
```

### 验证启动

```bash
# 健康检查
curl http://localhost:8081/api/ai/health

# 服务信息
curl http://localhost:8081/api/ai/info
```

预期响应示例：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "Easy Lowcode AI Module",
    "description": "AI 模块 - 支持多种大模型提供商",
    "version": "1.0.0-SNAPSHOT",
    "supportedProviders": ["openai", "dashscope", "ollama", "deepseek", "minimax"]
  }
}
```

### 切换 AI 提供商

```bash
mvn spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dai.provider.default=openai
```

### 修改端口

```bash
mvn spring-boot:run -Dserver.port=9090
export SERVER_PORT=9090
```

## 环境说明

### 开发环境 (dev)

- 默认使用 Ollama 本地模型，无需 API Key
- DEBUG 级别日志
- 适合本地开发和测试

### 生产环境 (prod)

- 默认使用 DashScope（通义千问）
- 需要配置 API Key
- INFO/WARN 级别日志，日志输出到文件

### 测试环境 (test)

- 使用模拟配置
- 适合单元测试

## Resources 资源配置

模块包含完整的 resources 配置文件体系：

```
easy-lowcode-ai/
├── src/main/resources/
│   ├── application.yaml           # 主配置文件
│   ├── application-dev.yaml       # 开发环境配置
│   ├── application-prod.yaml      # 生产环境配置
│   ├── .env.example               # 环境变量示例
│   └── logback-spring.xml         # 日志配置
└── src/test/resources/
    └── application-test.yaml      # 测试环境配置
```

### 主配置（application.yaml）

- 包含所有 AI 提供商的基础配置
- 支持 OpenAI、DashScope、Ollama、DeepSeek、Minimax
- 使用环境变量管理敏感信息（API Key）

### 开发环境（application-dev.yaml）

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### 生产环境（application-prod.yaml）

```bash
export DASHSCOPE_API_KEY=your-real-api-key
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

### 自定义配置

```bash
# 命令行参数
java -jar easy-lowcode-ai.jar --ai.provider.default=ollama --ai.ollama.model=mistral

# 环境变量
export AI_PROVIDER_DEFAULT=dashscope

# JVM 系统属性
java -Dai.provider.default=openai -DOPENAI_API_KEY=sk-xxx -jar easy-lowcode-ai.jar
```

### 配置优先级

1. 命令行参数（最高）
2. JVM 系统属性
3. 操作系统环境变量
4. `application-{profile}.yaml`
5. `application.yaml`（最低）

### 安全建议

✅ 使用环境变量管理 API Key（`export DASHSCOPE_API_KEY=your-key`）
✅ 使用 `.env` 文件（不提交到 Git）
✅ 定期轮换 API Key
❌ 不要硬编码 API Key
❌ 不要提交 `.env` 文件到版本控制
❌ 不要在日志中打印 API Key

## API 端点

### 聊天 API

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/ai/chat` | 发送聊天消息 |
| GET | `/api/ai/providers` | 获取支持的提供商列表 |
| GET | `/api/ai/health` | 健康检查 |
| GET | `/api/ai/info` | 服务信息 |

### Agent API（模拟实现，进行中）

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/ai/agent/list` | 获取可用 Agent 列表 |
| POST | `/api/ai/agent/execute` | 执行 Agent 任务 |
| POST | `/api/ai/agent/create` | 创建自定义 Agent |

### Agent 接口使用示例

```bash
# 获取可用 Agent 列表
curl http://localhost:8081/api/ai/agent/list

# 执行 Agent 任务
curl -X POST http://localhost:8081/api/ai/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "SampleAgent",
    "task": "请帮我分析这段代码的优缺点"
  }'

# 创建自定义 Agent
curl -X POST http://localhost:8081/api/ai/agent/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CodeReviewer",
    "description": "代码审查助手",
    "instructions": "你是一个专业的代码审查员，请仔细分析代码并提供改进建议"
  }'
```

## 高级配置

### Spring AI Alibaba Agent 配置

```yaml
spring:
  ai:
    alibaba:
      agent:
        enabled: true
        timeout: 300          # 超时时间（秒）
        max-iterations: 10    # 最大迭代次数
        memory-enabled: true  # 启用记忆功能
```

### 切换不同模型

```yaml
ai:
  ollama:
    model: codellama      # 切换到 CodeLlama
  deepseek:
    model: deepseek-coder # 切换到 DeepSeek Coder
```

### Agent 生成参数

```java
ChatRequest request = new ChatRequest();
request.setMessage("你的问题");
request.setTemperature(0.8);  // 创造性：0.0-1.0
request.setMaxTokens(2000);   // 最大生成长度
request.setSystemPrompt("你是一个专业的编程助手");
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

3. **AiAgentService**: Agent 管理服务（模拟实现）
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

**Agent 流程**：
```
用户请求 → AiAgentController → AiAgentService → AgentExecutor（待完善）
```

## 配置说明

### 环境变量

```bash
export OLLAMA_BASE_URL=http://localhost:11434
export DEEPSEEK_API_KEY=your-api-key
export MINIMAX_API_KEY=your-api-key
export DASHSCOPE_API_KEY=your-api-key
export OPENAI_API_KEY=your-api-key
```

### 模型参数

- **temperature**: 控制输出的随机性（0.0-1.0）
- **maxTokens**: 最大生成 token 数
- **systemPrompt**: 系统提示词，定义 AI 的行为
- **model**: 指定使用的模型名称

## 日志管理

### 日志文件位置

```
logs/
├── easy-lowcode-ai.log              # 当前日志
├── easy-lowcode-ai.2026-04-28.0.log # 历史日志
└── ...
```

### 查看日志

```bash
# 实时查看
tail -f logs/easy-lowcode-ai.log

# 搜索错误
grep "ERROR" logs/easy-lowcode-ai.log

# 搜索特定提供商日志
grep "DeepSeek" logs/easy-lowcode-ai.log
```

### 日志级别

- **ERROR**: 错误信息，需要立即处理
- **WARN**: 警告信息，可能需要关注
- **INFO**: 重要信息，如启动、关闭
- **DEBUG**: 调试信息，详细执行流程

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

    private final AgentExecutor agentExecutor;

    public CustomAgent(AgentExecutor agentExecutor) {
        this.agentExecutor = agentExecutor;
    }

    public String executeTask(String task) {
        Object result = agentExecutor.execute(task);
        return result.toString();
    }
}
```

## 依赖说明

- **spring-ai-alibaba-agent-framework**: 1.1.2.0
- **spring-ai-alibaba-starter-dashscope**: 1.1.2.0
- **spring-ai-starter-model-openai**: 1.1.2
- **spring-ai-starter-model-ollama**: 1.1.2
- **spring-ai-client-chat**: 1.1.2

## 故障排除

### Ollama 连接失败

```bash
# 检查 Ollama 是否运行
ollama list
curl http://localhost:11434/api/tags

# 重启 Ollama 服务
ollama serve
```

### 端口被占用

```bash
# 更改端口
mvn spring-boot:run -Dserver.port=8082

# 关闭占用端口的进程
lsof -i :8081
kill -9 <PID>
```

### API 调用失败

1. 检查 API Key 是否正确配置
2. 检查网络连接
3. 查看日志获取详细错误信息：`grep "ERROR" logs/easy-lowcode-ai.log`
4. 确认模型名称是否正确

### Agent 执行失败

1. 确认 Spring AI Alibaba Agent 已启用
2. 检查 Agent Executor 是否正确初始化
3. 查看日志中的异常信息

### API Key 未配置

```bash
# 设置环境变量
export DASHSCOPE_API_KEY=your-api-key

# 或在启动时指定
mvn spring-boot:run -Dai.dashscope.api-key=your-api-key
```

### Java 版本不匹配

```bash
# 需要 Java 21 或更高版本
java -version
```

## 性能优化建议

1. **使用本地模型（Ollama）**：无 API 费用，数据隐私好，但需要足够内存
2. **使用云端模型**：性能强，无需本地资源，有调用费用
3. **JVM 参数调优**：`java -Xms512m -Xmx2g -XX:+UseG1GC -jar app.jar`
4. **缓存策略**：对于重复问题添加缓存层，减少 API 调用

## Docker 部署

```dockerfile
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar app.jar
EXPOSE 8081
ENV SPRING_PROFILES_ACTIVE=prod
ENTRYPOINT ["java", "-jar", "app.jar"]
```

```bash
docker build -t easy-lowcode-ai .
docker run -p 8081:8081 -e DASHSCOPE_API_KEY=your-key easy-lowcode-ai
```

## 监控和管理

启用 Spring Boot Actuator 后可访问：
- 健康检查：`http://localhost:8081/actuator/health`
- 指标信息：`http://localhost:8081/actuator/metrics`
- 环境信息：`http://localhost:8081/actuator/env`

## 注意事项

1. **API Key 安全**: 不要将 API Key 硬编码在代码中，使用环境变量或配置中心
2. **速率限制**: 注意各提供商的 API 调用限制
3. **成本控制**: 监控 API 调用次数和费用
4. **本地模型**: Ollama 需要足够的内存和计算资源
5. **网络要求**: 确保能够访问相应的 API 端点

## 参考资源

- [Spring AI Alibaba 官方文档](https://github.com/alibaba/spring-ai-alibaba)
- [Spring AI 官方文档](https://spring.io/projects/spring-ai)
- [Ollama 官方网站](https://ollama.ai/)
- [DeepSeek 平台](https://platform.deepseek.com/)
- [Minimax API 文档](https://api.minimax.chat/)
- [阿里云通义千问](https://dashscope.aliyun.com/)

## 许可证

本项目采用 MIT 许可证。
