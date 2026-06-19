# Liquibase 数据库版本管理指南

## 概述

本项目使用 Liquibase 进行数据库版本管理，自动化执行数据库 schema 变更和数据初始化。

## 配置信息

### 数据库连接

- **数据库**: PostgreSQL
- **地址**: 127.0.0.1:5432
- **数据库名**: easy_lowcode
- **用户名**: postgres
- **密码**: ${DB_PASSWORD:postgres123}

### Liquibase 配置

配置文件位置：`easy-lowcode-startup/src/main/resources/application.yaml`

```yaml
spring:
  liquibase:
    enabled: true
    change-log: classpath:db/changelog/db.changelog-master.xml
    contexts: dev
```

## 文件结构

```
easy-lowcode-database/src/main/resources/db/changelog/
├── db.changelog-master.xml      # 主配置文件
├── 001-init-schema.xml          # 表结构定义
└── 002-init-data.xml            # 初始数据
```

## 快速开始

### 1. 创建数据库

```bash
psql -U postgres -c "CREATE DATABASE easy_lowcode WITH ENCODING 'UTF8';"
```

### 2. 启动应用

```bash
# Windows
start.bat

# Linux/Mac
./start.sh
```

Liquibase 会自动：
- 检查数据库版本
- 执行未应用的变更集
- 创建必要的表结构
- 插入初始数据

### 3. 验证

启动成功后，数据库中会自动创建以下表：

**业务表：**
- sys_user（用户表）
- sys_role（角色表）
- sys_menu（菜单表）
- sys_dept（部门表）
- sys_app（第三方应用表）
- sys_user_role（用户角色关联表）
- sys_role_menu（角色菜单关联表）

**Liquibase 管理表：**
- databasechangelog（变更记录表）
- databasechangeloglock（锁表）

## 添加新的数据库变更

### 步骤 1：创建新的 changelog 文件

在 `easy-lowcode-database/src/main/resources/db/changelog/` 目录下创建新文件：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<databaseChangeLog
        xmlns="http://www.liquibase.org/xml/ns/dbchangelog"
        xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        xsi:schemaLocation="http://www.liquibase.org/xml/ns/dbchangelog
        http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.25.xsd">

    <changeSet id="001-add-new-table" author="your-name">
        <createTable tableName="your_table" remarks="你的表说明">
            <column name="id" type="BIGINT">
                <constraints primaryKey="true" nullable="false"/>
            </column>
            <column name="name" type="VARCHAR(100)" remarks="名称"/>
            <!-- 其他字段 -->
        </createTable>
    </changeSet>

</databaseChangeLog>
```

### 步骤 2：在主配置文件中引用

编辑 `db.changelog-master.xml`，添加新的 include：

```xml
<databaseChangeLog>
    <!-- 现有的 include -->
    <include file="db/changelog/001-init-schema.xml" relativeToChangelogFile="false"/>
    <include file="db/changelog/002-init-data.xml" relativeToChangelogFile="false"/>
    
    <!-- 新增的 include -->
    <include file="db/changelog/003-your-change.xml" relativeToChangelogFile="false"/>
</databaseChangeLog>
```

### 步骤 3：重启应用

重启应用后，Liquibase 会自动执行新的变更集。

## 常用变更操作

### 创建表

```xml
<changeSet id="001-create-table" author="dabai">
    <createTable tableName="example_table" remarks="示例表">
        <column name="id" type="BIGINT">
            <constraints primaryKey="true" nullable="false"/>
        </column>
        <column name="name" type="VARCHAR(100)" remarks="名称">
            <constraints nullable="false"/>
        </column>
        <column name="status" type="INTEGER" defaultValueNumeric="1" remarks="状态"/>
        <column name="create_time" type="TIMESTAMP" defaultValueComputed="CURRENT_TIMESTAMP"/>
    </createTable>
</changeSet>
```

### 添加字段

```xml
<changeSet id="002-add-column" author="dabai">
    <addColumn tableName="sys_user">
        <column name="age" type="INTEGER" remarks="年龄"/>
    </addColumn>
</changeSet>
```

### 修改字段

```xml
<changeSet id="003-modify-column" author="dabai">
    <modifyDataType tableName="sys_user" columnName="phone" newDataType="VARCHAR(30)"/>
</changeSet>
```

### 删除字段

```xml
<changeSet id="004-drop-column" author="dabai">
    <dropColumn tableName="sys_user" columnName="old_field"/>
</changeSet>
```

### 创建索引

```xml
<changeSet id="005-create-index" author="dabai">
    <createIndex indexName="idx_example_name" tableName="example_table">
        <column name="name"/>
    </createIndex>
</changeSet>
```

### 添加唯一约束

```xml
<changeSet id="006-add-unique" author="dabai">
    <addUniqueConstraint tableName="example_table" columnNames="name" constraintName="uk_example_name"/>
</changeSet>
```

### 插入数据

```xml
<changeSet id="007-insert-data" author="dabai">
    <insert tableName="example_table">
        <column name="id" valueNumeric="1"/>
        <column name="name" value="示例数据"/>
        <column name="status" valueNumeric="1"/>
    </insert>
</changeSet>
```

### 更新数据

```xml
<changeSet id="008-update-data" author="dabai">
    <update tableName="example_table">
        <column name="status" valueNumeric="0"/>
        <where>id = 1</where>
    </update>
</changeSet>
```

### 删除数据

```xml
<changeSet id="009-delete-data" author="dabai">
    <delete tableName="example_table">
        <where>id = 1</where>
    </delete>
</changeSet>
```

### 执行原生 SQL

```xml
<changeSet id="010-execute-sql" author="dabai">
    <sql>
        ALTER TABLE example_table ADD COLUMN description TEXT;
    </sql>
</changeSet>
```

## 变更集属性说明

### changeSet 属性

- **id**: 变更集唯一标识（建议格式：序号-描述）
- **author**: 作者名称
- **context**: 执行上下文（如：dev, test, prod）
- **labels**: 标签，用于分组
- **runAlways**: 是否每次都执行（默认 false）
- **runOnChange**: 内容变化时是否重新执行（默认 false）

### 预条件（preConditions）

```xml
<changeSet id="001-with-precondition" author="dabai">
    <preConditions onFail="MARK_RAN">
        <not>
            <tableExists tableName="example_table"/>
        </not>
    </preConditions>
    
    <createTable tableName="example_table">
        <!-- 表定义 -->
    </createTable>
</changeSet>
```

预条件失败处理策略：
- **HALT**: 停止执行（默认）
- **CONTINUE**: 继续执行下一个变更集
- **MARK_RAN**: 标记为已执行但不实际执行
- **WARN**: 输出警告并继续

## 回滚操作

### 自动回滚

Liquibase 可以自动回滚某些操作（如 createTable、addColumn 等）。

### 手动指定回滚

```xml
<changeSet id="001-with-rollback" author="dabai">
    <createTable tableName="example_table">
        <column name="id" type="BIGINT">
            <constraints primaryKey="true"/>
        </column>
    </createTable>
    
    <rollback>
        <dropTable tableName="example_table"/>
    </rollback>
</changeSet>
```

### 执行回滚

```bash
# 回滚最后一个变更集
mvn liquibase:rollback -Dliquibase.rollbackCount=1

# 回滚到指定标签
mvn liquibase:rollback -Dliquibase.rollbackTag=v1.0

# 回滚所有变更
mvn liquibase:rollback -Dliquibase.rollbackDate=2024-01-01
```

## 常用 Maven 命令

```bash
# 查看待执行的变更集
mvn liquibase:updateSQL

# 查看数据库状态
mvn liquibase:status

# 生成变更日志
mvn liquibase:history

# 验证 changelog 文件格式
mvn liquibase:validate

# 生成数据库文档
mvn liquibase:dbDoc -Dliquibase.outputDirectory=target/dbdoc

# 差异对比（需要配置 referenceUrl）
mvn liquibase:diff
```

## 环境区分

通过 `contexts` 属性区分不同环境：

```xml
<changeSet id="001-dev-data" author="dabai" context="dev">
    <!-- 只在开发环境执行 -->
    <insert tableName="example_table">
        <column name="id" valueNumeric="1"/>
        <column name="name" value="测试数据"/>
    </insert>
</changeSet>

<changeSet id="002-prod-data" author="dabai" context="prod">
    <!-- 只在生产环境执行 -->
    <insert tableName="example_table">
        <column name="id" valueNumeric="1"/>
        <column name="name" value="生产数据"/>
    </insert>
</changeSet>
```

配置文件中指定：

```yaml
spring:
  liquibase:
    contexts: dev  # 或 test, prod
```

## 最佳实践

### 1. 变更集命名规范

- 使用序号前缀：`001-`, `002-`, `003-`
- 清晰的描述：`create-user-table`, `add-email-column`
- 包含作者信息

### 2. 原子性原则

- 每个变更集只做一件事
- 避免在一个变更集中执行多个不相关的操作

### 3. 幂等性

- 变更集应该可以安全地重复执行
- 使用 preConditions 检查前置条件

### 4. 不要修改已执行的变更集

- 一旦变更集被执行，就不要修改它
- 如需修改，创建新的变更集

### 5. 添加注释

- 为表和字段添加 remarks 注释
- 为复杂的变更添加说明

### 6. 测试变更

- 在开发环境充分测试
- 使用 `updateSQL` 预览将要执行的 SQL

## 故障排查

### 问题 1：变更集执行失败

**症状**：应用启动时报错

**解决**：
1. 查看错误日志，定位失败的变更集
2. 检查 SQL 语法是否正确
3. 检查是否有依赖关系问题
4. 手动修复数据库后，删除 databasechangelog 中的记录

### 问题 2：锁表问题

**症状**：提示数据库被锁定

**解决**：
```sql
-- 清除锁
DELETE FROM databasechangeloglock;
```

### 问题 3：重复执行变更集

**症状**：同一个变更集被执行多次

**解决**：
1. 检查 changeSet 的 id 和 author 是否唯一
2. 检查 databasechangelog 表中的记录

### 问题 4：跳过某个变更集

**解决**：
```sql
-- 手动标记为已执行
INSERT INTO databasechangelog (id, author, filename, dateexecuted, orderexecuted, exectype, md5sum)
VALUES ('001-skip-this', 'dabai', 'db/changelog/xxx.xml', NOW(), 1, 'EXECUTED', 'skip');
```

## 参考资料

- [Liquibase 官方文档](https://docs.liquibase.com/)
- [Liquibase XML 格式参考](https://docs.liquibase.com/concepts/changelogs/xml-format.html)
- [PostgreSQL 数据类型](https://www.postgresql.org/docs/current/datatype.html)

---

**提示**：Liquibase 会在应用启动时自动执行，无需手动干预。只需确保数据库已创建，其他交给 Liquibase 即可！
