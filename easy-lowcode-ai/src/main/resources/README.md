# Resources 目录说明

本目录包含 easy-lowcode-ai 模块的配置文件和资源文件。

## 目录结构

```
src/main/resources/
├── application.yaml              # 主配置文件
├── application-dev.yaml          # 开发环境配置
├── application-prod.yaml         # 生产环境配置
├── .env.example                  # 环境变量示例
└── logback-spring.xml           # 日志配置文件
```

## 配置文件说明

### 1. application.yaml
主配置文件，包含所有 AI 提供商的基础配置：
- OpenAI 配置
- 阿里云通义千问（DashScope）配置
- Ollama 配置
- DeepSeek 配置
- Minimax 配置

### 2. application-dev.yaml
开发环境配置：
- 默认使用 Ollama（本地运行，无需 API Key）
- 启用 DEBUG 级别日志
- 适合本地开发和测试

### 3. application-prod.yaml
生产环境配置：
- 默认使用云端模型（DashScope 或 OpenAI）
- 使用更强大的模型（qwen-plus, gpt-4）
- 日志输出到文件，保留 30 天
- API Key 通过环境变量注入

### 4. .env.example
环境变量示例文件：
- 复制为 `.env` 并填入真实的 API Key
- 不要将包含真实 Key 的文件提交到版本控制

### 5. logback-spring.xml
日志配置文件：
- 控制台输出：彩色日志，便于开发调试
- 文件输出：滚动日志，最大 10MB，保留 30 天
- AI 模块专用日志器：DEBUG 级别
- Spring AI 日志：INFO 级别

## 使用方法

### 开发环境

```bash
# 使用开发环境配置
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 或者设置环境变量
export SPRING_PROFILES_ACTIVE=dev
```

### 生产环境

```bash
# 使用生产环境配置
mvn spring-boot:run -Dspring-boot.run.profiles=prod

# 设置必要的 API Key
export DASHSCOPE_API_KEY=your-api-key
export SPRING_PROFILES_ACTIVE=prod
```

### 自定义配置

可以通过以下方式覆盖配置：

1. **命令行参数**：
```bash
java -jar easy-lowcode-ai.jar --ai.provider.default=ollama
```

2. **环境变量**：
```bash
export AI_PROVIDER_DEFAULT=dashscope
export DASHSCOPE_API_KEY=your-key
```

3. **JVM 系统属性**：
```bash
java -Dai.provider.default=openai -jar easy-lowcode-ai.jar
```

## 安全建议

1. **不要硬编码 API Key**：始终使用环境变量或配置中心
2. **使用 .gitignore**：确保 `.env` 文件不被提交
3. **定期轮换 Key**：定期更新 API Key 以提高安全性
4. **最小权限原则**：为每个应用分配独立的 API Key

## 日志管理

日志文件位置：`logs/easy-lowcode-ai.log`

查看实时日志：
```bash
tail -f logs/easy-lowcode-ai.log
```

搜索特定日志：
```bash
grep "ERROR" logs/easy-lowcode-ai.log
grep "DeepSeek" logs/easy-lowcode-ai.log
```

## 故障排查

如果配置不生效，检查：

1. 确认激活的 profile：
```bash
echo $SPRING_PROFILES_ACTIVE
```

2. 查看启动日志中的配置信息：
```bash
grep "ai.provider" logs/easy-lowcode-ai.log
```

3. 验证环境变量是否正确设置：
```bash
echo $DASHSCOPE_API_KEY
```

## 更多信息

- 详细配置说明：参见项目根目录的 `CONFIG_EXAMPLE.yaml`
- 快速开始指南：参见 `QUICK_START.md`
- 完整文档：参见 `README.md`
