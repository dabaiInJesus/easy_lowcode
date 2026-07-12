# Easy Lowcode 质量保障与 CI/CD 全面规划

> 版本：v1.0  
> 日期：2026-07-11  
> 状态：待评审

---

## 目录

1. [现状总览](#一现状总览)
2. [文档体系建设](#二文档体系建设)
3. [测试用例设计与执行](#三测试用例设计与执行)
4. [代码质量检测机制](#四代码质量检测机制)
5. [CI/CD 流程优化](#五cicd-流程优化)
6. [实施路线图](#六实施路线图)
7. [验收标准](#七验收标准)

---

## 一、现状总览

### 1.1 当前基线数据

| 维度 | 当前状态 | 目标状态 |
|------|---------|---------|
| 文档数量 | 30 个 MD 文件 | 40+ 个，覆盖全生命周期 |
| 后端测试覆盖率 | ~19%（144 个方法，36 个文件） | ≥ 80% |
| 前端测试覆盖率 | ~1.5%（5 个文件） | ≥ 70% |
| 后端静态分析 | 仅 JaCoCo | + Checkstyle + SpotBugs + PMD |
| 前端代码格式化 | 无 Prettier | ESLint + Prettier |
| CI 前端检查 | 软失败（continue-on-error） | 硬失败 |
| 安全漏洞 | 8 项 P0（部分已修复） | 0 项 P0/P1 |
| 部署流水线 | 仅 Docker 构建 | 多环境自动部署 |

### 1.2 已识别关键问题

**安全（代码评审报告）**：
- P0-1：硬编码 AES 密钥 + 静态 GCM nonce（已修复）
- P0-2：用户提交 SQL 未做 SELECT-only 校验（未修复）
- P0-3：生成式 API 默认为无需认证（未修复）
- P0-4：CORS 通配 + 凭据开放（已修复）
- P0-5：AI 配置接口返回明文密钥（已修复）
- P0-6：JWT 存于 localStorage（已修复为 sessionStorage）
- P0-7：DataSource 测试连接接受任意 JDBC URL（未修复）
- P0-8：Gateway Auth 白名单使用前缀匹配（未修复）

**测试**：
- Controller 层 MockMvc 测试极度缺失
- 无 Testcontainers 集成测试
- 无 E2E 测试
- 前端测试几乎为零

**CI/CD**：
- 前端 `vue-tsc` 和 `npm test` 使用 `continue-on-error: true`
- 无多环境部署流水线
- 无自动化回滚机制
- 无性能测试环节

---

## 二、文档体系建设

### 2.1 文档架构总览

```
docs/
├── 01-需求文档.md              ✅ 已有
├── 02-技术架构文档.md          ✅ 已有
├── 03-历史变更文档.md          ✅ 已有
├── 04-质量保障与CI-CD规划.md   🆕 本文档
├── 05-用户手册.md              📋 待创建
├── 06-运维手册.md              📋 待创建
├── 07-API参考文档.md           📋 待创建（基于 OpenAPI 自动生成）
├── 08-数据库设计文档.md        📋 待创建
├── 09-安全架构文档.md          📋 待创建
├── API_DETAILED_GUIDE.md       ✅ 已有
├── API_ERROR_CODES.md          ✅ 已有
├── architecture.md             📋 待创建（AGENTS.md 引用但缺失）
├── review/
│   └── code-review-report.md   ✅ 已有
├── compose/
│   ├── specs/                  ✅ 已有（设计规格）
│   └── plans/                  ✅ 已有（实施计划）
└── adr/                        📋 待创建（架构决策记录）
    └── 001-use-postgresql.md
```

### 2.2 待创建文档详细说明

#### 2.2.1 用户手册（docs/05-用户手册.md）

| 章节 | 内容 | 字数 | 优先级 |
|------|------|------|--------|
| 快速入门 | 5 分钟上手教程：登录、创建资源、查看数据 | ~1000 | P0 |
| 系统管理 | 用户/角色/菜单/部门管理操作指南 | ~1500 | P0 |
| 数据采集 | 数据源配置、API 注册、全文检索 | ~2000 | P0 |
| 资源管理 | configJson 配置、处理器链、SQL 模板 | ~2500 | P0 |
| 可视化大屏 | 大屏设计器、图表管理、SQL 查询 | ~2000 | P1 |
| AI 对话 | 多 Provider 配置、Agent 使用 | ~1500 | P1 |
| ETL 任务 | 数据同步、转换规则配置 | ~1500 | P1 |
| 常见问题 | FAQ、故障排查 | ~1000 | P1 |

#### 2.2.2 运维手册（docs/06-运维手册.md）

| 章节 | 内容 | 优先级 |
|------|------|--------|
| 环境要求 | JDK/Node/PostgreSQL/Redis/RocketMQ 版本与配置 | P0 |
| 部署指南 | Docker Compose / K8s / 裸机部署步骤 | P0 |
| 配置参考 | 全部环境变量、application.yaml 参数说明 | P0 |
| 监控告警 | Prometheus + Grafana 配置，关键指标 | P1 |
| 备份恢复 | 数据库备份策略、灾难恢复流程 | P1 |
| 日志管理 | 日志格式、级别、轮转、集中收集 | P1 |
| 扩容指南 | 水平扩展、负载均衡、会话共享 | P2 |
| 安全加固 | HTTPS、防火墙、密钥轮换、审计 | P1 |

#### 2.2.3 API 参考文档（docs/07-API参考文档.md）

基于 SpringDoc OpenAPI 自动生成，补充以下内容：

| 内容 | 说明 |
|------|------|
| 接口分组索引 | 按模块（auth/ai/resource/dashboard/collector/etl）分组 |
| 认证说明 | JWT 获取、刷新、过期处理 |
| 通用错误码 | 与 API_ERROR_CODES.md 联动 |
| 请求示例 | 每个接口的 curl 示例 |
| 响应示例 | 成功/失败响应 JSON |

#### 2.2.4 数据库设计文档（docs/08-数据库设计文档.md）

| 内容 | 说明 |
|------|------|
| ER 图 | 核心表关系图（sys_user/sys_role/sys_menu 等） |
| 表结构说明 | 每个表的字段、类型、约束、索引 |
| 数据字典 | 枚举字段含义（status/deleted/menu_type） |
| 迁移记录 | Liquibase changelog 索引 |

#### 2.2.5 安全架构文档（docs/09-安全架构文档.md）

| 内容 | 说明 |
|------|------|
| 认证流程 | JWT 签发、验证、刷新、黑名单 |
| 授权模型 | RBAC 权限矩阵、数据权限 |
| 数据加密 | AES 加密方案、密钥管理 |
| 安全配置 | CORS、CSRF、XSS 防护、SQL 注入防护 |
| 审计日志 | 操作记录、敏感数据访问记录 |
| 安全基线 | 生产部署前安全检查清单 |

#### 2.2.6 架构决策记录（docs/adr/）

| 编号 | 标题 | 说明 |
|------|------|------|
| 001 | 选用 PostgreSQL | 关系型数据库选型理由 |
| 002 | 选用 MyBatis Plus | ORM 框架选型理由 |
| 003 | 选用 JWT + Spring Security | 认证方案选型理由 |
| 004 | 选用 Liquibase | 数据库迁移工具选型理由 |
| 005 | 选用 Vue 3 + Element Plus | 前端框架选型理由 |

### 2.3 文档维护规范

| 规范 | 说明 |
|------|------|
| 版本管理 | 所有文档纳入 Git 版本控制，与代码同步更新 |
| 评审机制 | 重大文档变更需 PR 评审 |
| 更新触发 | 功能变更 → 同步更新对应文档；每季度全量审查 |
| 语言要求 | 全部使用中文 |
| 格式要求 | 统一使用 Markdown，遵循 GFM 规范 |
| 链接检查 | CI 中集成 markdown-link-check，防止死链 |

### 2.4 文档任务分解

| 编号 | 任务 | 产出物 | 预估工时 | 优先级 | 前置依赖 |
|------|------|--------|---------|--------|---------|
| DOC-01 | 创建 architecture.md | docs/architecture.md | 2h | P0 | 无 |
| DOC-02 | 编写用户手册 | docs/05-用户手册.md | 8h | P0 | 核心功能稳定 |
| DOC-03 | 编写运维手册 | docs/06-运维手册.md | 6h | P0 | 部署流程确定 |
| DOC-04 | 编写 API 参考文档 | docs/07-API参考文档.md | 4h | P1 | OpenAPI 配置完善 |
| DOC-05 | 编写数据库设计文档 | docs/08-数据库设计文档.md | 3h | P1 | 无 |
| DOC-06 | 编写安全架构文档 | docs/09-安全架构文档.md | 4h | P1 | P0 安全修复完成 |
| DOC-07 | 创建架构决策记录 | docs/adr/001-005 | 5h | P2 | 无 |
| DOC-08 | 更新 AGENTS.md | 补充安全/测试/部署规范 | 2h | P0 | 无 |
| DOC-09 | CI 集成文档链接检查 | .github/workflows/ci.yml | 1h | P2 | 文档基本完成 |

---

## 三、测试用例设计与执行

### 3.1 测试金字塔策略

```
        ┌──────────┐
        │  E2E 测试  │  ~10%  (Playwright)
        ├──────────┤
        │ 集成测试   │  ~20%  (Testcontainers + MockMvc)
        ├──────────┤
        │ 单元测试   │  ~70%  (JUnit + Vitest)
        └──────────┘
```

### 3.2 后端测试体系

#### 3.2.1 单元测试（JUnit 5 + Mockito）

**覆盖率目标**：行覆盖率 ≥ 80%，分支覆盖率 ≥ 70%

**测试范围**：
- Service 层：所有公开方法
- 工具类：所有公开方法
- 处理器：所有 Processor 实现
- 配置类：关键配置逻辑

**测试基类设计**：

```java
// 共享测试基类
@SpringBootTest
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class BaseServiceTest {
    // 统一 Mock 工具
    // 统一断言方法
    // 测试数据工厂
}
```

**各模块测试计划**：

| 模块 | 当前测试文件 | 目标测试文件 | 新增覆盖点 |
|------|------------|------------|-----------|
| common | 8 | 12 | EncryptUtil v1 格式兼容、SqlValidator |
| database | 1 | 3 | MyBatisPlus 自动填充、Liquibase 迁移验证 |
| auth | 10 | 18 | 认证流程、权限校验、Token 黑名单 |
| ai | 4 | 8 | 多 Provider 切换、Agent 工具调用 |
| resource | 4 | 10 | SQL 构建、处理器链、模板引擎 |
| dashboard | 4 | 8 | SQL 引擎、图表推荐、缓存策略 |
| collector | 2 | 6 | 数据源连接池、API 注册鉴权 |
| etl | 3 | 7 | 批处理流程、转换规则、事务边界 |
| gateway | 1 | 3 | 路由规则、鉴权过滤器 |
| startup | 1 | 2 | .env 加载、启动配置 |

#### 3.2.2 集成测试（Testcontainers + MockMvc）

**目标**：每个 Controller 至少 2 个集成测试用例

**基础设施**：

```yaml
# 测试环境配置
spring:
  datasource:
    url: jdbc:tc:postgresql:17:///easy_lowcode_test  # Testcontainers JDBC URL
  redis:
    host: ${REDIS_HOST:localhost}
```

**测试范围**：

| 模块 | 集成测试 | 测试场景 |
|------|---------|---------|
| auth | AuthControllerIntegrationTest | 登录成功/失败、Token 过期、权限不足 |
| auth | RoleControllerIntegrationTest | CRUD 完整流程、权限校验 |
| auth | UserControllerIntegrationTest | 用户 CRUD、密码重置 |
| resource | ResourceControllerIntegrationTest | 资源查询、多模板切换 |
| ai | AiControllerIntegrationTest | 对话流、配置管理 |
| dashboard | DashboardControllerIntegrationTest | 大屏 CRUD、SQL 执行 |
| collector | DataSourceControllerIntegrationTest | 数据源 CRUD、连接测试 |

#### 3.2.3 系统测试

| 测试类型 | 工具 | 说明 |
|---------|------|------|
| API 契约测试 | Spring Cloud Contract | 验证 API 响应格式一致性 |
| 性能测试 | JMeter / K6 | 核心接口 QPS 压测 |
| 安全测试 | OWASP ZAP | 被动扫描 + 主动扫描 |

### 3.3 前端测试体系

#### 3.3.1 单元测试（Vitest + @vue/test-utils）

**覆盖率目标**：行覆盖率 ≥ 70%

**测试范围**：

| 类型 | 当前测试文件 | 目标测试文件 | 说明 |
|------|------------|------------|------|
| 公共组件 | 1 | 8 | TableCard, SearchCard, DialogForm, StatusTag, ActionButtons 等 |
| Store 测试 | 3 | 5 | user, app, menu, ai, dashboard |
| 工具函数 | 1 | 5 | request, validate, helpers, auth |
| 组合式函数 | 0 | 6 | useTable, usePagination, useCrudDialog, useSearchForm 等 |
| API 层 | 0 | 7 | 每个模块的 API 函数 |

**测试示例**：

```typescript
// 公共组件测试
describe('TableCard', () => {
  it('renders table with data', () => { ... })
  it('handles pagination change', () => { ... })
  it('emits selection-change event', () => { ... })
  it('shows empty state when no data', () => { ... })
})

// 组合式函数测试
describe('usePagination', () => {
  it('initializes with default values', () => { ... })
  it('handles page size change', () => { ... })
  it('handles current page change', () => { ... })
  it('resets to first page', () => { ... })
})
```

#### 3.3.2 组件测试

| 页面 | 测试场景 |
|------|---------|
| Login.vue | 表单校验、登录成功/失败、记住密码 |
| Layout.vue | 菜单渲染、路由切换、用户信息展示 |
| UserManagement.vue | 列表加载、搜索、新增、编辑、删除 |
| RoleManagement.vue | 列表加载、权限分配、状态切换 |
| TableResourceManagement.vue | configJson 编辑器交互、处理器配置 |
| DashboardDesigner.vue | 拖拽布局、图表配置、数据源绑定 |

#### 3.3.3 E2E 测试（Playwright）

| 场景 | 优先级 |
|------|--------|
| 登录 → 首页 → 登出 | P0 |
| 用户管理 CRUD 完整流程 | P0 |
| 角色管理 + 权限分配 | P0 |
| 数据源配置 + 测试连接 | P1 |
| 资源查询 + 结果展示 | P1 |
| 大屏设计 + 预览 | P1 |
| AI 对话 | P2 |

### 3.4 测试基础设施

| 工具 | 用途 | 状态 |
|------|------|------|
| Testcontainers | 集成测试数据库/Redis | 📋 待集成 |
| H2 Database | 纯单元测试（无外部依赖） | 📋 待集成 |
| JaCoCo | 覆盖率报告 | ✅ 已有 |
| Codecov | 覆盖率可视化 | ✅ 已有 |
| Vitest | 前端测试框架 | ✅ 已有 |
| Playwright | E2E 测试 | 📋 待集成 |

### 3.5 测试任务分解

| 编号 | 任务 | 产出物 | 预估工时 | 优先级 | 前置依赖 |
|------|------|--------|---------|--------|---------|
| TST-01 | 集成 Testcontainers | 测试基础设施 | 4h | P0 | 无 |
| TST-02 | 编写 Service 层单元测试补充 | 各模块测试文件 | 10h | P0 | TST-01 |
| TST-03 | 编写 Controller 层 MockMvc 集成测试 | 7 个集成测试类 | 8h | P0 | TST-01 |
| TST-04 | 编写前端公共组件测试 | 8 个组件测试 | 6h | P0 | 无 |
| TST-05 | 编写前端组合式函数测试 | 6 个 composable 测试 | 4h | P0 | 无 |
| TST-06 | 编写前端页面组件测试 | 6 个页面测试 | 8h | P1 | TST-04 |
| TST-07 | 编写前端 API 层测试 | 7 个 API 测试 | 4h | P1 | 无 |
| TST-08 | 集成 Playwright E2E 测试 | 10+ 个 E2E 场景 | 8h | P1 | TST-03 |
| TST-09 | JaCoCo 覆盖率阈值提升至 80% | 父 pom.xml | 0.5h | P0 | TST-02 |
| TST-10 | 前端覆盖率收集（c8） | vitest.config.ts | 1h | P0 | TST-04 |
| TST-11 | 性能测试脚本 | K6 脚本 | 4h | P2 | 核心功能稳定 |
| TST-12 | 安全测试集成（OWASP ZAP） | CI 集成 | 3h | P2 | P0 安全修复完成 |

---

## 四、代码质量检测机制

### 4.1 工具链总览

```
          ┌──────────────────────────────────────────┐
          │              代码提交 (git push)           │
          └──────────────────┬───────────────────────┘
                             │
          ┌──────────────────▼───────────────────────┐
          │  Pre-commit Hook (可选)                    │
          │  ├─ Checkstyle (后端)                      │
          │  ├─ ESLint + Prettier (前端)               │
          │  └─ 敏感信息扫描 (git-secrets)             │
          └──────────────────┬───────────────────────┘
                             │
          ┌──────────────────▼───────────────────────┐
          │  CI 流水线 (自动触发)                      │
          │  ├─ 编译检查                               │
          │  ├─ Checkstyle (后端代码风格)              │
          │  ├─ SpotBugs   (后端 Bug 检测)            │
          │  ├─ PMD        (后端代码规范)              │
          │  ├─ JaCoCo     (覆盖率，阈值 80%)          │
          │  ├─ ESLint     (前端代码风格)              │
          │  ├─ Prettier   (前端代码格式化)            │
          │  ├─ TypeScript (类型检查)                  │
          │  ├─ Trivy      (依赖漏洞扫描) ✅           │
          │  └─ SonarQube  (综合质量平台)              │
          └──────────────────────────────────────────┘
```

### 4.2 后端代码质量工具

#### 4.2.1 Checkstyle（代码风格）

**配置文件**：`checkstyle.xml`（基于 Google Java Style）

```xml
<!-- 关键规则 -->
<module name="Checker">
    <!-- 行长度限制 120 -->
    <module name="LineLength"><property name="max" value="120"/></module>
    <!-- 禁止尾随空格 -->
    <module name="RegexpSingleline">
        <property name="format" value="\s+$"/>
    </module>
</module>

<module name="TreeWalker">
    <!-- 命名规范 -->
    <module name="LocalVariableName"/>
    <module name="MemberName"/>
    <module name="MethodName"/>
    <module name="TypeName"/>
    <!-- 禁止星号导入 -->
    <module name="AvoidStarImport"/>
    <!-- 未使用的导入 -->
    <module name="UnusedImports"/>
    <!-- 魔法数字 -->
    <module name="MagicNumber">
        <property name="ignoreNumbers" value="-1,0,1,2,3,100,200,500,1000"/>
    </module>
</module>
```

**Maven 配置**：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-checkstyle-plugin</artifactId>
    <version>3.6.0</version>
    <configuration>
        <configLocation>checkstyle.xml</configLocation>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

#### 4.2.2 SpotBugs（Bug 检测）

```xml
<plugin>
    <groupId>com.github.spotbugs</groupId>
    <artifactId>spotbugs-maven-plugin</artifactId>
    <version>4.8.6</version>
    <configuration>
        <effort>Max</effort>
        <threshold>Medium</threshold>  <!-- Low/Medium/High -->
        <failOnError>true</failOnError>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

#### 4.2.3 PMD（代码规范）

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-pmd-plugin</artifactId>
    <version>3.26.0</version>
    <configuration>
        <rulesets>
            <ruleset>category/java/bestpractices.xml</ruleset>
            <ruleset>category/java/errorprone.xml</ruleset>
            <ruleset>category/java/security.xml</ruleset>
        </rulesets>
        <failOnViolation>true</failOnViolation>
    </configuration>
    <executions>
        <execution>
            <goals><goal>check</goal></goals>
        </execution>
    </executions>
</plugin>
```

#### 4.2.4 JaCoCo 覆盖率阈值提升

```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <id>jacoco-check</id>
            <goals><goal>check</goal></goals>
            <configuration>
                <rules>
                    <rule>
                        <element>PACKAGE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.80</minimum>  <!-- 从 60% 提升至 80% -->
                            </limit>
                            <limit>
                                <counter>BRANCH</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.70</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### 4.3 前端代码质量工具

#### 4.3.1 ESLint 配置完善

```javascript
// eslint.config.js
import vue from 'eslint-plugin-vue'
import ts from '@typescript-eslint/eslint-plugin'
import tsParser from '@typescript-eslint/parser'

export default [
  {
    files: ['**/*.vue', '**/*.ts', '**/*.tsx'],
    plugins: { vue, '@typescript-eslint': ts },
    languageOptions: { parser: tsParser },
    rules: {
      'no-console': 'warn',                        // 警告 console
      'no-debugger': 'error',                      // 禁止 debugger
      '@typescript-eslint/no-explicit-any': 'warn', // 警告 any 类型
      '@typescript-eslint/no-unused-vars': 'error', // 未使用变量
      'vue/multi-word-component-names': 'error',    // 组件名多单词
      'vue/no-unused-components': 'error',          // 未使用组件
    },
  },
]
```

#### 4.3.2 Prettier 集成

```json
// .prettierrc
{
  "semi": false,
  "singleQuote": true,
  "trailingComma": "all",
  "printWidth": 100,
  "tabWidth": 2,
  "arrowParens": "always",
  "endOfLine": "lf"
}
```

```json
// package.json scripts
{
  "format": "prettier --write .",
  "format:check": "prettier --check ."
}
```

### 4.4 代码质量任务分解

| 编号 | 任务 | 产出物 | 预估工时 | 优先级 | 前置依赖 |
|------|------|--------|---------|--------|---------|
| CQ-01 | 集成 Checkstyle | checkstyle.xml + pom.xml | 2h | P0 | 无 |
| CQ-02 | 集成 SpotBugs | pom.xml 配置 | 1h | P0 | 无 |
| CQ-03 | 集成 PMD | pom.xml 配置 | 1h | P0 | 无 |
| CQ-04 | JaCoCo 阈值提升至 80% | pom.xml | 0.5h | P0 | TST-01~03 |
| CQ-05 | 完善 ESLint 配置 | eslint.config.js | 2h | P0 | 无 |
| CQ-06 | 集成 Prettier | .prettierrc + scripts | 1h | P0 | 无 |
| CQ-07 | 集成 git-secrets | .gitconfig + pre-commit | 1h | P1 | 无 |
| CQ-08 | 集成 SonarQube | CI 配置 + sonar-project.properties | 3h | P2 | CQ-01~06 |
| CQ-09 | 修复现有代码风格问题 | 各模块代码 | 8h | P1 | CQ-01, CQ-05~06 |
| CQ-10 | 前端 TypeScript 严格模式 | tsconfig.json | 1h | P0 | 无 |

---

## 五、CI/CD 流程优化

### 5.1 目标流水线架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                        Git Push / PR                                 │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  Stage 1: 代码质量检查（并行）                                        │
│  ├─ Checkstyle (后端)       ├─ ESLint + Prettier (前端)              │
│  ├─ SpotBugs (后端)         ├─ TypeScript 类型检查 (前端)            │
│  ├─ PMD (后端)              └─ 敏感信息扫描                          │
│  └─ Trivy 安全扫描                                                     │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  Stage 2: 测试（并行）                                                │
│  ├─ 后端单元测试 (JaCoCo)   ├─ 前端单元测试 (Vitest)                 │
│  ├─ 后端集成测试 (Testcontainers)                                    │
│  └─ SonarQube 分析                                                   │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  Stage 3: 构建（并行）                                                │
│  ├─ Maven Package (后端)    ├─ npm build (前端)                      │
│  └─ Docker Image Build (多平台)                                      │
└──────────────────────────────┬──────────────────────────────────────┘
                               │
┌──────────────────────────────▼──────────────────────────────────────┐
│  Stage 4: 部署（按分支策略）                                          │
│  ├─ develop → DEV 环境 (自动)                                        │
│  ├─ main    → STAGING 环境 (自动)                                    │
│  └─ tag     → PRODUCTION 环境 (手动审批)                             │
└──────────────────────────────────────────────────────────────────────┘
```

### 5.2 环境隔离策略

| 环境 | 触发条件 | 部署方式 | 数据库 | 审批 |
|------|---------|---------|--------|------|
| **DEV** | push 到 develop | 自动部署 | 独立 PostgreSQL | 无 |
| **STAGING** | push/PR 到 main | 自动部署 | 独立 PostgreSQL | 无 |
| **PRODUCTION** | git tag v* | 蓝绿部署 | 生产 PostgreSQL | 手动审批 |

### 5.3 前端 CI 硬失败修复

当前问题：

```yaml
# ❌ 当前：软失败
- name: TypeScript Type Check
  run: npx vue-tsc --noEmit
  continue-on-error: true   # 失败不阻止流水线

- name: Frontend Unit Tests
  run: npm test
  continue-on-error: true   # 失败不阻止流水线
```

修复方案：

```yaml
# ✅ 修复后：硬失败 + 分阶段修复
- name: TypeScript Type Check
  run: npx vue-tsc --noEmit
  # 移除 continue-on-error，先修复现有类型错误

- name: ESLint Check
  run: npm run lint
  # 移除 || true，基于完善后的 ESLint 配置

- name: Frontend Unit Tests
  run: npm test
  # 移除 continue-on-error，补充测试用例后启用
```

### 5.4 新增 Job 设计

#### 5.4.1 代码质量检查 Job

```yaml
code-quality:
  name: Code Quality Check
  runs-on: ubuntu-latest
  steps:
    - name: Checkout
      uses: actions/checkout@v4

    - name: Set up JDK 21
      uses: actions/setup-java@v4
      with:
        java-version: '21'
        distribution: 'temurin'
        cache: maven

    - name: Checkstyle
      run: mvn checkstyle:check -B

    - name: SpotBugs
      run: mvn spotbugs:check -B

    - name: PMD
      run: mvn pmd:check -B

    - name: Setup Node.js
      uses: actions/setup-node@v4
      with:
        node-version: '20'
        cache: 'npm'
        cache-dependency-path: easy-lowcode-frontend/package-lock.json

    - name: ESLint
      working-directory: easy-lowcode-frontend
      run: npm run lint

    - name: Prettier Check
      working-directory: easy-lowcode-frontend
      run: npm run format:check

    - name: TypeScript Check
      working-directory: easy-lowcode-frontend
      run: npx vue-tsc --noEmit
```

#### 5.4.2 部署 Job

```yaml
deploy-dev:
  name: Deploy to DEV
  needs: [backend-test, frontend-test, code-quality]
  if: github.ref == 'refs/heads/develop'
  runs-on: ubuntu-latest
  environment:
    name: dev
    url: https://dev.easy-lowcode.example.com
  steps:
    - name: Deploy to DEV server
      run: |
        # SSH 到 DEV 服务器
        # docker-compose pull && docker-compose up -d

deploy-staging:
  name: Deploy to STAGING
  needs: [backend-test, frontend-test, code-quality, docker-build]
  if: github.ref == 'refs/heads/main'
  runs-on: ubuntu-latest
  environment:
    name: staging
    url: https://staging.easy-lowcode.example.com
  steps:
    - name: Deploy to STAGING server
      run: |
        # 部署到预发布环境

deploy-prod:
  name: Deploy to PRODUCTION
  needs: [deploy-staging]
  if: startsWith(github.ref, 'refs/tags/v')
  runs-on: ubuntu-latest
  environment:
    name: production
    url: https://easy-lowcode.example.com
  steps:
    - name: Manual Approval Gate
      # 使用 GitHub Environment 保护规则

    - name: Blue-Green Deploy
      run: |
        # 蓝绿部署脚本
        # 1. 部署到 inactive 环境
        # 2. 健康检查
        # 3. 切换流量
        # 4. 保留旧版本用于回滚
```

### 5.5 版本控制策略

| 分支 | 用途 | 保护规则 |
|------|------|---------|
| `main` | 生产就绪代码 | 禁止直接 push，需 PR + 1 人评审 + CI 通过 |
| `develop` | 开发集成分支 | 禁止直接 push，需 PR + CI 通过 |
| `feature/*` | 功能开发分支 | 从 develop 创建，合并回 develop |
| `fix/*` | Bug 修复分支 | 从 develop 或 main 创建 |
| `release/*` | 发布准备分支 | 从 develop 创建，合并到 main |
| `hotfix/*` | 紧急修复分支 | 从 main 创建，合并到 main 和 develop |

**Tag 命名规范**：`v{major}.{minor}.{patch}`（如 `v1.0.0`）

### 5.6 CI/CD 任务分解

| 编号 | 任务 | 产出物 | 预估工时 | 优先级 | 前置依赖 |
|------|------|--------|---------|--------|---------|
| CI-01 | 修复前端 CI 软失败 | ci.yml | 1h | P0 | TST-04~05, CQ-05~06 |
| CI-02 | 新增代码质量检查 Job | ci.yml | 2h | P0 | CQ-01~03, CQ-05~06 |
| CI-03 | 新增 DEV 环境自动部署 | ci.yml + deploy/dev/ | 3h | P1 | 无 |
| CI-04 | 新增 STAGING 环境自动部署 | ci.yml + deploy/staging/ | 3h | P1 | 无 |
| CI-05 | 新增 PRODUCTION 蓝绿部署 | ci.yml + deploy/prod/ | 4h | P1 | CI-04 |
| CI-06 | 配置 GitHub Environment 保护规则 | GitHub Settings | 1h | P1 | CI-05 |
| CI-07 | 配置分支保护规则 | GitHub Settings | 0.5h | P0 | 无 |
| CI-08 | 新增 E2E 测试 Job | ci.yml | 2h | P2 | TST-08 |
| CI-09 | 新增性能测试 Job | ci.yml | 2h | P2 | TST-11 |
| CI-10 | 集成 SonarQube | ci.yml + sonar-project.properties | 2h | P2 | CQ-08 |

---

## 六、实施路线图

### 6.1 总体时间线

```
Phase 0 (当前)     Phase 1 (第1-2周)    Phase 2 (第3-4周)    Phase 3 (第5-6周)    Phase 4 (第7-8周)
  ├─ 已完成修复       ├─ 代码质量工具      ├─ 测试补充          ├─ 集成测试 + E2E    ├─ 部署流水线
  │  P0-1,4,5,6      │  Checkstyle        │  Service 单测      │  Testcontainers     │  DEV/STAGING 部署
  │  Bug-1~6         │  SpotBugs          │  Controller 集成   │  Playwright E2E     │  PROD 蓝绿部署
  │                  │  PMD               │  前端组件/页面测试  │  SonarQube          │  文档完善
  │                  │  ESLint + Prettier  │  覆盖率阈值提升    │  性能测试           │  监控告警
  └──────────────────┴────────────────────┴────────────────────┴────────────────────┴───────────────────►
```

### 6.2 Phase 1：代码质量基础（第 1-2 周）

| 优先级 | 任务编号 | 任务 | 预估工时 |
|--------|---------|------|---------|
| P0 | CQ-01 | 集成 Checkstyle | 2h |
| P0 | CQ-02 | 集成 SpotBugs | 1h |
| P0 | CQ-03 | 集成 PMD | 1h |
| P0 | CQ-05 | 完善 ESLint 配置 | 2h |
| P0 | CQ-06 | 集成 Prettier | 1h |
| P0 | CQ-10 | TypeScript 严格模式 | 1h |
| P0 | TST-01 | 集成 Testcontainers 基础设施 | 4h |
| P0 | CI-07 | 配置分支保护规则 | 0.5h |
| P0 | DOC-01 | 创建 architecture.md | 2h |
| P0 | DOC-08 | 更新 AGENTS.md | 2h |
| P1 | CQ-09 | 修复现有代码风格问题 | 8h |

**Phase 1 验收标准**：
- [ ] Checkstyle/SpotBugs/PMD 在 CI 中运行且零违规
- [ ] ESLint/Prettier 在 CI 中运行且零错误
- [ ] 前端 TypeScript 无类型错误
- [ ] Testcontainers 配置完成，至少 1 个集成测试通过
- [ ] 分支保护规则生效

### 6.3 Phase 2：测试补充（第 3-4 周）

| 优先级 | 任务编号 | 任务 | 预估工时 |
|--------|---------|------|---------|
| P0 | TST-02 | Service 层单元测试补充 | 10h |
| P0 | TST-03 | Controller 层 MockMvc 集成测试 | 8h |
| P0 | TST-04 | 前端公共组件测试 | 6h |
| P0 | TST-05 | 前端组合式函数测试 | 4h |
| P0 | TST-09 | JaCoCo 覆盖率阈值提升至 80% | 0.5h |
| P0 | TST-10 | 前端覆盖率收集 | 1h |
| P0 | CI-01 | 修复前端 CI 软失败 | 1h |
| P0 | CI-02 | 新增代码质量检查 Job | 2h |
| P1 | TST-06 | 前端页面组件测试 | 8h |
| P1 | TST-07 | 前端 API 层测试 | 4h |

**Phase 2 验收标准**：
- [ ] 后端行覆盖率 ≥ 80%
- [ ] 前端行覆盖率 ≥ 70%
- [ ] 所有 Controller 至少 2 个集成测试
- [ ] 前端 CI 所有检查为硬失败
- [ ] 代码质量检查 Job 正常运行

### 6.4 Phase 3：集成测试与安全（第 5-6 周）

| 优先级 | 任务编号 | 任务 | 预估工时 |
|--------|---------|------|---------|
| P1 | TST-08 | 集成 Playwright E2E 测试 | 8h |
| P1 | CI-08 | 新增 E2E 测试 Job | 2h |
| P1 | DOC-04 | 编写 API 参考文档 | 4h |
| P1 | DOC-05 | 编写数据库设计文档 | 3h |
| P1 | DOC-06 | 编写安全架构文档 | 4h |
| P2 | CQ-08 | 集成 SonarQube | 3h |
| P2 | CI-10 | SonarQube CI 集成 | 2h |
| P2 | TST-11 | 性能测试脚本 | 4h |
| P2 | TST-12 | 安全测试集成（OWASP ZAP） | 3h |

**Phase 3 验收标准**：
- [ ] 10+ 个 E2E 场景全部通过
- [ ] SonarQube Quality Gate 通过
- [ ] 核心接口 P95 延迟 < 500ms
- [ ] OWASP ZAP 无 High 级别告警

### 6.5 Phase 4：部署流水线与文档完善（第 7-8 周）

| 优先级 | 任务编号 | 任务 | 预估工时 |
|--------|---------|------|---------|
| P0 | DOC-02 | 编写用户手册 | 8h |
| P0 | DOC-03 | 编写运维手册 | 6h |
| P1 | CI-03 | 新增 DEV 环境自动部署 | 3h |
| P1 | CI-04 | 新增 STAGING 环境自动部署 | 3h |
| P1 | CI-05 | 新增 PRODUCTION 蓝绿部署 | 4h |
| P1 | CI-06 | 配置 GitHub Environment 保护规则 | 1h |
| P2 | DOC-07 | 创建架构决策记录 | 5h |
| P2 | DOC-09 | CI 集成文档链接检查 | 1h |
| P2 | CI-09 | 新增性能测试 Job | 2h |

**Phase 4 验收标准**：
- [ ] DEV 环境 push 自动部署 < 5 分钟
- [ ] PRODUCTION 蓝绿部署零停机
- [ ] 用户手册覆盖所有核心功能
- [ ] 运维手册覆盖部署、监控、备份

---

## 七、验收标准

### 7.1 文档体系验收

| 检查项 | 标准 |
|--------|------|
| 需求文档 | 覆盖全部 7 大模块，与当前代码一致 |
| 设计文档 | 架构图、数据流图、模块依赖关系完整 |
| 用户手册 | 覆盖所有核心功能操作流程，含截图 |
| API 文档 | 所有 REST 接口有请求/响应示例 |
| 运维手册 | 覆盖部署、监控、备份、故障恢复 |
| 开发规范 | AGENTS.md 与代码实际风格一致 |
| 文档链接 | 所有内部链接有效，无死链 |

### 7.2 测试验收

| 检查项 | 标准 |
|--------|------|
| 后端单元测试覆盖率 | 行覆盖率 ≥ 80%，分支覆盖率 ≥ 70% |
| 前端单元测试覆盖率 | 行覆盖率 ≥ 70% |
| Controller 集成测试 | 每个 Controller ≥ 2 个用例 |
| E2E 测试 | 核心流程 ≥ 10 个场景 |
| 性能测试 | 核心接口 P95 延迟 < 500ms，QPS > 100 |
| 安全测试 | OWASP ZAP 无 High/Critical 告警 |

### 7.3 代码质量验收

| 检查项 | 标准 |
|--------|------|
| Checkstyle | 零违规 |
| SpotBugs | 无 Medium 及以上级别问题 |
| PMD | 零违规 |
| ESLint | 零错误 |
| Prettier | 代码格式一致 |
| TypeScript | 无类型错误 |
| Trivy | 无 CRITICAL/HIGH 漏洞 |
| SonarQube | Quality Gate 通过 |

### 7.4 CI/CD 验收

| 检查项 | 标准 |
|--------|------|
| 代码提交触发 | push/PR 自动触发完整流水线 |
| 构建时间 | 全流程 < 15 分钟 |
| 前端检查 | 全部为硬失败（非 continue-on-error） |
| DEV 部署 | 自动部署，成功率 ≥ 95% |
| STAGING 部署 | 自动部署，部署后自动冒烟测试 |
| PRODUCTION 部署 | 蓝绿部署，零停机，支持一键回滚 |
| 环境隔离 | DEV/STAGING/PROD 完全隔离 |
| 分支保护 | main/develop 分支禁止直接 push |

---

## 附录

### A. 相关文档索引

| 文档 | 路径 |
|------|------|
| 需求文档 | `docs/01-需求文档.md` |
| 技术架构文档 | `docs/02-技术架构文档.md` |
| 历史变更文档 | `docs/03-历史变更文档.md` |
| 代码评审报告 | `docs/review/code-review-report.md` |
| 生产就绪计划 | `docs/compose/plans/2026-06-19-production-readiness.md` |
| CI/CD 配置 | `.github/workflows/ci.yml` |
| 当前父 POM | `pom.xml` |

### B. 工时估算汇总

| 阶段 | 总工时 | 任务数 |
|------|--------|--------|
| Phase 1（代码质量基础） | 24.5h | 11 |
| Phase 2（测试补充） | 44.5h | 10 |
| Phase 3（集成测试与安全） | 33h | 8 |
| Phase 4（部署与文档） | 33h | 9 |
| **总计** | **135h** | **38** |

### C. 风险与缓解

| 风险 | 影响 | 概率 | 缓解措施 |
|------|------|------|---------|
| 测试覆盖率提升导致 CI 长时间阻塞 | 中 | 高 | 分阶段提升阈值，Phase 1 先到 60%，Phase 2 到 80% |
| 代码风格工具引入大量修复工作 | 高 | 中 | 先以 warn 模式运行，逐步修复后切换为 error |
| Testcontainers 需要 Docker 环境 | 中 | 低 | CI 环境已支持 Docker，本地开发需要 Docker Desktop |
| 部署流水线依赖外部服务器 | 高 | 中 | 先实现 STAGING 环境，PRODUCTION 环境逐步完善 |
| 人员短缺 | 高 | 中 | 任务按优先级排列，P0 必须完成，P1/P2 可按需调整 |