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
| `MINIMAX_API_KEY` | ⭐推荐 | MiniMax API Key |
| `REDIS_PASSWORD` | ❌ | Redis 密码（留空无密码） |
| `JWT_SECRET` | ✅ | JWT 签名密钥 |
| `ENCRYPT_AES_KEY` | ✅ | AES 加密密钥（敏感数据加密） |
| `ROCKETMQ_NAMESERVER` | ❌ | RocketMQ NameServer 地址（默认 localhost:9876） |

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

## 提交规范

**每次大改动必须通过所有测试才能 commit。** 执行顺序：

```bash
# 1. 后端测试
mvn test

# 2. 前端测试
cd easy-lowcode-frontend && npm run test

# 3. 前端类型检查
npx vue-tsc --noEmit

# 4. 全部通过后才可 commit
git add . && git commit -m "feat: ..."
```

任何测试失败 → 修复后再 commit，禁止跳过。

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

## 开发规范

### 一、后端开发规范 (Java / Spring Boot)

#### 1. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| **类名** | UpperCamelCase (名词/名词短语) | `SysRoleService`, `RoleController` |
| **方法名** | lowerCamelCase (动词/动词短语) | `getRoleList()`, `createUser()` |
| **变量名** | lowerCamelCase | `roleName`, `userList` |
| **常量名** | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE`, `TOKEN_PREFIX` |
| **包名** | 全小写，点号分隔，`com.dabai.easy_lowcode.{module}` | `com.dabai.easy_lowcode.auth.service` |
| **表名** | snake_case | `sys_user`, `sys_role` |
| **字段名** | snake_case (MyBatis Plus 自动驼峰转换) | `role_code`, `role_name` |
| **测试类** | 被测试类名 + `Test` 后缀 | `SysRoleServiceImplTest` |
| **枚举/接口常量** | 定义在 `interface` 而非 `class` 中 | `CommonConstant` |

#### 2. 分层架构

```
Controller (请求入口) → Service (业务逻辑) → Mapper (数据访问) → Entity (数据模型)
                                 ↑
                           ServiceImpl (实现)
```

- **Controller**: `@RestController` + `@RequestMapping("/api/{module}/{resource}")`，只做参数校验和结果返回，不写业务逻辑
- **Service**: 定义接口，继承 `IService<T>`，方法命名以业务含义为主
- **ServiceImpl**: `extends ServiceImpl<Mapper, Entity> implements Service`，实现具体业务逻辑
- **Mapper**: `extends BaseMapper<T>`，复杂查询使用 MyBatis Plus `LambdaQueryWrapper`，复杂SQL使用 `@Select` / XML
- **Entity**: `extends BaseEntity`，字段与数据库列一一对应

#### 3. Lombok 使用规范

| 注解 | 使用场景 |
|------|---------|
| `@Data` | Entity、DTO、VO 类（自动生成 getter/setter/toString/equals/hashCode） |
| `@EqualsAndHashCode(callSuper = true)` | Entity 继承 BaseEntity 时必须加，确保父类字段参与比较 |
| `@Slf4j` | 所有 Service 实现类、Controller、工具类 |
| `@RequiredArgsConstructor` | Controller、Service 实现类代替 `@Autowired`，配合 `private final` 字段实现构造器注入 |
| `@Builder` | DTO、复杂参数对象（非 Entity） |
| `@Getter` | 异常类、枚举类（仅暴露 getter） |

```java
// ✅ 正确：构造器注入
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/auth/role")
public class RoleController {
    private final SysRoleService roleService;
}

// ❌ 避免：字段注入
@Autowired
private SysRoleService roleService;
```

#### 4. 统一返回结果 (Result)

```java
// 通用模式
Result.success(data);           // 200 + data
Result.success("操作成功");       // 200 + message
Result.error("参数错误");         // 500 + message
Result.error(400, "参数错误");    // 自定义 code + message

// Controller 方法签名
public Result<List<SysRole>> getRoleList() { ... }
public Result<Void> createRole(@RequestBody SysRole role) { ... }
```

- Controller 层统一返回 `Result<T>` 类型
- 分页查询使用 `PageResult<T>` 包装
- 前端请求拦截器自动解包 `res.data`

#### 5. 异常处理

| 异常类型 | 处理方式 | 响应码 |
|---------|---------|-------|
| **业务异常** | `throw new BusinessException("消息")` | 500 (或自定义) |
| **参数校验失败** | `@Valid` / `@Validated` 自动触发 | 400 |
| **参数绑定失败** | `BindException` 自动捕获 | 400 |
| **系统异常** | 兜底 `Exception` 拦截 | 500 |

```java
// 抛出业务异常
throw new BusinessException("角色编码已存在");
throw new BusinessException(400, "角色编码不合法");

// 参数校验
@PostMapping
public Result<Void> create(@Valid @RequestBody SysRole role) { ... }
```

- `GlobalExceptionHandler` 使用 `@RestControllerAdvice` 统一拦截
- 所有异常必须记录日志：`log.error(...)` 或 `log.warn(...)`

#### 6. 依赖注入

- **强制**: 使用构造器注入 (`@RequiredArgsConstructor` + `private final`)
- **禁止**: `@Autowired` 字段注入
- **禁止**: 循环依赖（如有则拆分 Service 或引入事件机制）

```java
@RequiredArgsConstructor
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {
    private final SysDeptService deptService;  // 依赖注入
}
```

#### 7. MyBatis Plus 最佳实践

| 场景 | 做法 |
|------|------|
| **条件查询** | 使用 `LambdaQueryWrapper`，避免硬编码字段名 |
| **排序** | `wrapper.orderByAsc(SysRole::getSort)` |
| **分页** | Service 层调用 `this.page(page, wrapper)` |
| **逻辑删除** | 字段 `deleted` (0/1)，`@TableLogic` 注解 |
| **主键** | `@TableId(type = IdType.ASSIGN_ID)` 雪花算法 |
| **自动填充** | `@TableField(fill = FieldFill.INSERT)` 配合 `AutoFillHandler` |

```java
LambdaQueryWrapper<SysRole> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(SysRole::getRoleCode, role.getRoleCode());
wrapper.orderByAsc(SysRole::getSort);
List<SysRole> list = this.list(wrapper);
```

#### 8. 事务管理

```java
@Transactional(rollbackFor = Exception.class)
public void createRoleWithMenus(SysRole role, List<Long> menuIds) {
    this.save(role);
    roleMenuService.saveBatch(menuIds.stream()
        .map(menuId -> new SysRoleMenu(role.getId(), menuId))
        .collect(Collectors.toList()));
}
```

- `@Transactional` 加在 Service 实现类方法上
- 必须指定 `rollbackFor`（默认只回滚 RuntimeException）
- 事务只用于写操作（增删改），查询不加

#### 9. Stream / Optional 最佳实践

```java
// ✅ Stream 链式处理
List<String> names = roleList.stream()
    .filter(r -> r.getStatus() == 1)
    .map(SysRole::getRoleName)
    .sorted()
    .collect(Collectors.toList());

// ✅ Optional 防 NPE
String roleName = Optional.ofNullable(role)
    .map(SysRole::getRoleName)
    .orElse("默认角色名");

// ❌ 避免：在 stream 中修改外部变量
// ❌ 避免：多层 forEach 嵌套
```

#### 10. 测试规范

```java
@SpringBootTest
@Slf4j
class SysRoleServiceImplTest {

    @Resource
    private SysRoleService roleService;

    @Test
    void testGetRoleList() {
        List<SysRole> list = roleService.getRoleList();
        assertThat(list).isNotNull();
        assertThat(list).isNotEmpty();
    }
}
```

- 测试类名: `{被测类}Test`，放在 `src/test/java` 对应包下
- 测试方法名: `test{场景}` 或 `{method}_{scenario}`（如 `createRole_duplicateCode_shouldFail`）
- 使用 AssertJ 或 JUnit 断言，避免手动 if 判断
- 使用 `@BeforeEach` 初始化测试数据，`@AfterEach` 清理
- Mock 外部依赖，只测当前层逻辑

#### 11. 日志规范

- 使用 `@Slf4j` 注解
- 日志级别: `error`(异常) > `warn`(需关注) > `info`(关键流程) > `debug`(调试)
- 禁止: `System.out.println`、`e.printStackTrace()`
- 异常日志必须传入完整异常对象: `log.error("业务异常: {}", e.getMessage(), e)`

---

### 二、前端开发规范 (Vue 3 / TypeScript / Element Plus)

#### 1. 命名规范

| 类型 | 规范 | 示例 |
|------|------|------|
| **组件名** | PascalCase，多单词 | `TableCard.vue`, `DialogForm.vue` |
| **文件/目录名** | camelCase | `userManagement.vue`, `processorForms/` |
| **变量/函数** | camelCase | `formData`, `handleAdd()` |
| **常量/枚举** | UPPER_SNAKE_CASE | `DEFAULT_PAGE_SIZE` |
| **类型定义** | PascalCase | `UserInfo`, `LoginParams` |
| **API 函数** | camelCase | `getRoleList()`, `createUser()` |
| **Pinia Store** | `use{Name}Store` | `useUserStore()`, `useAppStore()` |

#### 2. 目录结构

```
src/
├── api/              # API 请求（按模块分文件）
│   ├── base.ts       # 通用 CRUD 工厂函数
│   ├── auth.ts       # 认证/用户/角色/菜单/部门 API
│   ├── ai.ts         # AI 相关 API
│   └── ...
├── assets/           # 静态资源（图片、字体）
├── components/       # 公共通用组件
│   ├── TableCard.vue
│   ├── SearchCard.vue
│   └── StatusTag.vue
├── composables/      # 组合式函数（逻辑复用）
├── router/
│   └── index.ts      # 路由配置
├── stores/           # Pinia 状态管理
│   ├── user.ts
│   ├── app.ts
│   └── menu.ts
├── types/            # TypeScript 类型定义
│   ├── common.ts     # 通用类型（BaseEntity, PageResult, ApiResponse）
│   ├── auth.ts
│   └── dashboard.ts
├── utils/            # 工具函数
│   ├── request.ts    # Axios 封装（拦截器）
│   ├── validate.ts   # 表单校验
│   └── helpers.ts    # 辅助函数
└── views/            # 页面组件（按模块分目录）
    ├── system/
    │   ├── UserManagement.vue
    │   └── RoleManagement.vue
    ├── ai/
    ├── dashboard/
    ├── resource/
    │   └── components/    # 局部组件（仅当前模块使用）
    └── etl/
```

#### 3. Composition API + `<script setup>`

```vue
<!-- ✅ 强制使用：Composition API + <script setup lang="ts"> -->
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getRoleList } from '@/api/auth'
import type { SysRole } from '@/types/auth'

// 响应式状态
const tableData = ref<SysRole[]>([])
const loading = ref(false)

// 方法
const fetchData = async () => {
  loading.value = true
  try {
    tableData.value = await getRoleList()
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
})
</script>
```

- **禁用**: Options API (`export default { data(), methods: {} }`)
- **禁用**: `this` 关键字
- 组件必须加 `lang="ts"`
- 复杂组件提取逻辑到 composables

#### 4. Pinia Store 规范

```typescript
// ✅ setup 方式（推荐）
export const useUserStore = defineStore('user', () => {
  // state
  const token = ref<string>('')
  const userInfo = ref<UserInfo | null>(null)

  // getters
  const isLoggedIn = computed(() => !!token.value)
  const nickname = computed(() => userInfo.value?.nickname || '')

  // actions
  function setToken(newToken: string) {
    token.value = newToken
    localStorage.setItem('token', newToken)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    localStorage.removeItem('token')
  }

  return { token, userInfo, isLoggedIn, nickname, setToken, logout }
})
```

- Store 命名: `use{Name}Store`
- 文件名: `user.ts`, `app.ts` 等
- 优先使用 setup 语法（函数式）
- State 用 `ref()` / `reactive()`，getter 用 `computed()`，action 用普通函数

#### 5. 组件 Props / Emits 规范

```vue
<script setup lang="ts">
// ✅ Props: 使用 interface + withDefaults
interface Props {
  title?: string
  showPagination?: boolean
  data: TableData[]
}
const props = withDefaults(defineProps<Props>(), {
  title: '默认标题',
  showPagination: true,
})

// ✅ Emits: 使用类型化声明
const emit = defineEmits<{
  (e: 'update:modelValue', val: string): void
  (e: 'save', data: FormData): void
  (e: 'close'): void
}>()
</script>
```

- 必须显式声明 Props 类型（禁止 `defineProps(['name'])` 字符串写法）
- Emit 事件名使用 kebab-case（如 `page-change`、`update:current`）
- `v-model` 绑定使用 `update:modelValue` 或具名 `update:{prop}`

#### 6. Element Plus 使用规范

| 组件 | 使用方式 | 说明 |
|------|---------|------|
| **表格** | `<el-table>` + `<el-table-column>` | `border stripe` 样式，`show-overflow-tooltip` 处理长文本 |
| **分页** | `TableCard` 组件封装 | 统一布局: `total, sizes, prev, pager, next, jumper` |
| **表单** | `<el-form>` + `<el-form-item>` | 必填项使用 `required` 属性或 `rules` 校验规则 |
| **对话框** | `<el-dialog>` | `v-model` 控制显隐，`@close` 做清理 |
| **按钮** | `<el-button>` | 主要操作用 `type="primary"`，危险操作 `type="danger"` |
| **标签** | `<el-tag>` | 状态展示，`:type` 映射颜色 |
| **图标** | `<el-icon>` | 配合 Element Plus 图标库 |
| **输入框** | `<el-input>` | 搜索框使用 `clearable` 属性 |
| **远程搜索** | `<el-select>` + `remote` | 大数据量下拉使用远程搜索 |

```vue
<el-table :data="tableData" border stripe>
  <el-table-column type="index" label="序号" width="60" align="center" />
  <el-table-column prop="roleName" label="角色名称" min-width="150" show-overflow-tooltip />
  <el-table-column label="状态" width="100">
    <template #default="{ row }">
      <el-tag :type="row.status === 1 ? 'success' : 'danger'">
        {{ row.status === 1 ? '启用' : '禁用' }}
      </el-tag>
    </template>
  </el-table-column>
</el-table>
```

#### 7. Axios 请求拦截器

```typescript
// utils/request.ts

// 请求拦截器
service.interceptors.request.use((config) => {
  // 自动注入 Token
  const userStore = useUserStore()
  if (userStore.token) {
    config.headers['Authorization'] = `Bearer ${userStore.token}`
  }
  return config
})

// 响应拦截器
service.interceptors.response.use((response) => {
  const res = response.data
  if (res.code !== 200) {
    // 401 跳登录
    if (res.code === 401) {
      useUserStore().clearUser()
      window.location.href = '/login'
    }
    // 非静默模式弹出错误提示
    if (!config.silentError) {
      ElMessage.error(res.message)
    }
    return Promise.reject(new Error(res.message))
  }
  return res.data  // 自动解包，组件中直接拿到 data
})
```

- API 函数返回 `Promise<T>`（已解包类型）
- 错误重试、重复请求取消、请求 ID 追踪、`silentError` 模式
- 统一错误处理（`ElMessage.error`）

#### 8. API 封装规范

```typescript
// ✅ api/auth.ts — 按功能模块拆分

// 标准 CRUD 使用工厂函数
export const userApi = createCrudApi<UserInfo>('/auth/user')

// 自定义接口单独导出函数
export function login(data: LoginParams): Promise<{ token: string }> {
  return request({ url: '/auth/login', method: 'post', data })
}

export function assignRolesToUser(userId: number, roleIds: number[]) {
  return request({
    url: `/auth/authorization/user/${userId}/roles`,
    method: 'post',
    data: { roleIds },
  })
}
```

- 优先使用 `createCrudApi` 工厂函数生成标准 CRUD
- 自定义接口使用具名函数导出
- API 路径统一: `/{module}/{resource}`（如 `/auth/user/page`）
- 禁用: 在 Vue 组件中直接调用 `axios`，统一通过 `api/` 层

#### 9. Router 命名和结构

```typescript
const routes = [
  {
    path: '/login',
    name: 'login',  // camelCase
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: 'system/user',
        name: 'userManagement',
        component: () => import('@/views/system/UserManagement.vue'),
        meta: { title: '用户管理' },
      },
    ],
  },
]
```

- 路由 name 使用 camelCase（如 `userManagement`）
- 路由 path 使用 kebab-case（如 `/system/user`）
- 页面组件使用懒加载 `() => import(...)`
- 路由 meta 统一放置 title、requiresAuth、permissions 等元数据
- 路由守卫统一写在 `router/index.ts` 中

#### 10. TypeScript 严格类型

```typescript
// ✅ 定义接口 — 前端自己维护，与后端对齐
export interface LoginParams {
  username: string
  password: string
}

export interface UserInfo extends BaseEntity {
  username: string
  nickname: string
  realName: string
  phone: string
  email: string
  deptId: number
  status: number
}

// ✅ API 函数标注返回类型
export function getUserPage(current: number, size: number): Promise<PageResult<UserInfo>>

// ❌ 避免: any 类型（除非万不得已）
const data: any = await request(...)  // 禁止
```

- 所有 API 参数和返回值必须定义类型接口
- 通用类型放在 `types/common.ts`（`BaseEntity`, `PageResult`, `ApiResponse`, `PageParams`）
- 每个模块的类型放在 `types/{module}.ts`
- 尽量避免 `any`，使用 `unknown` 或泛型

#### 11. 样式规范

- 使用 `scoped` 样式，避免全局污染
- 类名使用 kebab-case（如 `.role-management`）
- 通用样式（字体、颜色、间距）统一在 `style.css` 中定义
- Element Plus 组件样式覆盖用 `:deep()` 选择器

```vue
<style scoped>
.role-management {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
  :deep(.el-table) {
    margin-top: 16px;
  }
}
</style>
```

#### 12. Git Commit 规范

使用 Conventional Commits:

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: add role management CRUD` |
| `fix` | Bug 修复 | `fix: fix role code generation NPE` |
| `refactor` | 重构 | `refactor: extract query builder` |
| `style` | 样式调整 | `style: adjust table column width` |
| `docs` | 文档 | `docs: update API documentation` |
| `test` | 测试 | `test: add role service unit test` |
| `chore` | 构建/工具 | `chore: update maven dependencies` |

提交信息格式: `<type>: <简短描述>`（不超过 72 字符）

---

### 三、安全规范

#### 1. 密钥管理

- JWT Secret 必须通过 `JWT_SECRET` 环境变量配置，禁止硬编码
- AES 加密密钥通过 `ENCRYPT_AES_KEY` 环境变量配置
- 密码使用 BCrypt 加密存储，禁止明文
- API Key（OpenAI、DeepSeek 等）禁止提交到 Git，通过环境变量注入

#### 2. 认证与鉴权

```java
@SaCheckLogin                    // 需要登录（大多数接口）
@SaCheckRole("admin")           // 需要 admin 角色
@SaCheckPermission("system:user:list")  // 需要特定权限
```

- Controller 层方法默认加 `@SaCheckLogin`
- 敏感操作（创建、删除）加具体权限注解
- 权限标识格式: `{模块}:{资源}:{操作}`（如 `system:user:create`）

#### 3. 数据安全

- `@JsonSerialize(using = ToStringSerializer.class)` 解决 Long ID 前端精度丢失
- 敏感数据（手机号、身份证）传输需脱敏处理
- SQL 参数必须使用 MyBatis Plus 参数绑定，禁止拼接字符串
- API 返回结果不暴露密码、Token 等敏感信息
