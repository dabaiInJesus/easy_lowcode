## 📝 描述

<!-- 简要描述这个PR的改动内容 -->


## 🎯 关联Issue

<!-- 关联相关的Issue，如: Fixes #123 或 Related to #456 -->


## 📋 类型

请选择此PR的类型：

- [ ] 🎨 新功能 (feat) - 添加新功能
- [ ] 🐛 缺陷修复 (fix) - 修复bug或问题
- [ ] ⚡ 性能优化 (perf) - 性能改进
- [ ] 📖 文档更新 (docs) - 仅更新文档
- [ ] ♻️ 重构 (refactor) - 代码重构，无功能变化
- [ ] 🧪 测试 (test) - 添加或修改测试
- [ ] 🔧 构建/工具 (chore) - 依赖更新、配置变更等

## 🧪 测试检查清单

- [ ] ✅ 在本地通过所有单元测试
  ```bash
  mvn clean test
  cd easy-lowcode-frontend && npm test
  ```
- [ ] ✅ 有新增或修改的单元测试用例
- [ ] ✅ 测试覆盖率保持或提升（后端 ≥ 60%）
- [ ] ✅ 没有新的警告或错误信息

## 🔍 代码质量检查清单

- [ ] ✅ 代码遵循项目规范 ([AGENTS.md](../AGENTS.md))
- [ ] ✅ 命名符合规范 (后端: PascalCase类/lowerCamelCase方法，前端: camelCase)
- [ ] ✅ 添加了必要的日志输出（DEBUG/INFO级别）
- [ ] ✅ 没有硬编码的配置值（使用环境变量或配置文件）
- [ ] ✅ 异常处理完善，使用 GlobalExceptionHandler

## 📚 文档更新检查清表

- [ ] ✅ 更新了相关的API文档 (如新增/修改接口)
- [ ] ✅ 更新了README或开发指南 (如有架构变化)
- [ ] ✅ 添加了代码注释（复杂逻辑处）
- [ ] ✅ 更新了CHANGELOG或版本说明

## 🔐 安全性检查

- [ ] ✅ 没有泄露敏感信息（密钥、密码、API Key）
- [ ] ✅ 没有SQL注入漏洞（使用参数化查询）
- [ ] ✅ 适当的权限检查 (@PreAuthorize 注解)
- [ ] ✅ 敏感数据加密处理

## 📸 截图或演示（如适用）

<!-- 如果是UI变化，请提供截图或GIF演示 -->


## 💭 补充说明

<!-- 任何需要Review者注意的额外信息 -->


---

## 🤖 CI/CD 流程检查

这个PR将自动运行以下检查，**所有检查必须通过**才能合并：

- ✅ 后端编译 (mvn clean compile)
- ✅ 后端单元测试 (mvn test)
- ✅ 代码覆盖率检查 (jacoco:check)
- ✅ 前端类型检查 (npx vue-tsc --noEmit)
- ✅ 前端ESLint检查 (npm run lint)
- ✅ 前端构建 (npm run build)
- ✅ 安全扫描 (Trivy)

如果CI失败，请在合并前修复所有问题。
