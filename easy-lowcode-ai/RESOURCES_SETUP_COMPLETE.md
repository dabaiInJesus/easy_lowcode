# Resources 目录创建完成说明

## 概述

已为 easy-lowcode-ai 模块创建完整的 resources 目录结构和配置文件。

## 创建的文件

### 主资源配置 (src/main/resources/)

1. **application.yaml** - 主配置文件
   - 包含所有 AI 提供商的基础配置
   - 支持 OpenAI、DashScope、Ollama、DeepSeek、Minimax
   - 使用环境变量管理敏感信息（API Key）

2. **application-dev.yaml** - 开发环境配置
   - 默认使用 Ollama（本地运行，无需 API Key）
   - 启用 DEBUG 级别日志
   - 适合本地开发和测试

3. **application-prod.yaml** - 生产环境配置
   - 默认使用云端模型（DashScope）
   - 使用更强大的模型（qwen-plus）
   - 日志输出到文件，保留 30 天
   - API Key 必须通过环境变量注入

4. **.env.example** - 环境变量示例
   - 包含所有需要的环境变量模板
   - 复制为 `.env` 并填入真实值
   - 已添加到 .gitignore，不会被提交

5. **logback-spring.xml** - 日志配置
   - 控制台输出：彩色日志，便于调试
   - 文件输出：滚动日志，最大 10MB
   - AI 模块专用日志器：DEBUG 级别
   - 日志文件位置：logs/easy-lowcode-ai.log

6. **README.md** - Resources 目录说明
   - 详细的配置文件说明
   - 使用方法和示例
   - 安全建议和故障排查

### 测试资源配置 (src/test/resources/)

1. **application-test.yaml** - 测试环境配置
   - 使用模拟的 API Key
   - 适合单元测试和集成测试
   - 降低日志级别以减少噪音

## 目录结构

```
easy-lowcode-ai/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/dabai/easy_lowcode/ai/
│   │   └── resources/              ✅ 新创建
│   │       ├── application.yaml
│   │       ├── application-dev.yaml
│   │       ├── application-prod.yaml
│   │       ├── .env.example
│   │       ├── logback-spring.xml
│   │       └── README.md
│   └── test/
│       ├── java/
│       └── resources/              ✅ 新创建
│           └── application-test.yaml
└── pom.xml
```

## 使用方法

### 1. 开发环境

```bash
# 方式一：使用 dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# 方式二：设置环境变量
export SPRING_PROFILES_ACTIVE=dev
mvn spring-boot:run
```

开发环境特点：
- ✅ 使用 Ollama 本地模型（无需 API Key）
- ✅ DEBUG 级别日志
- ✅ 详细的控制台输出

### 2. 生产环境

```bash
# 设置必要的 API Key
export DASHSCOPE_API_KEY=your-real-api-key
export SPRING_PROFILES_ACTIVE=prod

# 启动应用
mvn spring-boot:run
```

生产环境特点：
- ✅ 使用云端模型（DashScope/OpenAI）
- ✅ INFO/WARN 级别日志
- ✅ 日志输出到文件
- ✅ 日志滚动和清理

### 3. 测试环境

```bash
# 运行测试时自动使用 test profile
mvn test

# 或手动指定
mvn spring-boot:run -Dspring-boot.run.profiles=test
```

### 4. 自定义配置

可以通过以下方式覆盖配置：

**命令行参数：**
```bash
java -jar easy-lowcode-ai.jar \
  --ai.provider.default=ollama \
  --ai.ollama.model=mistral
```

**环境变量：**
```bash
export AI_PROVIDER_DEFAULT=dashscope
export DASHSCOPE_API_KEY=your-key
export DASHSCOPE_MODEL=qwen-plus
```

**JVM 系统属性：**
```bash
java -Dai.provider.default=openai \
     -DOPENAI_API_KEY=sk-xxx \
     -jar easy-lowcode-ai.jar
```

## 配置优先级

Spring Boot 配置优先级（从高到低）：

1. 命令行参数
2. JVM 系统属性
3. 操作系统环境变量
4. application-{profile}.yaml
5. application.yaml

## 安全建议

### ✅ 推荐做法

1. **使用环境变量管理 API Key**
   ```bash
   export DASHSCOPE_API_KEY=your-key
   ```

2. **使用 .env 文件（不提交到 Git）**
   ```bash
   cp .env.example .env
   # 编辑 .env 填入真实值
   ```

3. **定期轮换 API Key**
   - 每 3-6 个月更新一次
   - 发现泄露立即更换

4. **最小权限原则**
   - 为每个应用分配独立的 API Key
   - 限制 API 调用配额

### ❌ 避免做法

1. **不要硬编码 API Key**
   ```java
   // ❌ 错误
   private String apiKey = "sk-123456";
   
   // ✅ 正确
   @Value("${ai.dashscope.api-key}")
   private String apiKey;
   ```

2. **不要提交 .env 文件到版本控制**
   - 已在 .gitignore 中配置

3. **不要在日志中打印 API Key**
   - 日志配置已过滤敏感信息

## 日志管理

### 日志文件位置

```
logs/
├── easy-lowcode-ai.log              # 当前日志
├── easy-lowcode-ai.2026-04-28.0.log # 历史日志
├── easy-lowcode-ai.2026-04-27.0.log
└── ...
```

### 查看日志

```bash
# 实时查看日志
tail -f logs/easy-lowcode-ai.log

# 查看最近 100 行
tail -n 100 logs/easy-lowcode-ai.log

# 搜索错误日志
grep "ERROR" logs/easy-lowcode-ai.log

# 搜索特定提供商的日志
grep "DeepSeek" logs/easy-lowcode-ai.log

# 查看今天的日志
grep "$(date +%Y-%m-%d)" logs/easy-lowcode-ai.log
```

### 日志级别

- **ERROR**: 错误信息，需要立即处理
- **WARN**: 警告信息，可能需要关注
- **INFO**: 重要信息，如启动、关闭
- **DEBUG**: 调试信息，详细执行流程

## 故障排查

### 问题 1: 配置不生效

**检查步骤：**

1. 确认激活的 profile
   ```bash
   echo $SPRING_PROFILES_ACTIVE
   ```

2. 查看启动日志
   ```bash
   grep "The following profiles are active" logs/easy-lowcode-ai.log
   ```

3. 验证配置文件是否存在
   ```bash
   ls -la src/main/resources/application*.yaml
   ```

### 问题 2: API Key 未找到

**检查步骤：**

1. 确认环境变量已设置
   ```bash
   echo $DASHSCOPE_API_KEY
   ```

2. 检查 .env 文件是否加载
   ```bash
   cat .env | grep DASHSCOPE_API_KEY
   ```

3. 查看启动日志中的配置值
   ```bash
   grep "dashscope" logs/easy-lowcode-ai.log
   ```

### 问题 3: 日志文件未生成

**检查步骤：**

1. 确认 logs 目录存在且有写权限
   ```bash
   mkdir -p logs
   chmod 755 logs
   ```

2. 检查 logback 配置是否正确
   ```bash
   cat src/main/resources/logback-spring.xml
   ```

3. 查看控制台是否有日志输出错误

## 最佳实践

### 1. 环境隔离

- **开发环境**: 使用 Ollama 本地模型
- **测试环境**: 使用模拟配置
- **生产环境**: 使用云端模型 + 严格的安全策略

### 2. 配置管理

- 基础配置放在 `application.yaml`
- 环境特定配置放在 `application-{profile}.yaml`
- 敏感信息通过环境变量注入

### 3. 日志策略

- 开发环境：DEBUG 级别，控制台输出
- 生产环境：INFO/WARN 级别，文件输出
- 定期清理旧日志文件

### 4. 安全措施

- 使用 .gitignore 保护敏感文件
- 定期轮换 API Key
- 监控 API 调用量和费用
- 设置合理的超时和重试策略

## 下一步

1. ✅ Resources 目录已创建
2. ✅ 配置文件已完成
3. ✅ 日志配置已完成
4. 📋 根据实际需求调整配置
5. 📋 设置 CI/CD 中的环境变量
6. 📋 配置监控和告警

## 相关文档

- [主 README](../README.md) - 完整功能说明
- [快速开始](../QUICK_START.md) - 快速上手指南
- [配置示例](../CONFIG_EXAMPLE.yaml) - 详细配置参考
- [集成说明](../INTEGRATION_SUMMARY.md) - 技术细节

---

**创建时间**: 2026-04-28  
**最后更新**: 2026-04-28  
**版本**: 1.0.0-SNAPSHOT
