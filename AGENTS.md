# Easy Lowcode - 低代码平台

基于 Spring Boot 3 + Vue 3 的前后端分离低代码开发平台。

## 项目结构

```
easy_lowcode/
├── easy-lowcode-common          # 公共模块 - 工具类、常量、异常处理
├── easy-lowcode-database        # 数据库模块 - MyBatisPlus配置、Liquibase迁移
├── easy-lowcode-auth            # 认证授权模块 - Spring Security、用户/角色/权限管理
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

### 权限控制（Spring Security + JWT）
```java
@PreAuthorize("hasRole('admin')")     // 需要 admin 角色
@PreAuthorize("hasAuthority('system:user:list')")  // 需要特定权限
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

- **语言**: 必须使用**中文**编写 commit message
- **格式**: `<type>: <简短中文描述>`（不超过 72 字符）

| 类型 | 说明 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat: 新增角色管理CRUD` |
| `fix` | Bug 修复 | `fix: 修复角色编码生成空指针` |
| `refactor` | 重构 | `refactor: 抽取查询构建器` |
| `style` | 样式调整 | `style: 调整表格列宽` |
| `docs` | 文档 | `docs: 更新API文档` |
| `test` | 测试 | `test: 新增角色服务单元测试` |
| `chore` | 构建/工具 | `chore: 更新Maven依赖` |

---

### 三、安全规范

#### 1. 密钥管理

- JWT Secret 必须通过 `JWT_SECRET` 环境变量配置，禁止硬编码
- AES 加密密钥通过 `ENCRYPT_AES_KEY` 环境变量配置
- 密码使用 BCrypt 加密存储，禁止明文
- API Key（OpenAI、DeepSeek 等）禁止提交到 Git，通过环境变量注入

#### 2. 认证与鉴权

```java
@PreAuthorize("hasRole('admin')")           // 需要 admin 角色
@PreAuthorize("hasAuthority('system:user:list')")  // 需要特定权限
```

- Controller 层方法默认加 `@PreAuthorize` 注解
- 敏感操作（创建、删除）加具体权限注解
- 权限标识格式: `{模块}:{资源}:{操作}`（如 `system:user:create`）

#### 3. 数据安全

- `@JsonSerialize(using = ToStringSerializer.class)` 解决 Long ID 前端精度丢失
- 敏感数据（手机号、身份证）传输需脱敏处理
- SQL 参数必须使用 MyBatis Plus 参数绑定，禁止拼接字符串
- API 返回结果不暴露密码、Token 等敏感信息

---

### 四、资源模块开发规范 (configJson / Processor / Template)

#### 1. configJson 配置结构

```java
// ConfigJson.java — 资源配置的根对象
@Data
public class ConfigJson {
    private List<FieldConfig> fields;
    private List<ProcessorConfig> parameterProcessors;
    private List<ProcessorConfig> resultProcessors;
    private List<QueryTemplate> queryTemplates;
    private DisplaySettings displaySettings;
}
```

- `fields`: 字段定义列表（名称、类型、是否可查询、是否返回、查询组件类型）
- `parameterProcessors`: SQL 执行前的参数处理器链
- `resultProcessors`: SQL 执行后的结果处理器链
- `queryTemplates`: 可选的多个 SQL 模板（支持占位符）
- `displaySettings`: 查询结果的列表列配置和详情页配置

configJson 作为 JSON 存储在 `table_resource.config_json` 字段，前端在 `TableResourceManagement.vue` 中以标签页形式进行可视化编辑。

#### 2. 新增处理器步骤

```java
// 1. 实现 Processor 接口
@Component
public class MyProcessor implements ParameterProcessor {
    @Override
    public String type() { return "my_processor"; }       // 唯一类型标识

    @Override
    public int order() { return 5; }                      // 执行顺序

    @Override
    public boolean enabled() { return true; }

    @Override
    public Map<String, Object> process(Map<String, Object> params,
                                        Map<String, Object> config) {
        // config 来自 configJson 中该处理器的 config 字段
        return params;
    }
}

// 2. 配置前端表单组件 (如需要)
// 在 src/views/resource/components/processor-forms/ 下新建 .vue
// 在 types/tableResource.ts 的 processorFormComponentMap 中注册
```

| 步骤 | 说明 |
|------|------|
| 创建 Java 类 | 实现 `ParameterProcessor` 或 `ResultProcessor`，加 `@Component` |
| 注册到 `ProcessorRegistry` | 自动扫描，无需手动注册 |
| 创建前端配置表单 | 在 `processor-forms/` 下新建组件 |
| 注册组件映射 | 在 `processorFormComponentMap` 中添加 type → 组件映射 |
| 添加到 configJson | 前端处理器编辑器中选择该类型即可 |

#### 3. 模板语法

```
{{fieldName}}         → 参数替换（自动参数化绑定防注入）
{{#if fieldName}}     → 条件包含（参数非空时渲染）
  SQL片段
{{/if}}
{{#unless fieldName}} → 条件排除（参数为空时渲染）
  SQL片段
{{/unless}}
```

- 模板存储在 `queryTemplates` 列表中，可配置多个
- 前端单资源查询时用户可切换模板
- SQL 注入防护：关键词黑名单 + 列白名单 + 参数化绑定三重防护

#### 4. 资源查询规范

| 场景 | 使用方式 |
|------|---------|
| 单资源精确查询 | `POST /api/resource/search/single`，传 resourceCode + params + templateName |
| 多资源统一 Key 查询 | `POST /api/resource/search/multi`，传 keyName + keyValue |
| 多资源关键词查询 | `POST /api/resource/search/keyword`，传 resourceCodes + keyword |
| 全文检索 | `POST /api/resource/search/fulltext`，传 query + indexName |

---

### 五、数据可视化开发规范 (Dashboard / Chart)

#### 1. 大屏设计器架构

```
DashboardDesigner.vue
  ├── 左侧：图表列表（从 dashboard_chart 读取）
  ├── 中间：vue-grid-layout 画布（拖拽调整位置/大小）
  └── 右侧：图表属性面板（标题、类型、数据源、SQL）
```

- 大屏数据 = 大屏定义（dashboard）+ 关联图表列表（dashboard_chart）
- 布局使用 `vue-grid-layout`，位置/大小信息存储在 `dashboard_chart.x/y/w/h` 字段
- 刷新机制：大屏配置 `refresh_interval`，前端轮询查询每个图表数据

#### 2. 新增图表类型步骤

```java
// 1. ChartRecommendService 中扩展推荐逻辑（如需要AI推荐）
//    在 recommendChartType(List<ColumnInfo> columns) 方法中
//    新增 chartType 的检测规则

// 2. 前端 DashboardDesigner 渲染对应图表
//    ECharts 配置在 DashboardView.vue 的 renderChart 函数中
//    根据 chartType 映射到不同的 ECharts option
```

| 步骤 | 说明 |
|------|------|
| 后端 | `ChartRecommendService` 规则引擎（按字段类型推荐） |
| 前端 | `DashboardDesigner` 选择图表类型，`DashboardView` 渲染 ECharts |
| 数据源 | `SqlEngine.execute()` 执行 SQL 获取数据 |
| 缓存 | `ChartCacheService` Redis 缓存，Key = chartId + sql md5 |

#### 3. SQL 引擎使用规范

```java
// 获取数据源对应的 SQL 引擎
SqlEngine engine = sqlEngineFactory.getEngine(datasourceId);

// 执行 SQL 查询
List<Map<String, Object>> result = engine.execute(datasourceId, sql, params);

// 测试连接
engine.testConnection(datasource);

// 获取表字段
List<ColumnInfo> columns = engine.getColumns(datasource, tableName);
```

- 支持 MySQL、PostgreSQL、Oracle、SQLServer、Hive
- 引擎按 datasourceId 缓存，复用 JDBC 连接
- 数据源密码解密使用 `ENCRYPT_AES_KEY`

---

### 六、数据采集开发规范

#### 1. 数据源管理

```java
// 新增数据源类型
// 1. pom.xml 添加 JDBC 驱动依赖
// 2. application.yaml 配置连接池（如需要）
// 3. SqlEngineFactory 注册新类型
```

| 数据库 | JDBC 驱动 | 已支持 |
|--------|-----------|--------|
| MySQL | `mysql-connector-j` | ✅ |
| PostgreSQL | `postgresql` | ✅ |
| Oracle | `ojdbc11` | ✅ |
| SQLServer | `mssql-jdbc` | ✅ |
| 达梦 | `DmJdbcDriver` | ❌（注释待启用） |
| 人大金仓 | `kingbase8` | ❌（注释待启用） |

#### 2. 全文检索接入

```yaml
fulltext:
  search:
    type: meilisearch    # 或 elasticsearch
    meilisearch:
      host: http://localhost:7700
      api-key: ""
    elasticsearch:
      host: http://localhost:9200
      api-key: ""
  storage:
    type: minio          # 或 s3, local
    minio:
      endpoint: http://localhost:9000
      access-key: minioadmin
      secret-key: minioadmin
      bucket: easy-lowcode-docs
    s3:
      region: us-east-1
      bucket: easy-lowcode-docs
    local:
      path: ./data/docs
```

接入流程：上传文件 → StorageService 存储 → TikaContentExtractor 提取文本 → SearchService 索引

#### 3. 搜索引擎接入规范

```java
// 实现 SearchService 接口
@Component
public class CustomSearchService implements SearchService {
    @Override
    public List<Map<String, Object>> search(String index, String query,
                                            Map<String, Object> options) {
        // 实现搜索逻辑
    }

    @Override
    public void indexDocument(String index, String id, Map<String, Object> doc) {
        // 实现文档索引
    }

    @Override
    public void deleteDocument(String index, String id) {
        // 实现文档删除
    }
}
```

#### 4. 存储后端接入规范

```java
// 实现 StorageService 接口
@Component
public class CustomStorageService implements StorageService {
    @Override
    public String upload(String fileName, byte[] content, String contentType) {
        // 返回存储路径
    }

    @Override
    public byte[] download(String path) { ... }

    @Override
    public void delete(String path) { ... }

    @Override
    public String getUrl(String path) { ... }
}
```

---

### 七、前端页面开发规范

#### 1. 动态菜单注册

添加一个新页面需要执行 3 步：

**Step 1: 后端 — 菜单表插入**
```xml
<!-- db/changelog/xxx-add-new-menu.xml -->
<insert tableName="sys_menu">
  <column name="id" valueComputed="true"/>
  <column name="menu_name" value="新功能"/>
  <column name="parent_id" value="上级菜单ID"/>
  <column name="path" value="module/feature"/>
  <column name="component" value="module/FeaturePage.vue"/>
  <column name="permission" value="module:feature:list"/>
  <column name="menu_type" value="C"/>
  <column name="sort" value="1"/>
  <column name="icon" value="Setting"/>
</insert>
```

**Step 2: 前端 — 注册组件映射**
```typescript
// stores/menu.ts — componentMap 中添加映射
const componentMap: Record<string, Component> = {
  'system/UserManagement.vue': defineAsyncComponent(() => import('@/views/system/UserManagement.vue')),
  'module/FeaturePage.vue': defineAsyncComponent(() => import('@/views/module/FeaturePage.vue')),
}
```

**Step 3: 前端 — 创建页面组件**
```vue
<script setup lang="ts">
// 使用 useTable + useCrudDialog 快速生成 CRUD 页面
import { usePagination, useSearchForm, useConfirmDelete } from '@/composables/useTable'
import { useCrudDialog } from '@/composables/useCrudDialog'
import { featureApi } from '@/api/feature'
import { onMounted } from 'vue'

const { pagination, loading, handleSizeChange, handleCurrentChange, resetPagination } = usePagination()
const { form, searchForm, handleSearch, handleReset } = useSearchForm({ keyword: '' })
const { handleDelete, deleteLoading } = useConfirmDelete(featureApi.delete)
const { dialogVisible, dialogTitle, dialogMode, formData, submitting,
        openAdd, openEdit, openView, closeDialog, handleSubmit } =
  useCrudDialog({
    fetchList: fetchData,
    createFn: featureApi.create,
    updateFn: featureApi.update,
    deleteFn: featureApi.delete,
  })

async function fetchData() {
  loading.value = true
  try {
    const res = await featureApi.getPage({ current: pagination.current, size: pagination.size, ...searchForm })
    tableData.value = res.records
    pagination.total = res.total
  } finally { loading.value = false }
}

onMounted(() => fetchData())
</script>
```

#### 2. API 文件命名与导出

```typescript
// ✅ api/feature.ts — 按业务模块拆分
import { createCrudApi } from './base'
import request from '@/utils/request'
import type { FeatureInfo } from '@/types/feature'

// 标准 CRUD 使用工厂函数一行生成
export const featureApi = createCrudApi<FeatureInfo>('/module/feature')

// 自定义接口单独写
export function customRequest(data: Params): Promise<Result> {
  return request({ url: '/module/feature/custom', method: 'post', data })
}
```

- 文件名 = 模块名小写（feature.ts）
- 导出变量 = 模块名 + Api（featureApi）
- 偏好使用 `createCrudApi` 工厂函数
- 自定义接口使用具名函数导出

#### 3. 页面组件命名约定

| 场景 | 命名 | 示例 |
|------|------|------|
| 列表页 (CRUD) | `{Feature}Management.vue` | `UserManagement.vue` |
| 配置页 | `{Feature}Config.vue` | `UnifiedKeyManagement.vue` |
| 设计器 | `{Feature}Designer.vue` | `DashboardDesigner.vue` |
| 查看页 | `{Feature}View.vue` | `DashboardView.vue` |
| 搜索页 | `{Feature}Search.vue` | `SingleResourceSearch.vue` |

#### 4. 本地组件 vs 公共组件

```
components/           # 公共通用组件（跨模块复用）
├── TableCard.vue      # 表格卡片 + 分页
├── SearchCard.vue     # 搜索表单卡片
├── DialogForm.vue     # 弹窗表单
├── ActionButtons.vue  # 操作按钮组
└── StatusTag.vue      # 状态标签

views/{module}/components/   # 模块局部组件（仅当前模块使用）
└── processor-forms/          # 处理器配置表单
```

- 公共组件放在 `src/components/`，模块局部组件放在 `views/{module}/components/`
- 公共组件必须加完善的 Props/Emits 类型声明
- 局部组件文件命名使用 camelCase

#### 5. Store 模式

```typescript
// stores/feature.ts
export const useFeatureStore = defineStore('feature', () => {
  // state — 用 ref/reactive
  const list = ref<FeatureInfo[]>([])

  // getters — 用 computed
  const activeList = computed(() => list.value.filter(i => i.status === 1))

  // actions — 用普通函数
  async function fetchList() {
    list.value = await featureApi.getList()
  }

  return { list, activeList, fetchList }
})
```

- 命名: `use{Name}Store`
- 语法: 优先 setup 方式（函数式）
- 职责: 只管理全局状态，组件内部状态用 `ref` 或 composable

---

### 八、版本演进路线图

#### MVP (当前版本 v1.0.0-SNAPSHOT)

| 模块 | 状态 |
|------|------|
| 系统管理（用户/角色/菜单/部门） | ✅ 完成 |
| 数据采集（多数据源 CRUD + 测试连接） | ✅ 完成 |
| 资源管理（表资源 configJson 完整配置 + 处理器链 + API 注册） | ✅ 完成 |
| 资源查询（单资源/多资源统一 Key/全文检索） | ✅ 完成 |
| 数据可视化（大屏设计器 + 图表管理 + Text-to-SQL） | ✅ 完成 |
| AI 模块（多 Provider 对话 + 会话管理） | ✅ 完成 |
| ETL（Spring Batch 任务 + 同步配置） | ✅ 完成 |

#### v1.1 规划

| 特性 | 说明 |
|------|------|
| 代码生成器 | 基于表资源自动生成 Controller/Service/Mapper/前端 CRUD 页面 |
| 可视化 ETL 编排 | 拖拽式 ETL 流程设计 |
| 大屏模板市场 | 预置大屏模板，一键导入 |
| 数据导出增强 | PDF/Excel 带格式导出查询结果 |
| 操作审计日志 | 所有敏感操作记录日志 |

#### v1.2 规划

| 特性 | 说明 |
|------|------|
| 表单设计器 | 拖拽式表单设计 + 数据绑定 + 表单渲染引擎 |
| 流程设计器 | Flowable 可视化 BPMN 设计器 |
| 多租户支持 | SaaS 多租户数据隔离 |
| LDAP/OAuth2 集成 | 企业级身份认证集成 |
| 在线文件预览 | PDF/Word/Excel 在线预览 |

#### 后端构建

```bash
mvn clean install -DskipTests
```

#### 前端构建

```bash
cd easy-lowcode-frontend && npm ci && npm run build
```

> 关于项目更详细的技术决策和架构设计，请参考 `docs/architecture.md`。
