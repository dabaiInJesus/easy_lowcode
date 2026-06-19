# 国产数据库支持指南

## 概述

本系统已全面支持主流国产数据库，包括达梦、人大金仓、南大通用、OceanBase、TiDB、openGauss和华为GaussDB等。

## 支持的国产数据库列表

### 1. 达梦数据库 (Dameng DM)
- **类型标识**: `dm`
- **JDBC驱动**: `dm.jdbc.driver.DmDriver`
- **兼容性**: 兼容Oracle语法
- **特点**: 
  - 使用 `SELECT 1 FROM DUAL` 进行测试查询
  - 表结构和列信息查询使用Oracle风格的系统表
  - LIMIT语法使用 `WHERE ROWNUM <= n`

### 2. 人大金仓 (Kingbase)
- **类型标识**: `kingbase`
- **JDBC驱动**: `com.kingbase8.Driver`
- **兼容性**: 兼容PostgreSQL语法
- **特点**:
  - 使用 `SELECT 1` 进行测试查询
  - 表结构和列信息查询使用PostgreSQL风格的系统表
  - 支持标准LIMIT语法

### 3. 南大通用 (GBase)
- **类型标识**: `gbase`
- **JDBC驱动**: `com.gbase.jdbc.Driver`
- **兼容性**: 兼容MySQL语法
- **特点**:
  - 使用 `SELECT 1` 进行测试查询
  - 表结构和列信息查询使用MySQL风格的INFORMATION_SCHEMA
  - 支持标准LIMIT语法

### 4. OceanBase
- **类型标识**: `oceanbase`
- **JDBC驱动**: `com.oceanbase.jdbc.Driver`
- **兼容性**: 兼容MySQL语法
- **特点**:
  - 使用 `SELECT 1` 进行测试查询
  - 表结构和列信息查询使用MySQL风格的INFORMATION_SCHEMA
  - 支持标准LIMIT语法

### 5. TiDB
- **类型标识**: `tidb`
- **JDBC驱动**: `com.mysql.cj.jdbc.Driver`（复用MySQL驱动）
- **兼容性**: 完全兼容MySQL协议
- **特点**:
  - 使用MySQL驱动和语法
  - 支持标准LIMIT语法
  - 分布式NewSQL数据库

### 6. openGauss
- **类型标识**: `opengauss`
- **JDBC驱动**: `org.opengauss.Driver`
- **兼容性**: 兼容PostgreSQL语法
- **特点**:
  - 使用 `SELECT 1` 进行测试查询
  - 表结构和列信息查询使用PostgreSQL风格的系统表
  - 支持标准LIMIT语法

### 7. 华为 GaussDB
- **类型标识**: `gaussdb`
- **JDBC驱动**: `com.huawei.gaussdb.jdbc.Driver`
- **兼容性**: 支持多种兼容模式（PostgreSQL/Oracle）
- **特点**:
  - 使用通用的INFORMATION_SCHEMA查询
  - 支持标准LIMIT语法
  - 企业级分布式数据库

### 8. 瀚高数据库 (HighGo)
- **类型标识**: `highgo`
- **JDBC驱动**: `com.highgo.jdbc.Driver`
- **兼容性**: 完全兼容PostgreSQL语法
- **特点**:
  - 使用 `SELECT 1` 进行测试查询
  - 表结构和列信息查询使用PostgreSQL风格的系统表
  - 支持标准LIMIT语法
  - 基于PostgreSQL开发的国产数据库

## 配置示例

### 达梦数据库配置
```yaml
datasource:
  name: 达梦测试库
  code: dm_test
  dbType: dm
  url: jdbc:dm://localhost:5236
  username: SYSDBA
  password: your_password
  driverClassName: dm.jdbc.driver.DmDriver
```

### 人大金仓配置
```yaml
datasource:
  name: 金仓测试库
  code: kingbase_test
  dbType: kingbase
  url: jdbc:kingbase8://localhost:54321/test
  username: SYSTEM
  password: your_password
  driverClassName: com.kingbase8.Driver
```

### GBase配置
```yaml
datasource:
  name: GBase测试库
  code: gbase_test
  dbType: gbase
  url: jdbc:gbase://localhost:5258/test
  username: root
  password: your_password
  driverClassName: com.gbase.jdbc.Driver
```

### OceanBase配置
```yaml
datasource:
  name: OceanBase测试库
  code: oceanbase_test
  dbType: oceanbase
  url: jdbc:oceanbase://localhost:2881/test
  username: root@test
  password: your_password
  driverClassName: com.oceanbase.jdbc.Driver
```

### TiDB配置
```yaml
datasource:
  name: TiDB测试库
  code: tidb_test
  dbType: tidb
  url: jdbc:mysql://localhost:4000/test
  username: root
  password: your_password
  driverClassName: com.mysql.cj.jdbc.Driver
```

### openGauss配置
```yaml
datasource:
  name: openGauss测试库
  code: opengauss_test
  dbType: opengauss
  url: jdbc:opengauss://localhost:5432/test
  username: gaussdb
  password: your_password
  driverClassName: org.opengauss.Driver
```

### GaussDB配置
```yaml
datasource:
  name: GaussDB测试库
  code: gaussdb_test
  dbType: gaussdb
  url: jdbc:gaussdb://localhost:5432/test
  username: gaussdb
  password: your_password
  driverClassName: com.huawei.gaussdb.jdbc.Driver
```

### 瀚高数据库配置
```yaml
datasource:
  name: 瀚高测试库
  code: highgo_test
  dbType: highgo
  url: jdbc:highgo://localhost:5866/highgo
  username: highgo
  password: your_password
  driverClassName: com.highgo.jdbc.Driver
```

## ⚠️ 重要：启用数据库驱动

> **注意**：`easy-lowcode-collector/pom.xml` 中的国产数据库驱动依赖**默认被注释掉**。使用前需要手动取消对应数据库驱动的注释。

在 `easy-lowcode-collector/pom.xml` 中，根据需要取消相应数据库驱动的注释：

```xml
<!-- 达梦数据库驱动 -->
<dependency>
    <groupId>com.dameng</groupId>
    <artifactId>DmJdbcDriver18</artifactId>
    <version>8.1.2.141</version>
</dependency>

<!-- 人大金仓驱动 -->
<dependency>
    <groupId>com.kingbase</groupId>
    <artifactId>kingbase8</artifactId>
    <version>8.6.0</version>
</dependency>

<!-- 其他驱动类似... -->
```

## 功能特性

### 1. 连接测试
系统支持对所有国产数据库进行连接测试，自动使用正确的测试SQL语法。

### 2. 表扫描
可以扫描数据库中的所有表，获取表名和表注释信息。

### 3. 表结构查询
可以获取指定表的详细结构信息，包括：
- 字段名
- 数据类型
- 字段注释
- 是否可空
- 主键标识

### 4. 数据预览
支持对表数据进行预览查询，自动适配不同数据库的LIMIT语法。

### 5. 动态API生成
注册表资源后，可以自动生成CRUD API接口，支持数据查询和操作。

## 注意事项

1. **驱动版本**: 请根据实际使用的数据库版本选择对应的JDBC驱动版本
2. **连接URL**: 不同数据库的连接URL格式可能有所不同，请参考官方文档
3. **字符编码**: 建议统一使用UTF-8字符编码
4. **权限配置**: 确保数据库用户具有足够的权限进行表结构查询和数据访问
5. **网络连通性**: 确保应用服务器能够访问数据库服务器

## 兼容性说明

| 数据库 | 连接测试 | 表扫描 | 结构查询 | 数据预览 | API生成 |
|-------|---------|--------|---------|---------|---------|
| 达梦DM | ✓ | ✓ | ✓ | ✓ | ✓ |
| 人大金仓 | ✓ | ✓ | ✓ | ✓ | ✓ |
| GBase | ✓ | ✓ | ✓ | ✓ | ✓ |
| OceanBase | ✓ | ✓ | ✓ | ✓ | ✓ |
| TiDB | ✓ | ✓ | ✓ | ✓ | ✓ |
| openGauss | ✓ | ✓ | ✓ | ✓ | ✓ |
| GaussDB | ✓ | ✓ | ✓ | ✓ | ✓ |
| 瀚高HighGo | ✓ | ✓ | ✓ | ✓ | ✓ |

## 技术支持

如遇到数据库连接或兼容性问题，请检查：
1. JDBC驱动是否正确加载
2. 连接URL格式是否正确
3. 用户名密码是否正确
4. 网络是否通畅
5. 数据库服务是否正常运行

## 更新日志

- **v1.1.0** (2026-05-04): 添加瀚高数据库支持
- **v1.0.0** (2026-05-04): 初始版本，支持7种主流国产数据库
