# .env 文件自动加载功能

## 📋 功能说明

项目已集成 `dotenv-java` 库，支持自动加载 `.env` 文件中的环境变量配置。

## 🎯 工作原理

1. **启动时自动加载**：应用启动时会自动查找并加载 `.env` 文件
2. **智能路径搜索**：会在多个可能的位置查找 `.env` 文件（当前目录、上级目录等）
3. **优先级机制**：系统环境变量 > `.env` 文件 > 默认值
4. **容错处理**：如果 `.env` 文件不存在，应用仍会正常启动，只是使用系统环境变量或默认值

## 📁 文件位置

`.env` 文件应放置在项目根目录：
```
easy-lowcode/
├── .env              # ← 放在这里
├── .env.example      # 示例配置文件
├── easy-lowcode-startup/
└── ...
```

## 🔧 配置示例

参考 `.env.example` 创建 `.env` 文件：

```bash
# 数据库配置
POSTGRES_USER=postgres
POSTGRES_PASSWORD=your_password
POSTGRES_DB=easy_lowcode

# Redis 配置
REDIS_PASSWORD=

# AI API Keys（至少配置一个）
MINIMAX_API_KEY=your-minimax-api-key
DASHSCOPE_API_KEY=your-dashscope-api-key
OPENAI_API_KEY=your-openai-api-key
DEEPSEEK_API_KEY=your-deepseek-api-key

# AI 默认供应商
AI_DEFAULT_PROVIDER=minimax

# CORS 配置
CORS_ORIGINS=**
```

## 🚀 使用方式

### 1. 本地开发（Maven）

```bash
# 直接启动，会自动加载 .env 文件
cd easy-lowcode-startup
mvn spring-boot:run
```

### 2. 使用启动脚本

```bash
# Linux/Mac
./start.sh

# Windows
start.bat
```

### 3. Docker Compose

Docker Compose 也会自动读取 `.env` 文件：

```bash
docker-compose up -d
```

### 4. IDEA 运行配置

在 IDEA 中运行时，无需额外配置，启动类会自动加载 `.env` 文件。

## ⚙️ 技术实现

### 依赖配置

在 `pom.xml` 中添加了 `dotenv-java` 依赖：

```xml
<dependency>
    <groupId>io.github.cdimascio</groupId>
    <artifactId>dotenv-java</artifactId>
    <version>3.0.0</version>
</dependency>
```

### 启动类实现

在 `EasyLowcodeApplication.java` 中实现了自动加载逻辑：

```java
private static void loadEnvFile() {
    // 尝试多个可能的位置
    String[] possiblePaths = {"./", "../", "../../"};
    
    for (String path : possiblePaths) {
        try {
            dotenv = Dotenv.configure()
                    .directory(path)
                    .filename(".env")
                    .load();
            break;
        } catch (Exception e) {
            // 继续尝试下一个路径
        }
    }
    
    // 将 .env 变量设置到系统属性
    dotenv.entries().forEach(entry -> {
        if (System.getProperty(entry.getKey()) == null) {
            System.setProperty(entry.getKey(), entry.getValue());
        }
    });
}
```

## 🔐 安全注意事项

1. **不要提交 `.env` 文件到 Git**
   - `.env` 已在 `.gitignore` 中配置
   - 只提交 `.env.example` 作为模板

2. **敏感信息管理**
   - 数据库密码
   - API Keys
   - 加密密钥
   
   都应该放在 `.env` 文件中，而不是硬编码在代码里。

3. **生产环境部署**
   - 建议使用 Kubernetes Secrets
   - 或使用云平台的环境变量管理
   - 不要将 `.env` 文件打包到镜像中

## ❓ 常见问题

### Q1: 为什么我的配置没有生效？

**A:** 检查以下几点：
1. `.env` 文件是否在项目根目录
2. 文件格式是否正确（`KEY=VALUE`，不要有空格）
3. 查看启动日志，确认是否显示 "✅ .env 文件加载成功"

### Q2: 如何验证配置已加载？

**A:** 查看应用启动日志，应该能看到：
```
✅ .env 文件加载成功 (位置: ./)
```

### Q3: 可以同时使用系统环境变量和 .env 文件吗？

**A:** 可以。系统环境变量的优先级更高：
- 如果系统已设置 `DB_PASSWORD`，则使用系统的值
- 否则使用 `.env` 文件中的值
- 最后使用 `application.yaml` 中的默认值

### Q4: Docker 环境下如何处理？

**A:** Docker Compose 会自动读取 `.env` 文件并注入到容器中，Spring Boot 会从容器环境变量中读取。

## 📝 相关文档

- [dotenv-java GitHub](https://github.com/cdimascio/dotenv-java)
- [Spring Boot 外部化配置](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.external-config)
- [Docker Compose 环境变量](https://docs.docker.com/compose/environment-variables/)
