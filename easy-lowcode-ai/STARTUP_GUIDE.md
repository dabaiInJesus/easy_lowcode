# Easy Lowcode AI 模块启动指南

## 概述

easy-lowcode-ai 模块现在可以独立运行，提供完整的 AI 聊天和 Agent 功能。

## 快速启动

### 方式一：使用启动脚本（推荐）

#### Linux/macOS
```bash
# 赋予执行权限
chmod +x start.sh

# 启动（默认使用 dev 环境）
./start.sh

# 或指定环境
./start.sh prod
```

#### Windows
```batch
# 双击运行或使用命令行
start.bat

# 或指定环境
start.bat prod
```

### 方式二：使用 Maven 命令

```bash
# 开发环境（默认使用 Ollama）
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 生产环境
export DASHSCOPE_API_KEY=your-api-key
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# 测试环境
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### 方式三：打包后运行

```bash
# 1. 打包
mvn clean package -DskipTests

# 2. 运行 JAR
java -jar target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar

# 3. 指定环境和配置
java -jar target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar \
  --spring.profiles.active=prod \
  --ai.provider.default=dashscope
```

## 验证启动

### 1. 健康检查

```bash
curl http://localhost:8081/api/ai/health
```

预期响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "status": "UP",
    "service": "easy-lowcode-ai",
    "timestamp": "2026-04-28T20:00:00",
    "version": "1.0.0-SNAPSHOT"
  }
}
```

### 2. 服务信息

```bash
curl http://localhost:8081/api/ai/info
```

预期响应：
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "name": "Easy Lowcode AI Module",
    "description": "AI 模块 - 支持多种大模型提供商",
    "version": "1.0.0-SNAPSHOT",
    "supportedProviders": [
      "openai",
      "dashscope",
      "ollama",
      "deepseek",
      "minimax"
    ]
  }
}
```

### 3. 浏览器访问

打开浏览器访问：
- 健康检查：http://localhost:8081/api/ai/health
- 服务信息：http://localhost:8081/api/ai/info

## 环境说明

### 开发环境 (dev)

**特点：**
- 默认使用 Ollama 本地模型
- 无需 API Key
- DEBUG 级别日志
- 适合本地开发和测试

**启动前准备：**
```bash
# 1. 安装 Ollama
# 访问 https://ollama.ai/ 下载并安装

# 2. 拉取模型
ollama pull llama2

# 3. 启动 Ollama 服务
ollama serve
```

**启动命令：**
```bash
./start.sh dev
```

### 生产环境 (prod)

**特点：**
- 默认使用 DashScope（通义千问）
- 需要配置 API Key
- INFO/WARN 级别日志
- 日志输出到文件

**启动前准备：**
```bash
# 设置 API Key
export DASHSCOPE_API_KEY=your-dashscope-api-key
```

**启动命令：**
```bash
./start.sh prod
```

### 测试环境 (test)

**特点：**
- 使用模拟配置
- 适合单元测试
- WARN 级别日志

**启动命令：**
```bash
./start.sh test
```

## 配置自定义

### 切换 AI 提供商

```bash
# 使用 OpenAI
mvn spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dai.provider.default=openai

# 使用 DeepSeek
mvn spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dai.provider.default=deepseek

# 使用 Minimax
mvn spring-boot:run \
  -Dspring-boot.run.profiles=dev \
  -Dai.provider.default=minimax
```

### 修改端口

```bash
# 命令行参数
mvn spring-boot:run -Dserver.port=9090

# 或环境变量
export SERVER_PORT=9090
mvn spring-boot:run
```

### 自定义模型

```bash
# 使用不同的 Ollama 模型
mvn spring-boot:run \
  -Dai.ollama.model=mistral

# 使用不同的 DashScope 模型
mvn spring-boot:run \
  -Dai.dashscope.model=qwen-plus
```

## API 使用示例

### 1. 聊天接口

```bash
curl -X POST http://localhost:8081/api/ai/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "你好，请介绍一下自己",
    "systemPrompt": "你是一个有用的AI助手",
    "temperature": 0.7
  }'
```

### 2. Agent 接口

```bash
# 获取 Agent 列表
curl http://localhost:8081/api/ai/agent/list

# 执行 Agent 任务
curl -X POST http://localhost:8081/api/ai/agent/execute \
  -H "Content-Type: application/json" \
  -d '{
    "agentName": "SampleAgent",
    "task": "请帮我分析这段代码"
  }'
```

## 日志查看

### 控制台日志

启动后直接在终端查看实时日志。

### 文件日志（生产环境）

```bash
# 查看实时日志
tail -f logs/easy-lowcode-ai.log

# 搜索错误
grep "ERROR" logs/easy-lowcode-ai.log

# 查看今天的日志
grep "$(date +%Y-%m-%d)" logs/easy-lowcode-ai.log
```

## 故障排查

### 问题 1: 端口被占用

**错误信息：**
```
Web server failed to start. Port 8081 was already in use.
```

**解决方案：**
```bash
# 方案一：更改端口
mvn spring-boot:run -Dserver.port=8082

# 方案二：关闭占用端口的进程
lsof -i :8081
kill -9 <PID>
```

### 问题 2: Ollama 连接失败

**错误信息：**
```
Connection refused: localhost/127.0.0.1:11434
```

**解决方案：**
```bash
# 检查 Ollama 是否运行
curl http://localhost:11434/api/tags

# 如果没有响应，启动 Ollama
ollama serve
```

### 问题 3: API Key 未配置

**错误信息：**
```
API key is required
```

**解决方案：**
```bash
# 设置环境变量
export DASHSCOPE_API_KEY=your-api-key

# 或在启动时指定
mvn spring-boot:run \
  -Dai.dashscope.api-key=your-api-key
```

### 问题 4: Java 版本不匹配

**错误信息：**
```
Unsupported class file major version
```

**解决方案：**
```bash
# 检查 Java 版本
java -version

# 需要 Java 21 或更高版本
# 如果版本过低，请升级 Java
```

## 停止服务

### 方式一：Ctrl+C

在运行服务的终端按 `Ctrl+C`。

### 方式二：kill 命令

```bash
# 查找进程
ps aux | grep easy-lowcode-ai

# 杀死进程
kill -9 <PID>
```

### 方式三：Maven 命令

```bash
# 如果使用 mvn spring-boot:run 启动
# 在另一个终端执行
mvn spring-boot:stop
```

## 性能优化

### JVM 参数调优

```bash
java -Xms512m -Xmx2g \
  -XX:+UseG1GC \
  -jar target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar
```

### 启用压缩

```bash
java -Dserver.compression.enabled=true \
  -Dserver.compression.mime-types=application/json,text/plain \
  -jar target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar
```

## 监控和管理

### Actuator 端点

如果启用了 Spring Boot Actuator，可以访问：

- 健康检查：http://localhost:8081/actuator/health
- 指标信息：http://localhost:8081/actuator/metrics
- 环境信息：http://localhost:8081/actuator/env

### JMX 监控

启用 JMX：
```bash
java -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9999 \
  -jar target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar
```

然后使用 JConsole 或 VisualVM 连接监控。

## 部署建议

### Docker 部署

创建 `Dockerfile`：
```dockerfile
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY target/easy-lowcode-ai-1.0.0-SNAPSHOT.jar app.jar

EXPOSE 8081

ENV SPRING_PROFILES_ACTIVE=prod

ENTRYPOINT ["java", "-jar", "app.jar"]
```

构建和运行：
```bash
docker build -t easy-lowcode-ai .
docker run -p 8081:8081 \
  -e DASHSCOPE_API_KEY=your-key \
  easy-lowcode-ai
```

### Kubernetes 部署

创建 deployment.yaml，配置环境变量和资源限制。

## 下一步

1. ✅ 启动类已创建
2. ✅ 启动脚本已创建
3. ✅ 健康检查接口已添加
4. 📋 根据实际需求调整配置
5. 📋 配置 CI/CD 自动化部署
6. 📋 添加监控和告警

## 相关文档

- [主 README](README.md) - 完整功能说明
- [快速开始](QUICK_START.md) - 快速上手指南
- [资源配置说明](src/main/resources/README.md) - 配置文件详解

---

**最后更新**: 2026-04-28  
**版本**: 1.0.0-SNAPSHOT
