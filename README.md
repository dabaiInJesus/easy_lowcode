# Easy Lowcode - 低代码平台

一个基于 Spring Boot 3 + Vue 3 的前后端分离低代码开发平台。

## 技术栈

### 后端技术
- **核心框架**: Spring Boot 3.5.5
- **ORM框架**: MyBatis Plus 3.5.5
- **权限认证**: Sa-Token 1.37.0
- **工作流引擎**: Flowable 7.0.1
- **AI集成**: Spring AI 1.1.4
- **数据库**: PostgreSQL 17
- **消息队列**: RocketMQ
- **批处理**: Spring Batch
- **构建工具**: Maven

### 前端技术（待实现）
- **核心框架**: Vue 3.4+
- **构建工具**: Vite
- **UI组件库**: Element Plus
- **样式框架**: TailwindCSS 3.4+
- **开发语言**: TypeScript

## 项目结构

```
easy_lowcode/
├── easy-lowcode-common          # 公共模块 - 工具类、常量、异常处理等
├── easy-lowcode-database        # 数据库模块 - MyBatisPlus配置、通用Mapper等
├── easy-lowcode-auth            # 认证授权模块 - Sa-Token、用户/角色/权限管理
├── easy-lowcode-resource        # 资源查询模块
├── easy-lowcode-dashboard       # 可视化大屏模块
├── easy-lowcode-collector       # 数据采集及处理模块 - 支持各种主流数据库
├── easy-lowcode-etl             # ETL模块 - SpringBatch技术支持页面可配置
├── easy-lowcode-gateway         # API网关模块 - 统一入口
├── easy-lowcode-startup         # 启动模块 - 应用启动入口
└── sql/                         # 数据库脚本
    └── init.sql                 # 初始化脚本
```

## 模块说明

### 1. easy-lowcode-common (公共模块)
- 统一返回结果封装 (Result, PageResult)
- 全局异常处理
- 通用常量定义
- 业务异常类

### 2. easy-lowcode-database (数据库模块)
- MyBatis Plus 配置
- 基础实体类 (BaseEntity)
- 自动填充处理器
- 通用 Mapper 接口

### 3. easy-lowcode-auth (认证授权模块)
**核心功能：**
- 用户登录/登出
- 部门管理
- 角色管理
- 用户管理
- 菜单管理
- 权限管理
- 角色授权
- 用户授权
- 第三方应用管理（支持从平台跳转到第三方app）

**技术实现：**
- Sa-Token 权限认证
- Flowable 工作流引擎
- Redis 会话存储

### 4. easy-lowcode-resource (资源查询模块)
- 动态数据源查询
- 自定义SQL执行
- 数据权限控制

### 5. easy-lowcode-dashboard (可视化大屏模块)
- 大屏模板管理
- 图表组件库
- 实时数据展示
- Spring AI 智能分析

### 6. easy-lowcode-collector (数据采集及处理模块)
**支持的数据库：**
- PostgreSQL
- MySQL
- Oracle
- SQL Server
- 达梦数据库（国产）
- 其他主流数据库

**功能特性：**
- 多数据源配置
- 数据采集任务管理
- 数据同步
- RocketMQ 消息推送

### 7. easy-lowcode-etl (ETL模块)
- 基于 Spring Batch
- 页面可配置ETL任务
- 数据抽取 (Extract)
- 数据转换 (Transform)
- 数据加载 (Load)
- 任务调度管理

### 8. easy-lowcode-gateway (API网关模块)
- 统一API入口
- 路由转发
- 权限校验
- 限流熔断

### 9. easy-lowcode-startup (启动模块)
- 应用启动入口
- 整合所有业务模块
- 统一配置管理

## 快速开始

### 环境要求
- JDK 21+
- Maven 3.6+
- PostgreSQL 17
- Redis 6+
- RocketMQ 4.9+

### 数据库初始化

1. 创建数据库
```sql
CREATE DATABASE easy_lowcode WITH ENCODING 'UTF8';
```

2. 执行初始化脚本
```bash
psql -U postgres -d easy_lowcode -f sql/init.sql
```

### 配置文件

修改 `easy-lowcode-startup/src/main/resources/application.yaml` 中的配置：

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
  
  rocketmq:
    name-server: localhost:9876
```

### 启动应用

```bash
# 编译项目
mvn clean install

# 启动应用
cd easy-lowcode-startup
mvn spring-boot:run
```

访问 http://localhost:8080

### 默认账号
- 用户名: admin
- 密码: admin123

## API文档

### 认证接口

#### 登录
```http
POST /api/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "admin123"
}
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

#### 获取当前用户信息
```http
GET /api/auth/current
Authorization: Bearer {token}
```

#### 登出
```http
POST /api/auth/logout
Authorization: Bearer {token}
```

## 开发指南

### 添加新模块

1. 在根 pom.xml 中添加模块声明
2. 创建模块目录和 pom.xml
3. 在 startup 模块中添加依赖
4. 编写业务代码

### 代码规范

- 使用 Lombok 简化代码
- 统一使用 Result 封装返回结果
- 异常统一由 GlobalExceptionHandler 处理
- 实体类继承 BaseEntity
- Mapper 继承 BaseMapper

### 权限控制

使用 Sa-Token 注解：

```java
@SaCheckLogin  // 需要登录
@SaCheckRole("admin")  // 需要admin角色
@SaCheckPermission("system:user:list")  // 需要特定权限
```

## 后续规划

1. **前端项目**: 创建 Vue 3 + Vite + Element Plus 前端项目
2. **代码生成器**: 根据数据库表自动生成前后端代码
3. **表单设计器**: 可视化表单设计
4. **流程设计器**: BPMN 流程设计
5. **报表设计器**: 可视化报表设计
6. **大屏设计器**: 拖拽式大屏设计
7. **API设计器**: RESTful API 可视化配置

## 许可证

MIT License

## 联系方式

如有问题，请提交 Issue 或联系开发团队。
