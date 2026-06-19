# 低代码平台 - 快速开始指南

欢迎使用 Easy Lowcode 低代码开发平台！本指南将帮助您快速上手。

## 📖 目录

- [项目简介](#项目简介)
- [技术架构](#技术架构)
- [环境要求](#环境要求)
- [快速启动](#快速启动)
- [项目结构](#项目结构)
- [核心功能](#核心功能)
- [开发指南](#开发指南)
- [常见问题](#常见问题)

## 项目简介

Easy Lowcode 是一个基于 Spring Boot 3 + Vue 3 的前后端分离低代码开发平台，提供：

- ✅ 完整的权限管理体系（用户、角色、菜单、部门）
- ✅ 第三方应用集成支持
- ✅ 多数据源数据采集（支持主流数据库和国产数据库）
- ✅ 可视化 ETL 数据处理（基于 Spring Batch）
- ✅ 工作流引擎集成（Flowable）
- ✅ AI 能力集成（Spring AI）
- ✅ 可视化大屏展示
- ✅ 动态资源查询

## 技术架构

### 后端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 3.5.5 | 核心框架 |
| MyBatis Plus | 3.5.5 | ORM 框架 |
| Sa-Token | 1.37.0 | 权限认证 |
| Flowable | 7.0.1 | 工作流引擎（禁用中） |
| Spring AI | 1.1.4 | AI 集成 |
| PostgreSQL | 17 | 主数据库 |
| Redis | 6+ | 缓存/会话存储 |
| RocketMQ | 5.3 | 消息队列 |
| Spring Batch | 3.x | 批处理 |

### 前端技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Vue | 3.4+ | 核心框架 |
| Vite | 5.x | 构建工具 |
| Element Plus | 2.x | UI 组件库 |
| TailwindCSS | 3.4+ | 原子化 CSS |
| TypeScript | 5.x | 类型系统 |

## 环境要求

### 必需软件

- **JDK**: 21 或更高版本
- **Maven**: 3.6 或更高版本
- **PostgreSQL**: 17
- **Redis**: 6 或更高版本

### 可选软件

- **RocketMQ**: 5.3（消息队列功能需要）
- **Node.js**: 18+（前端开发需要）

### 环境检查

**Windows:**
```cmd
check-env.bat
```

**Linux/Mac:**
```bash
chmod +x check-env.sh
./check-env.sh
```

## 快速启动

### 方式一：使用启动脚本（推荐）

**Windows:**
```cmd
start.bat
```

**Linux/Mac:**
```bash
chmod +x start.sh
./start.sh
```

### 方式二：手动启动

#### 1. 创建数据库

```sql
CREATE DATABASE easy_lowcode WITH ENCODING 'UTF8';
```

#### 2. 修改配置（可选）

编辑 `easy-lowcode-startup/src/main/resources/application.yaml`：

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/easy_lowcode
    username: your_username
    password: your_password
  
  data:
    redis:
      host: localhost
      port: 6379
      password: your_redis_password
```

#### 3. 编译并运行

```bash
# 编译项目
mvn clean install -DskipTests

# 启动应用
cd easy-lowcode-startup
mvn spring-boot:run
```

#### 4. 访问应用

打开浏览器访问：http://localhost:8081

**默认账号：**
- 用户名：admin
- 密码：123456

## 项目结构

```
easy_lowcode/
├── easy-lowcode-common/          # 公共模块
│   ├── result/                   # 统一返回结果
│   ├── exception/                # 异常处理
│   └── constant/                 # 常量定义
│
├── easy-lowcode-database/        # 数据库模块
│   ├── config/                   # MyBatis Plus 配置
│   ├── entity/                   # 基础实体类
│   ├── mapper/                   # 通用 Mapper
│   └── handler/                  # 自动填充处理器
│
├── easy-lowcode-auth/            # 认证授权模块 ⭐
│   ├── entity/                   # 用户、角色、菜单等实体
│   ├── mapper/                   # 数据访问层
│   ├── service/                  # 业务逻辑层
│   └── controller/               # 控制器层
│
├── easy-lowcode-resource/        # 资源查询模块
├── easy-lowcode-dashboard/       # 可视化大屏模块
├── easy-lowcode-collector/       # 数据采集模块
├── easy-lowcode-etl/             # ETL 模块
├── easy-lowcode-gateway/         # API 网关模块
├── easy-lowcode-startup/         # 启动模块
├── README.md                     # 项目说明
├── DEVELOPMENT.md                # 开发文档
├── PROJECT_SUMMARY.md            # 项目总结
└── QUICK_START.md                # 本文档
```

## 核心功能

### 1. 认证授权（已实现）

- ✅ 用户登录/登出
- ✅ Token 管理（基于 Sa-Token）
- ✅ 获取当前用户信息
- ✅ 用户管理（CRUD）
- ✅ 角色管理（CRUD）
- ✅ 菜单管理（CRUD）
- ✅ 部门管理（CRUD）
- ✅ 权限分配
- ✅ 第三方应用管理

### 2. 数据采集（基础已搭建）

- ✅ 支持 PostgreSQL
- ✅ 支持 MySQL
- ✅ 支持 Oracle
- ✅ 支持 SQL Server
- ⏳ 支持达梦数据库
- ⏳ 多数据源动态切换
- ⏳ 数据采集任务管理

### 3. ETL 处理（基础已搭建）

- ✅ Spring Batch 集成
- ⏳ 可视化 ETL 流程设计
- ⏳ 数据抽取组件
- ⏳ 数据转换组件
- ⏳ 数据加载组件
- ⏳ 任务调度

### 4. 工作流（基础已搭建）

- ✅ Flowable 引擎集成
- ⏳ 流程设计器
- ⏳ 流程部署
- ⏳ 任务审批

### 5. 可视化大屏（基础已搭建）

- ✅ Spring AI 集成
- ⏳ 大屏设计器
- ⏳ 图表组件
- ⏳ 实时数据展示

## 开发指南

### API 测试示例

#### 登录

```bash
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'
```

响应：
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9..."
  },
  "timestamp": 1234567890
}
```

#### 获取当前用户

```bash
curl -X GET http://localhost:8081/api/auth/current \
  -H "Authorization: Bearer {your_token}"
```

### 添加新功能

详细开发指南请查看 [DEVELOPMENT.md](DEVELOPMENT.md)

**快速步骤：**

1. 创建实体类（继承 BaseEntity）
2. 创建 Mapper 接口（继承 BaseMapper）
3. 创建 Service 接口（继承 IService）
4. 创建 ServiceImpl（继承 ServiceImpl）
5. 创建 Controller（使用 @RestController）

### 代码示例

```java
// 实体类
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("xxx_table")
public class XxxEntity extends BaseEntity {
    private String name;
    private String description;
}

// Mapper
@Mapper
public interface XxxMapper extends BaseMapper<XxxEntity> {
}

// Service
public interface XxxService extends IService<XxxEntity> {
}

// ServiceImpl
@Service
@RequiredArgsConstructor
public class XxxServiceImpl extends ServiceImpl<XxxMapper, XxxEntity> 
    implements XxxService {
}

// Controller
@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
public class XxxController {
    private final XxxService xxxService;
    
    @GetMapping("/list")
    public Result<List<XxxEntity>> list() {
        return Result.success(xxxService.list());
    }
}
```

## 常见问题

### Q1: 启动时提示数据库连接失败？

**A:** 请检查：
1. PostgreSQL 是否已启动
2. 数据库 `easy_lowcode` 是否已创建
3. `application.yaml` 中的数据库配置是否正确
4. 数据库连接配置是否正确

### Q2: Redis 连接失败？

**A:** 请检查：
1. Redis 是否已启动
2. `application.yaml` 中的 Redis 配置是否正确
3. 测试连接：`redis-cli ping`（应返回 PONG）

### Q3: 登录后访问接口提示未授权？

**A:** 请检查：
1. 请求头是否包含 `Authorization: Bearer {token}`
2. Token 是否已过期
3. Sa-Token 配置是否正确

### Q4: 如何修改端口？

**A:** 编辑 `application.yaml`：
```yaml
server:
  port: 8080  # 修改为你想要的端口
```

### Q5: 如何查看 SQL 日志？

**A:** 在 `application.yaml` 中已默认开启：
```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### Q6: 忘记密码怎么办？

**A:** 直接在数据库中修改：
```sql
UPDATE sys_user 
SET password = '$2a$10$l9Z.7X9AIuzj1gk783KvcORuWBlLCGraRFMTtdAmz7DTALdS0ajjO' 
WHERE username = 'admin';
-- 密码重置为: 123456
```

## 下一步

1. 📚 阅读 [README.md](README.md) 了解项目详情
2. 🔧 阅读 [DEVELOPMENT.md](DEVELOPMENT.md) 学习开发规范
3. 📊 阅读 [PROJECT_SUMMARY.md](PROJECT_SUMMARY.md) 了解项目进度
4. 💻 开始开发您的第一个功能模块

## 技术支持

- 📖 查看完整文档
- 🐛 提交 Issue
- 💬 联系开发团队

## 许可证

MIT License

---

**祝您使用愉快！** 🎉

如有任何问题，请随时查阅文档或联系技术支持。
