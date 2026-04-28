# Easy Lowcode AI 模块 - 快速启动指南

## 概述

本模块已集成 Spring AI Alibaba 1.1.2.0，支持多种 AI 模型提供商和 Agent 功能。

## 支持的 AI 提供商

✅ **OpenAI** - GPT-3.5, GPT-4  
✅ **阿里云通义千问** - qwen-turbo, qwen-plus, qwen-max  
✅ **百度文心一言** - eb-instant, ernie-bot  
✅ **Ollama** - 本地运行的开源模型（推荐用于开发）  
✅ **DeepSeek** - deepseek-chat, deepseek-coder  
✅ **Minimax** - abab6-chat 及其他模型  
✅ **腾讯混元**、**智谱清言**、**Moonshot** 等

## 快速开始（推荐：使用 Ollama）

### 1. 安装 Ollama（本地运行，无需 API Key）

```bash
# Windows/macOS/Linux
# 访问 https://ollama.ai/ 下载并安装

# 验证安装
ollama --version
```

### 2. 拉取模型

```bash
# 拉取 Llama 2 模型（约 3.8GB）
ollama pull llama2

# 或者使用更小的模型进行测试
ollama pull mistral    # Mistral 7B
ollama pull codellama  # CodeLlama（适合代码任务）
```

### 3. 启动 Ollama 服务

```bash
# 启动服务（保持运行）
ollama serve
```

### 4. 配置应用

编辑 `easy-lowcode-startup/src/main/resources/application.yaml`：

```yaml
ai:
  provider:
    default: ollama  # 设置为 ollama
  
  ollama:
    base-url: http://localhost:11434
    model: llama2  # 或 mistral, codellama 等
```

### 5. 启动应用

```bash
# 在项目根目录执行
mvn spring-boot:run -pl easy-lowcode-startup
```

### 6. 测试聊天功能

```bash
# 发送聊天请求
curl -X POST http://localhost:8080/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己",
    "systemPrompt": "你是一个有用的AI助手",
    "temperature": 0.7
  }'
```

## 使用云端 AI 提供商

### DeepSeek

1. 注册并获取 API Key：https://platform.deepseek.com/
2. 配置环境变量：
   ```bash
   export DEEPSEEK_API_KEY=your-api-key-here
   ```
3. 修改配置：
   ```yaml
   ai:
     provider:
       default: deepseek
     deepseek:
       api-key: ${DEEPSEEK_API_KEY}
       model: deepseek-chat
   ```

### Minimax

1. 注册并获取 API Key：https://api.minimax.chat/
2. 配置环境变量：
   ```bash
   export MINIMAX_API_KEY=your-api-key-here
   ```
3. 修改配置：
   ```yaml
   ai:
     provider:
       default: minimax
     minimax:
       api-key: ${MINIMAX_API_KEY}
       model: abab6-chat
   ```

### 通义千问（阿里云）

1. 注册并获取 API Key：https://dashscope.aliyun.com/
2. 配置环境变量：
   ```bash
   export DASHSCOPE_API_KEY=your-api-key-here
   ```
3. 修改配置：
   ```yaml
   ai:
     provider:
       default: dashscope
     dashscope:
       api-key: ${DASHSCOPE_API_KEY}
       model: qwen-turbo
   ```

## Agent 功能使用

### 1. 获取可用 Agent 列表

```bash
curl http://localhost:8080/api/ai/agent/list
```

### 2. 执行 Agent 任务

```bash
curl -X POST http://localhost:8080/api/ai/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "SampleAgent",
    "task": "请帮我分析这段代码的优缺点"
  }'
```

### 3. 创建自定义 Agent

```bash
curl -X POST http://localhost:8080/api/ai/agent/create \
  -H "Content-Type: application/json" \
  -d '{
    "name": "CodeReviewer",
    "description": "代码审查助手",
    "instructions": "你是一个专业的代码审查员"
  }'
```

## 高级配置

### Spring AI Alibaba Agent 配置

在 `application.yaml` 中添加：

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

### 切换不同的模型

```yaml
ai:
  ollama:
    model: codellama  # 切换到 CodeLlama 进行代码任务
    
  deepseek:
    model: deepseek-coder  # 切换到 DeepSeek Coder
```

### 调整生成参数

```java
ChatRequest request = new ChatRequest();
request.setMessage("你的问题");
request.setTemperature(0.8);  // 创造性：0.0-1.0
request.setMaxTokens(2000);   // 最大生成长度
request.setSystemPrompt("你是一个专业的编程助手");
```

## 常见问题

### Q1: Ollama 连接失败？

```bash
# 检查 Ollama 是否运行
curl http://localhost:11434/api/tags

# 如果没有响应，启动 Ollama
ollama serve
```

### Q2: 如何查看可用的 Ollama 模型？

```bash
ollama list
```

### Q3: API Key 配置后仍然报错？

检查环境变量是否正确设置：

```bash
# Linux/macOS
echo $DEEPSEEK_API_KEY

# Windows PowerShell
echo $env:DEEPSEEK_API_KEY
```

### Q4: 如何切换 AI 提供商？

只需修改配置文件中的 `ai.provider.default` 值，然后重启应用。

## 性能优化建议

1. **使用本地模型（Ollama）**：
   - 优点：无 API 调用费用，数据隐私好
   - 缺点：需要足够的内存和计算资源

2. **使用云端模型**：
   - 优点：性能强，无需本地资源
   - 缺点：有 API 调用费用，需要网络

3. **缓存策略**：
   - 对于重复的问题，可以添加缓存层
   - 减少 API 调用次数

## 下一步

- 📖 查看 [README.md](README.md) 了解详细功能
- 📝 查看 [CONFIG_EXAMPLE.yaml](CONFIG_EXAMPLE.yaml) 获取完整配置示例
- 🧪 运行集成测试：`mvn test -pl easy-lowcode-ai`
- 🔍 查看 Spring AI Alibaba 官方文档

## 技术支持

如有问题，请查看：
- 应用日志：`logs/easy-lowcode.log`
- Spring AI Alibaba 文档：https://github.com/alibaba/spring-ai-alibaba
- Ollama 文档：https://ollama.ai/docs
