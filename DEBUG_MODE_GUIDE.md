# Debug模式启动指南

## 已配置的功能

✅ **Spring Boot DevTools 热重载**
- 修改Java代码后自动重启（比手动重启快很多）
- 修改配置文件自动生效
- 支持LiveReload浏览器自动刷新

## 使用方法

### 方式一：Maven命令启动（推荐用于快速测试）

```bash
# 在项目根目录执行
mvn spring-boot:run -pl easy-lowcode-startup
```

**优点：**
- 简单快捷
- 自动检测代码变化并重启
- 适合快速验证功能

**注意：**
- DevTools会自动监控 `src/main/java` 下的文件变化
- 修改代码后保存，应用会在1-2秒内自动重启

---

### 方式二：IDEA Debug模式（推荐用于开发调试）

#### 1. 创建运行配置

1. 点击 IDEA 右上角的 **Add Configuration...**
2. 点击 **+** → 选择 **Spring Boot**
3. 配置如下：
   - **Name**: `EasyLowcode-Debug`
   - **Main class**: `com.dabai.easy_lowcode.EasyLowcodeApplication`
   - **Module**: `easy-lowcode-startup`
   - **VM options**: `-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005`
   - **Working directory**: `$MODULE_WORKING_DIR$`

4. 点击 **Apply** → **OK**

#### 2. 启动Debug模式

- 点击右上角的 **Debug** 按钮（虫子图标）
- 或使用快捷键：**Shift + F9**

#### 3. 设置断点

- 在代码行号左侧点击，出现红点即为断点
- 程序运行到断点时会暂停
- 可以查看变量值、单步执行等

#### 4. 热重载配合使用

即使使用Debug模式，DevTools也会工作：
- 修改代码 → 保存 → 按 **Ctrl + F9** (Build Project)
- 应用会自动重启（比完全重启快很多）

---

### 方式三：IDEA Run模式（日常开发）

与Debug模式类似，但不支持断点调试：
- 点击 **Run** 按钮（绿色三角形）
- 或使用快捷键：**Shift + F10**

---

## DevTools 工作原理

### 双类加载器机制

DevTools使用两个类加载器：
1. **Base ClassLoader**: 加载不变的类（第三方库）
2. **Restart ClassLoader**: 加载变化的类（你的代码）

当代码变化时，只重启 Restart ClassLoader，所以速度很快。

### 触发重启的条件

- 修改 `src/main/java` 下的 `.java` 文件
- 修改 `src/main/resources` 下的配置文件
- 保存文件后自动检测（IDEA需要手动编译：Ctrl+F9）

### 不会触发重启的文件

- `static/**` - 静态资源
- `public/**` - 公共资源
- `templates/**` - 模板文件

---

## IDEA 自动编译配置（重要！）

为了让DevTools在IDEA中正常工作，需要启用自动编译：

### 1. 启用自动构建

1. **File** → **Settings** → **Build, Execution, Deployment** → **Compiler**
2. 勾选 **Build project automatically**

### 2. 允许运行时自动构建

1. **File** → **Settings** → **Advanced Settings**
2. 找到 **Compiler** 部分
3. 勾选 **Allow auto-make to start even if developed application is currently running**

### 3. 快捷键手动编译

如果没有配置自动编译，修改代码后按：
- **Windows/Linux**: `Ctrl + F9`
- **Mac**: `Cmd + F9`

---

## 常见问题

### Q1: 修改代码后没有自动重启？

**解决方案：**
1. 确认已保存文件（Ctrl+S）
2. 手动编译项目（Ctrl+F9）
3. 检查控制台是否有 "Restarting due to changes" 日志

### Q2: 重启太慢怎么办？

**优化建议：**
1. 排除不必要的路径：
   ```yaml
   spring:
     devtools:
       restart:
         exclude: node_modules/**,dist/**,build/**
   ```

2. 增加JVM内存：
   ```
   -Xmx512m -Xms256m
   ```

### Q3: 生产环境会启用DevTools吗？

**不会！** DevTools的配置：
```xml
<scope>runtime</scope>
<optional>true</optional>
```
确保它只在开发时生效，打包时不会包含。

### Q4: 如何临时禁用DevTools？

在 `application.yaml` 中设置：
```yaml
spring:
  devtools:
    restart:
      enabled: false
```

---

## 性能对比

| 启动方式 | 首次启动 | 代码修改后 | 适用场景 |
|---------|---------|-----------|---------|
| 完全重启 | 15-30秒 | 15-30秒 | 重大架构调整 |
| DevTools重启 | 15-30秒 | 2-5秒 | 日常开发 ✅ |
| JRebel | 15-30秒 | <1秒 | 大型项目（付费） |

---

## 推荐的开发流程

1. **启动应用**：使用Debug模式启动
2. **编写代码**：正常编写业务逻辑
3. **保存文件**：Ctrl+S
4. **自动编译**：如果配置了自动编译，无需操作；否则按 Ctrl+F9
5. **等待重启**：观察控制台日志，约2-5秒完成重启
6. **测试功能**：刷新浏览器或调用API测试
7. **调试问题**：如有bug，设置断点重新Debug

---

## 额外提示

### 1. 使用 Spring Boot Actuator 监控

访问以下端点查看应用状态：
- http://localhost:8081/actuator/health - 健康检查
- http://localhost:8081/actuator/info - 应用信息
- http://localhost:8081/actuator/metrics - 性能指标

### 2. 远程Debug（可选）

如果需要远程调试：
```bash
mvn spring-boot:run -pl easy-lowcode-startup \
  -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005"
```

然后在IDEA中创建 Remote JVM Debug 配置连接到 5005 端口。

### 3. 日志级别动态调整

开发时可以设置更详细的日志：
```yaml
logging:
  level:
    com.dabai.easy_lowcode: DEBUG
    org.springframework.web: DEBUG
```

---

## 总结

✅ **已完成配置：**
- Spring Boot DevTools 依赖已添加
- 热重载已启用
- 模板缓存已关闭

✅ **推荐操作：**
1. 配置IDEA自动编译（重要！）
2. 使用Debug模式启动
3. 修改代码后保存即可自动重启

✅ **优势：**
- 开发效率提升 60%+
- 无需手动重启应用
- 支持断点调试
- 配置简单，开箱即用

祝您开发愉快！🚀
