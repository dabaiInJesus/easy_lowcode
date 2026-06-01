# 贡献指南

感谢您对 Easy Lowcode 项目的关注！我们欢迎任何形式的贡献，包括 bug 修复、功能开发、文档改进等。

## 📋 目录

- [行为准则](#行为准则)
- [如何贡献](#如何贡献)
- [开发流程](#开发流程)
- [代码规范](#代码规范)
- [提交规范](#提交规范)
- [Pull Request 指南](#pull-request-指南)

## 行为准则

本项目采用开源社区行为准则，期望所有参与者遵守：

- 使用友好和包容的语言
- 尊重不同的观点和经验
- 优雅地接受建设性批评
- 关注对社区最有利的事情
- 对其他社区成员表示同理心

## 如何贡献

### 报告 Bug

如果您发现了 bug，请创建一个 Issue 并包含：

1. **清晰的标题**：简明扼要地描述问题
2. **复现步骤**：详细说明如何复现该问题
3. **预期行为**：描述您期望发生什么
4. **实际行为**：描述实际发生了什么
5. **环境信息**：
   - JDK 版本
   - 操作系统
   - 数据库版本
   - 其他相关信息
6. **错误日志**：如果有异常堆栈，请一并提供

### 提出新功能

如果您有新功能的想法，请先创建一个 Issue 讨论：

1. **功能描述**：详细描述这个功能
2. **使用场景**：说明为什么需要这个功能
3. **实现思路**：如果有，可以提出大致的实现方案
4. **替代方案**：是否考虑过其他解决方案

### 代码贡献

1. Fork 本仓库
2. 创建您的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交您的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

## 开发流程

### 1. 环境准备

确保您的开发环境满足以下要求：

- JDK 21+
- Maven 3.6+
- PostgreSQL 17
- Redis 6+
- Git

### 2. 获取代码

```bash
# Fork 仓库后，克隆到您的本地
git clone https://github.com/YOUR_USERNAME/easy_lowcode.git
cd easy_lowcode

# 添加上游仓库
git remote add upstream https://github.com/ORIGINAL_OWNER/easy_lowcode.git
```

### 3. 保持同步

```bash
# 获取上游最新代码
git fetch upstream

# 合并到主分支
git checkout master
git merge upstream/master
```

### 4. 创建分支

```bash
# 功能开发
git checkout -b feature/your-feature-name

# Bug 修复
git checkout -b fix/issue-number
```

### 5. 开发测试

```bash
# 编译项目
mvn clean install

# 运行测试
mvn test

# 启动应用
cd easy-lowcode-startup
mvn spring-boot:run
```

### 6. 提交代码

```bash
git add .
git commit -m "type: description"
git push origin your-branch-name
```

## 代码规范

### Java 编码规范

#### 1. 命名规范

- **类名**：使用大驼峰命名（PascalCase）
  ```java
  public class UserServiceImpl { }
  ```

- **方法名**：使用小驼峰命名（camelCase）
  ```java
  public void getUserById() { }
  ```

- **常量**：全部大写，下划线分隔
  ```java
  public static final String DEFAULT_ENCODING = "UTF-8";
  ```

- **变量**：使用小驼峰命名，要有意义
  ```java
  String userName;  // ✅ 好
  String str;       // ❌ 不好
  ```

#### 2. 注释规范

- **类注释**：每个类都必须有 Javadoc 注释
  ```java
  /**
   * 用户服务实现类
   * 
   * @author Your Name
   * @since 1.0.0
   */
  public class UserServiceImpl implements UserService {
  }
  ```

- **方法注释**：公共方法必须有 Javadoc
  ```java
  /**
   * 根据ID查询用户
   * 
   * @param id 用户ID
   * @return 用户信息
   */
  public User getById(Long id) {
  }
  ```

- **行内注释**：解释复杂的业务逻辑
  ```java
  // 先检查用户状态，再执行后续操作
  if (user.getStatus() == 1) {
      // 业务逻辑
  }
  ```

#### 3. 代码格式

- 使用 4 个空格缩进
- 每行代码不超过 120 字符
- 方法长度不超过 80 行
- 类长度不超过 500 行

#### 4. 最佳实践

**使用 Lombok 简化代码：**
```java
@Data
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;
}
```

**统一异常处理：**
```java
if (user == null) {
    throw new BusinessException("用户不存在");
}
```

**统一返回结果：**
```java
@GetMapping("/{id}")
public Result<User> getById(@PathVariable Long id) {
    return Result.success(userService.getById(id));
}
```

**使用 Lambda 表达式：**
```java
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getUsername, username);
```

### 数据库规范

#### 1. 表命名

- 使用小写字母和下划线
- 添加模块前缀
  ```sql
  sys_user      -- 系统模块用户表
  auth_role     -- 认证模块角色表
  ```

#### 2. 字段命名

- 使用小写字母和下划线
- 主键统一命名为 `id`
- 外键使用 `关联表名_id`
  ```sql
  id            -- 主键
  user_id       -- 外键
  create_time   -- 创建时间
  ```

#### 3. 必备字段

每张表都应包含以下字段：
```sql
id BIGINT PRIMARY KEY,              -- 主键
create_time TIMESTAMP,              -- 创建时间
update_time TIMESTAMP,              -- 更新时间
create_by BIGINT,                   -- 创建人
update_by BIGINT,                   -- 更新人
deleted INTEGER DEFAULT 0           -- 逻辑删除
```

## 提交规范

### Commit Message 格式

遵循 [Conventional Commits](https://www.conventionalcommits.org/) 规范：

```
type(scope): description

[optional body]

[optional footer(s)]
```

### Type 类型

- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响代码运行）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建过程或辅助工具的变动

### 示例

```bash
# 新功能
git commit -m "feat(auth): 添加用户登录功能"

# Bug 修复
git commit -m "fix(user): 修复用户查询分页错误"

# 文档更新
git commit -m "docs: 更新 API 文档"

# 重构
git commit -m "refactor(database): 优化数据库配置"
```

## Pull Request 指南

### 提交 PR 前检查清单

- [ ] 代码遵循项目规范
- [ ] 添加了必要的注释
- [ ] 更新了相关文档
- [ ] 通过了所有测试
- [ ] 没有合并冲突
- [ ] Commit 信息清晰明了

### PR 模板

```markdown
## 描述
简要描述这个 PR 做了什么改动

## 类型
- [ ] Bug 修复
- [ ] 新功能
- [ ] 文档更新
- [ ] 重构
- [ ] 其他（请说明）

## 相关问题
Fixes #123

## 测试
描述如何测试这些改动

## 截图（如果适用）
添加截图以帮助理解改动

## 检查清单
- [ ] 代码遵循规范
- [ ] 已添加测试
- [ ] 文档已更新
- [ ] 无合并冲突
```

### PR 审查流程

1. 自动检查（CI）
   - 代码编译
   - 单元测试
   - 代码质量检查

2. 人工审查
   - 代码质量
   - 功能正确性
   - 性能影响
   - 安全性

3. 合并
   - 至少需要 1 个维护者批准
   - 所有 CI 检查通过
   - 解决所有评论

## 文档贡献

文档同样重要，欢迎改进：

- README.md
- DEVELOPMENT.md
- QUICK_START.md
- 代码注释
- API 文档

### 文档规范

- 使用 Markdown 格式
- 保持简洁清晰
- 提供实际示例
- 及时更新过时内容
- 中英文对照（可选）

## 测试要求

### 单元测试

为新功能编写单元测试：

```java
@SpringBootTest
class UserServiceTest {
    
    @Autowired
    private UserService userService;
    
    @Test
    void testGetById() {
        User user = userService.getById(1L);
        assertNotNull(user);
        assertEquals("admin", user.getUsername());
    }
}
```

### 集成测试

测试完整的业务流程：

```java
@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @Test
    void testLogin() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
```

## 版本发布

### 版本号规范

遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/)：

- **主版本号**：不兼容的 API 修改
- **次版本号**：向下兼容的功能性新增
- **修订号**：向下兼容的问题修正

例如：`1.0.0` → `1.1.0` → `1.1.1`

### 发布流程

1. 更新版本号
2. 更新 CHANGELOG.md
3. 创建 Release Tag
4. 编写发布说明
5. 打包发布

## 社区交流

- 💬 加入讨论组
- 📧 发送邮件至：support@dabai.com
- 🐛 提交 Issue
- 💡 参与讨论

## 致谢

感谢所有为这个项目做出贡献的开发者！

---

**再次感谢您的贡献！** 🎉

让我们一起把这个项目做得更好！
