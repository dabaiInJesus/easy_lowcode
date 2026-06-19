# Easy Lowcode - 低代码平台

基于 Spring Boot 3 + Vue 3 的前后端分离低代码开发平台。

## 项目结构

```
easy_lowcode/
├── easy-lowcode-common          # 公共模块 - 工具类、常量、异常处理
├── easy-lowcode-database        # 数据库模块 - MyBatisPlus配置、Liquibase迁移
├── easy-lowcode-auth            # 认证授权模块 - Sa-Token、用户/角色/权限管理
├── easy-lowcode-ai              # AI模块 - 多Provider对话/Agent/配置管理
├── easy-lowcode-collector       # 数据采集模块 - 多数据源配置、数据同步
├── easy-lowcode-resource        # 资源查询模块 - 动态SQL、数据权限
├── easy-lowcode-dashboard       # 可视化大屏模块 - 图表、SQL引擎
├── easy-lowcode-etl             # ETL模块 - SpringBatch任务配置
├── easy-lowcode-gateway         # API网关 - 统一入口、CORS（独立部署）
├── easy-lowcode-startup         # 启动模块 - 单体运行入口
└── easy-lowcode-frontend        # 前端项目 - Vue 3 + Vite + Element Plus
```

## 构建命令

### 后端

```bash
# 完整构建（跳过测试）
mvn clean install -DskipTests

# 仅编译
mvn clean compile -DskipTests

# 运行测试
mvn test

# 单模块构建
mvn clean install -pl easy-lowcode-auth -am
```

### 前端

```bash
cd easy-lowcode-frontend
npm ci
npm run build
npm run dev        # 开发服务器
npm run lint       # ESLint检查
npx vue-tsc --noEmit  # TypeScript类型检查
```

## 本地开发

### 环境要求
- JDK 21+
- Maven 3.9+
- Node.js 18+
- PostgreSQL 17
- Redis 6+
- RocketMQ 5.3

### 启动依赖服务

```bash
docker-compose up -d postgres redis rocketmq-namesrv rocketmq-broker
```

### 启动应用

```bash
# 方式一：脚本启动
./start.sh

# 方式二：Maven启动
cd easy-lowcode-startup && mvn spring-boot:run
```

### 访问地址
- 应用: http://localhost:8081
- Swagger: http://localhost:8081/swagger-ui.html
- Actuator: http://localhost:8081/actuator/health

## 环境变量

| 变量 | 必须 | 说明 |
|------|------|------|
| `POSTGRES_PASSWORD` | ✅ | PostgreSQL 密码 |
| `DASHSCOPE_API_KEY` | ⭐推荐 | 通义千问 API Key |
| `OPENAI_API_KEY` | ⭐推荐 | OpenAI API Key |
| `DEEPSEEK_API_KEY` | ⭐推荐 | DeepSeek API Key |
| `REDIS_PASSWORD` | ❌ | Redis 密码（留空无密码） |

## 技术规范

### 代码风格
- 使用 Lombok 简化代码
- 统一使用 `Result` 封装返回结果
- 异常统一由 `GlobalExceptionHandler` 处理
- 实体类继承 `BaseEntity`
- Mapper 继承 `BaseMapper`

### 权限控制（Sa-Token）
```java
@SaCheckLogin                    // 需要登录
@SaCheckRole("admin")           // 需要admin角色
@SaCheckPermission("system:user:list")  // 需要特定权限
```

### 数据库
- 使用 Liquibase 自动管理数据库迁移
- 变更日志: `classpath:db/changelog/db.changelog-master.xml`
- 逻辑删除字段: `deleted`（0=未删除, 1=已删除）
- 主键策略: `assign_id`（雪花算法）

### 命名约定
- 包名: `com.dabai.easy_lowcode.{module}`
- 表名: `snake_case`（如 `sys_user`）
- 字段名: `snake_case`（MyBatis Plus自动驼峰转换）

## 模块依赖关系

```
easy-lowcode-common
    ↑
easy-lowcode-database
    ↑
    ├── easy-lowcode-auth
    ├── easy-lowcode-resource
    ├── easy-lowcode-dashboard
    ├── easy-lowcode-collector
    └── easy-lowcode-etl
    ↑
easy-lowcode-startup (整合所有业务模块)

easy-lowcode-gateway (独立部署)
```

## API 规范

### 请求格式
- 认证: `Authorization: Bearer {token}`（Header）
- 内容类型: `application/json`

### 响应格式
```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1234567890
}
```

## 测试

### 后端测试
```bash
# 运行所有测试
mvn test

# 运行单个模块测试
mvn test -pl easy-lowcode-auth

# 运行单个测试类
mvn test -pl easy-lowcode-auth -Dtest=SysRoleServiceImplTest
```

### 前端测试
```bash
cd easy-lowcode-frontend
npm run lint
npx vue-tsc --noEmit
```

## Docker 部署

```bash
# 一键启动
docker-compose up -d

# 查看日志
docker-compose logs -f app

# 停止服务
docker-compose down
```

## 常见问题

### 编译错误
```bash
mvn clean install -U  # 强制更新依赖
```

### 数据库连接失败
- 检查 PostgreSQL 是否启动
- 确认 `application.yaml` 中的数据库配置
- 确认数据库已创建

### Redis 连接失败
```bash
redis-cli ping  # 应返回 PONG
```

## CI/CD

GitHub Actions 工作流:
- `backend-test`: 后端编译和单元测试
- `frontend-test`: 前端构建和类型检查
- `docker-build`: Docker镜像构建（main分支）
- `security-scan`: Trivy安全扫描

## Security Notes

- JWT secret must be configured via `JWT_SECRET` env var (never hardcode)
- Token blacklisting uses Redis - ensure Redis is running
- Passwords are BCrypt-hashed, never logged
- AES encryption key for sensitive data via `ENCRYPT_AES_KEY` env var

## 参考文档

- `README.md` - 项目概述和快速开始
- `DEVELOPMENT.md` - 开发环境配置详细指南
- `CONTRIBUTING.md` - 贡献指南
- `docs/` - 其他文档
