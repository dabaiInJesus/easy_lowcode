# 低代码平台开发文档

## 项目架构概述

本项目采用聚合工程架构，将不同功能模块拆分为独立的Maven模块，便于维护和扩展。

### 模块依赖关系

```
easy-lowcode-common (公共模块)
    ↑
easy-lowcode-database (数据库模块)
    ↑
    ├── easy-lowcode-auth (认证授权模块)
    ├── easy-lowcode-resource (资源查询模块)
    ├── easy-lowcode-dashboard (可视化大屏模块)
    ├── easy-lowcode-collector (数据采集模块)
    └── easy-lowcode-etl (ETL模块)
    ↑
easy-lowcode-startup (启动模块 - 整合所有业务模块)

easy-lowcode-gateway (网关模块 - 独立部署)
```

## 环境准备

### 1. 安装必需软件

- **JDK 21**: https://adoptium.net/
- **Maven 3.6+**: https://maven.apache.org/
- **PostgreSQL 17**: https://www.postgresql.org/
- **Redis 6+**: https://redis.io/
- **RocketMQ 5.3**: https://rocketmq.apache.org/

### 2. 配置环境变量

```bash
# Windows
JAVA_HOME=C:\Program Files\Java\jdk-21
MAVEN_HOME=C:\Program Files\Apache\maven

# Linux/Mac
export JAVA_HOME=/usr/lib/jvm/java-21
export MAVEN_HOME=/opt/maven
```

## 数据库配置

### 1. 创建数据库

```sql
CREATE DATABASE easy_lowcode WITH ENCODING 'UTF8';
```

数据库使用 Liquibase 自动管理迁移，启动时自动执行，无需手动执行 SQL 脚本。

### 3. 验证数据

```sql
-- 查看用户表
SELECT * FROM sys_user;

-- 查看菜单表
SELECT * FROM sys_menu;
```

## Redis 配置

### Windows

1. 下载 Redis: https://github.com/microsoftarchive/redis/releases
2. 解压并运行 `redis-server.exe`
3. 默认端口: 6379

### Linux/Mac

```bash
# Ubuntu/Debian
sudo apt-get install redis-server

# CentOS
sudo yum install redis

# Mac
brew install redis

# 启动 Redis
redis-server
```

## RocketMQ 配置

### 快速启动（单机模式）

```bash
# 1. 下载 RocketMQ
wget https://archive.apache.org/dist/rocketmq/5.3.0/rocketmq-all-5.3.0-bin-release.zip
unzip rocketmq-all-5.3.0-bin-release.zip

# 2. 启动 NameServer
cd rocketmq-5.3.0
nohup sh bin/mqnamesrv &

# 3. 启动 Broker
nohup sh bin/mqbroker -n localhost:9876 &

# 4. 验证
sh bin/mqadmin clusterList -n localhost:9876
```

### Windows

```cmd
:: 启动 NameServer
start bin\mqnamesrv.cmd

:: 启动 Broker
start bin\mqbroker.cmd -n localhost:9876
```

## 编译和运行

### 1. 编译项目

```bash
# 进入项目根目录
cd D:\code\easy_lowcode

# 清理并编译
mvn clean install

# 跳过测试编译
mvn clean install -DskipTests
```

### 2. 运行应用

```bash
# 方式一：使用 Maven
cd easy-lowcode-startup
mvn spring-boot:run

# 方式二：打包后运行
mvn clean package
java -jar easy-lowcode-startup/target/easy-lowcode-startup-1.0.0-SNAPSHOT.jar
```

### 3. 验证启动

访问 http://localhost:8081/actuator/health

应该返回：
```json
{
  "status": "UP"
}
```

## API 测试

### 使用 curl 测试

```bash
# 1. 登录获取 token
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"123456"}'

# 响应示例
# {"code":200,"message":"登录成功","data":{"token":"xxx"},"timestamp":1234567890}

# 2. 使用 token 访问受保护接口
curl -X GET http://localhost:8081/api/auth/current \
  -H "Authorization: Bearer {your_token}"
```

### 使用 Postman 测试

1. 导入项目中的 API 集合（待创建）
2. 设置环境变量：
   - `base_url`: http://localhost:8081
   - `token`: {{从登录接口获取}}
3. 发送请求

## 开发指南

### 添加新的业务模块

#### 1. 创建模块结构

```bash
mkdir -p easy-lowcode-xxx/src/main/java/com/dabai/easy_lowcode/xxx
mkdir -p easy-lowcode-xxx/src/main/resources
```

#### 2. 创建 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>com.dabai</groupId>
        <artifactId>easy-lowcode</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>easy-lowcode-xxx</artifactId>
    <packaging>jar</packaging>
    <name>easy-lowcode-xxx</name>
    <description>模块描述</description>

    <dependencies>
        <dependency>
            <groupId>com.dabai</groupId>
            <artifactId>easy-lowcode-database</artifactId>
        </dependency>
    </dependencies>
</project>
```

#### 3. 在根 pom.xml 中注册模块

```xml
<modules>
    <!-- 其他模块 -->
    <module>easy-lowcode-xxx</module>
</modules>
```

#### 4. 在 startup 模块中添加依赖

```xml
<dependency>
    <groupId>com.dabai</groupId>
    <artifactId>easy-lowcode-xxx</artifactId>
</dependency>
```

### 创建实体类

```java
package com.dabai.easy_lowcode.xxx.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("xxx_table")
public class XxxEntity extends BaseEntity {
    
    private String name;
    private String description;
    // 其他字段...
}
```

### 创建 Mapper

```java
package com.dabai.easy_lowcode.xxx.mapper;

import com.dabai.easy_lowcode.database.mapper.BaseMapper;
import com.dabai.easy_lowcode.xxx.entity.XxxEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface XxxMapper extends BaseMapper<XxxEntity> {
}
```

### 创建 Service

```java
package com.dabai.easy_lowcode.xxx.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.xxx.entity.XxxEntity;

public interface XxxService extends IService<XxxEntity> {
    // 自定义业务方法
}
```

### 创建 ServiceImpl

```java
package com.dabai.easy_lowcode.xxx.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.xxx.entity.XxxEntity;
import com.dabai.easy_lowcode.xxx.mapper.XxxMapper;
import com.dabai.easy_lowcode.xxx.service.XxxService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class XxxServiceImpl extends ServiceImpl<XxxMapper, XxxEntity> implements XxxService {
    // 实现自定义业务方法
}
```

### 创建 Controller

```java
package com.dabai.easy_lowcode.xxx.controller;

import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.xxx.entity.XxxEntity;
import com.dabai.easy_lowcode.xxx.service.XxxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/xxx")
@RequiredArgsConstructor
public class XxxController {
    
    private final XxxService xxxService;
    
    @GetMapping("/list")
    public Result<List<XxxEntity>> list() {
        return Result.success(xxxService.list());
    }
    
    @GetMapping("/{id}")
    public Result<XxxEntity> getById(@PathVariable Long id) {
        return Result.success(xxxService.getById(id));
    }
    
    @PostMapping
    public Result<Void> save(@RequestBody XxxEntity entity) {
        xxxService.save(entity);
        return Result.success();
    }
    
    @PutMapping
    public Result<Void> update(@RequestBody XxxEntity entity) {
        xxxService.updateById(entity);
        return Result.success();
    }
    
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        xxxService.removeById(id);
        return Result.success();
    }
}
```

## 常见问题

### 1. 编译错误

**问题**: 找不到符号或类

**解决**:
```bash
# 清理并重新编译
mvn clean install -U

# 检查 JDK 版本
java -version  # 应该是 21+
```

### 2. 数据库连接失败

**问题**: Cannot connect to database

**解决**:
- 检查 PostgreSQL 是否启动
- 检查 `application.yaml` 中的数据库配置
- 确认数据库已创建并执行了初始化脚本

### 3. Redis 连接失败

**问题**: Cannot connect to Redis

**解决**:
- 检查 Redis 是否启动
- 检查 `application.yaml` 中的 Redis 配置
- 测试连接: `redis-cli ping` (应返回 PONG)

### 4. Sa-Token 配置问题

**问题**: Token 验证失败

**解决**:
- 检查请求头是否包含 `Authorization: Bearer {token}`
- 确认 Sa-Token 配置正确
- 查看日志输出

## 调试技巧

### 1. 开启 SQL 日志

在 `application.yaml` 中：

```yaml
mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 2. 开启 Sa-Token 日志

```yaml
sa-token:
  is-log: true
```

### 3. 远程调试

```bash
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
  -jar easy-lowcode-startup/target/easy-lowcode-startup-1.0.0-SNAPSHOT.jar
```

然后在 IDE 中配置远程调试，连接到 5005 端口。

## 性能优化建议

1. **数据库优化**
   - 为常用查询字段添加索引
   - 使用连接池（HikariCP 已默认配置）
   - 定期清理逻辑删除的数据

2. **Redis 优化**
   - 合理设置过期时间
   - 使用连接池
   - 避免大 key

3. **应用优化**
   - 启用 GZIP 压缩
   - 使用缓存减少数据库查询
   - 异步处理耗时操作

## 部署指南

### 生产环境配置

创建 `application-prod.yaml`:

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://prod-db:5432/easy_lowcode
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
  
  data:
    redis:
      host: ${REDIS_HOST}
      port: 6379
      password: ${REDIS_PASSWORD}

logging:
  level:
    root: WARN
    com.dabai.easy_lowcode: INFO
  file:
    name: /var/log/easy-lowcode/app.log
```

### Docker 部署 ✅

```dockerfile
FROM openjdk:21-jdk-slim
WORKDIR /app
COPY easy-lowcode-startup/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

## 下一步工作

1. ✅ 后端聚合工程架构搭建
2. ✅ 基础模块创建
3. ✅ 认证授权功能实现
4. ✅ 前端项目开发（Vue 3 + Vite）
5. ⏳ 代码生成器开发
6. ⏳ 表单设计器开发
7. ⏳ 流程设计器集成
8. ⏳ ETL 功能完善
9. ⏳ 数据采集功能完善
10. ⏳ 可视化大屏功能开发

## 技术支持

如有问题，请：
1. 查看本文档
2. 搜索相关技术文档
3. 提交 Issue
4. 联系开发团队

祝开发愉快！🚀
