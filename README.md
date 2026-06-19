# Easy Lowcode - 低代码平台

一个基于 Spring Boot 3 + Vue 3 的前后端分离低代码开发平台。

## 技术栈

### 后端技术
- **核心框架**: Spring Boot 3.5.5
- **ORM框架**: MyBatis Plus 3.5.5
- **权限认证**: Spring Security 6.x + JWT (jjwt 0.12.5)
- **工作流引擎**: Flowable 7.0.1
- **AI集成**: Spring AI 1.1.2（支持通义千问/OpenAI/DeepSeek/MiniMax/Ollama等）
- **数据库**: PostgreSQL 17 + pgvector
- **消息队列**: RocketMQ 5.3
- **批处理**: Spring Batch
- **构建工具**: Maven 3.9
- **容器化**: Docker + Docker Compose

### 前端技术
- **核心框架**: Vue 3.4+ / TypeScript
- **构建工具**: Vite
- **UI组件库**: Element Plus
- **状态管理**: Pinia
- **HTTP客户端**: Axios

## 项目结构

```
easy_lowcode/
├── easy-lowcode-common          # 公共模块 - 工具类、常量、异常处理等
├── easy-lowcode-database        # 数据库模块 - MyBatisPlus配置、通用Mapper、Liquibase迁移
├── easy-lowcode-auth            # 认证授权模块 - Spring Security、用户/角色/权限管理
├── easy-lowcode-ai              # AI模块 - 多Provider对话/Agent/配置管理
├── easy-lowcode-collector       # 数据采集模块 - 多数据源配置、数据同步
├── easy-lowcode-resource        # 资源查询模块 - 动态SQL、数据权限
├── easy-lowcode-dashboard        # 可视化大屏模块 - 图表、SQL引擎
├── easy-lowcode-etl             # ETL模块 - SpringBatch任务配置
├── easy-lowcode-gateway          # API网关 - 统一入口、CORS
├── easy-lowcode-startup          # 启动模块 - 单体运行入口
├── easy-lowcode-frontend        # 前端项目 - Vue 3 + Vite + Element Plus
├── docker-compose.yml           # Docker Compose（PostgreSQL + Redis + RocketMQ + App）
├── Dockerfile                   # 应用镜像构建
└── .env.example                 # 环境变量示例
```

## 快速开始

### 方式一: Docker Compose 一键启动（推荐）

```bash
# 1. 复制环境变量文件
cp .env.example .env
# 编辑 .env 填入真实密码和 API Key

# 2. 启动所有服务（含数据库/队列）
docker-compose up -d

# 3. 查看日志
docker-compose logs -f app

# 4. 访问
#   应用: http://localhost:8081
#   Swagger: http://localhost:8081/swagger-ui.html
#   RocketMQ Console: http://localhost:8088
```

### 方式二: 本地开发启动

**环境要求**
- JDK 21+ / Maven 3.9+ / Node.js 18+
- PostgreSQL 17 + Redis 6+ + RocketMQ 5.3

**步骤**

```bash
# 1. 启动依赖服务
docker-compose up -d postgres redis rocketmq-namesrv rocketmq-broker

# 2. 配置环境变量
export DB_PASSWORD=your_db_password
export DASHSCOPE_API_KEY=your_api_key
# ... 其他变量见 .env.example

# 3. 编译并启动
./start.sh
# 或
cd easy-lowcode-startup && mvn spring-boot:run
```

### 数据库

数据库通过 Liquibase 自动管理，启动时自动执行迁移，无需手动执行 SQL。

## API 文档

启动后访问 Swagger UI：http://localhost:8081/swagger-ui.html

## 环境变量

| 变量 | 必须 | 说明 |
|------|------|------|
| `POSTGRES_PASSWORD` | ✅ | PostgreSQL 密码 |
| `DASHSCOPE_API_KEY` | ⭐推荐 | 通义千问 API Key |
| `OPENAI_API_KEY` | ⭐推荐 | OpenAI API Key |
| `DEEPSEEK_API_KEY` | ⭐推荐 | DeepSeek API Key |
| `REDIS_PASSWORD` | ❌ | Redis 密码（留空无密码） |
| `CORS_ORIGINS` | ❌ | CORS 允许的来源（默认 **） |
| `JWT_SECRET` | ✅ | JWT 签名密钥 |
| `ENCRYPT_AES_KEY` | ✅ | AES 加密密钥（敏感数据加密） |
| `AI_DEFAULT_PROVIDER` | ⭐推荐 | AI 默认供应商 |
| `MINIMAX_API_KEY` | ⭐推荐 | MiniMax API Key |
| `ROCKETMQ_NAMESERVER` | ❌ | RocketMQ NameServer 地址（默认 localhost:9876） |

## 后续规划

1. **代码生成器**: 根据数据库表自动生成前后端代码
2. **表单设计器**: 可视化表单设计
3. **流程设计器**: BPMN 流程设计
4. **报表设计器**: 可视化报表设计
5. **大屏设计器**: 拖拽式大屏设计
6. **API设计器**: RESTful API 可视化配置

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
