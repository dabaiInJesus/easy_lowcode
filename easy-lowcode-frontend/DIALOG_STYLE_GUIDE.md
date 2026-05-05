# 弹框统一配置指南

## 概述

本项目已配置全局弹框样式，确保所有弹框都有固定高度，不会因为弹框内容过多而导致整个页面出现滚动条。

## 全局样式配置

### 位置
`src/style.css`

### 配置内容

```css
/* 防止弹框打开时页面出现滚动条 */
.el-overlay {
  overflow: hidden !important;
}

/* 弹框主体最大高度限制 */
.el-dialog__body {
  max-height: calc(100vh - 200px);
  overflow-y: auto;
  padding-right: 10px; /* 避免滚动条遮挡内容 */
}

/* 小屏幕适配 */
@media (max-width: 768px) {
  .el-dialog__body {
    max-height: calc(100vh - 150px);
  }
}
```

## 使用方法

### 方法1：自动应用（推荐）

所有 `el-dialog` 组件会自动应用全局样式，无需额外配置：

```vue
<el-dialog
  v-model="dialogVisible"
  title="标题"
  width="800px"
>
  <!-- 内容会自动限制高度并显示内部滚动条 -->
  <el-form>
    <!-- 表单内容 -->
  </el-form>
</el-dialog>
```

### 方法2：使用预定义样式类

如果需要更精确控制弹框内容区域的高度，可以使用以下预定义样式类：

#### 标准高度（500px）
```vue
<el-dialog v-model="dialogVisible" title="标题">
  <div class="dialog-content">
    <!-- 内容区域，最大高度500px -->
  </div>
</el-dialog>
```

#### 大高度（600px）
```vue
<el-dialog v-model="dialogVisible" title="标题" width="1000px">
  <div class="dialog-content-lg">
    <!-- 内容区域，最大高度600px -->
  </div>
</el-dialog>
```

#### 小高度（400px）
```vue
<el-dialog v-model="dialogVisible" title="标题" width="600px">
  <div class="dialog-content-sm">
    <!-- 内容区域，最大高度400px -->
  </div>
</el-dialog>
```

## 最佳实践

### 1. 弹框宽度建议

- **小弹框**: 600px - 简单表单、确认对话框
- **中弹框**: 800px - 常规表单、列表
- **大弹框**: 1000px - 复杂配置、多列布局

### 2. 弹框位置建议

```vue
<!-- 推荐：居中偏上 -->
<el-dialog top="50px" ...>

<!-- 或者使用默认居中 -->
<el-dialog ...>
```

### 3. 内容组织建议

对于内容较多的弹框，建议使用选项卡或步骤条来组织内容：

```vue
<el-dialog v-model="dialogVisible" title="复杂配置" width="1000px">
  <div class="dialog-content-lg">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="基础配置" name="basic">
        <!-- 基础配置内容 -->
      </el-tab-pane>
      <el-tab-pane label="高级配置" name="advanced">
        <!-- 高级配置内容 -->
      </el-tab-pane>
    </el-tabs>
  </div>
</el-dialog>
```

### 4. 表格在弹框中的使用

如果弹框中包含表格，建议设置最大高度：

```vue
<el-table 
  :data="tableData" 
  max-height="300"
  border
>
  <!-- 表格列定义 -->
</el-table>
```

## 示例

### 示例1：注册外部接口弹框

```vue
<el-dialog
  v-model="registerDialogVisible"
  title="注册外部接口"
  width="1000px"
  top="50px"
  :close-on-click-modal="false"
>
  <div class="dialog-content">
    <el-form label-width="120px">
      <!-- 表单内容 -->
    </el-form>
  </div>
</el-dialog>
```

### 示例2：生成API配置弹框

```vue
<el-dialog
  v-model="generateApiDialogVisible"
  title="生成API配置"
  width="900px"
  top="50px"
>
  <div class="dialog-content-lg">
    <el-form label-width="120px">
      <!-- 基本信息 -->
      <el-divider content-position="left">基本信息</el-divider>
      
      <!-- API配置 -->
      <el-divider content-position="left">API配置</el-divider>
      
      <!-- 字段查询配置 -->
      <el-divider content-position="left">字段查询配置</el-divider>
      <el-tabs v-model="queryFieldTab">
        <el-tab-pane label="显示字段" name="select">
          <!-- 显示字段配置 -->
        </el-tab-pane>
        <el-tab-pane label="查询条件" name="where">
          <!-- 查询条件配置 -->
        </el-tab-pane>
        <el-tab-pane label="排序字段" name="order">
          <!-- 排序字段配置 -->
        </el-tab-pane>
      </el-tabs>
    </el-form>
  </div>
</el-dialog>
```

## 注意事项

1. **不要在全局样式中重复定义** `.dialog-content` 等样式类
2. **弹框内容过多时**，优先使用选项卡或步骤条组织，而不是单纯增加高度
3. **移动端适配**已通过媒体查询自动处理
4. **滚动条样式**可以通过CSS自定义美化（可选）

## 自定义滚动条样式（可选）

如果需要美化滚动条，可以添加以下样式：

```css
/* Webkit浏览器滚动条样式 */
.dialog-content::-webkit-scrollbar,
.dialog-content-lg::-webkit-scrollbar,
.dialog-content-sm::-webkit-scrollbar,
.el-dialog__body::-webkit-scrollbar {
  width: 8px;
}

.dialog-content::-webkit-scrollbar-thumb,
.dialog-content-lg::-webkit-scrollbar-thumb,
.dialog-content-sm::-webkit-scrollbar-thumb,
.el-dialog__body::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 4px;
}

.dialog-content::-webkit-scrollbar-thumb:hover,
.dialog-content-lg::-webkit-scrollbar-thumb:hover,
.dialog-content-sm::-webkit-scrollbar-thumb:hover,
.el-dialog__body::-webkit-scrollbar-thumb:hover {
  background-color: #c0c4cc;
}
```

## 更新记录

- **2026-05-05**: 创建全局弹框样式配置
- 统一所有弹框的高度限制
- 防止弹框导致页面出现滚动条
- 提供三种预设高度样式类
