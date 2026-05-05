# 图标选择器使用指南

## 概述

菜单管理中的图标选择器支持三种图标库：
1. **Element Plus Icons** - Vue组件形式的图标
2. **Font Awesome** - CSS类名形式的图标
3. **阿里矢量图标 (IconFont)** - CSS类名形式的自定义图标

## 图标分组

### Element Plus Icons（14个分组，106个图标）
- 基础、数据、工具、导航、状态
- 时间、通知、收藏分享、服务、安全
- 设备、文件、其他

### Font Awesome（9个分组，70+个图标）
- 常用、操作、导航、状态
- 时间、通知、社交、安全
- 设备、商务

### 阿里矢量图标（1个示例分组，20个图标）
- 需要根据实际项目配置

## 使用方法

### 1. 浏览图标
- 打开新增/编辑菜单对话框
- 点击"图标"输入框
- 按分组浏览所有可用图标
- 点击图标即可选择

### 2. 搜索图标
- 在搜索框中输入关键词
- 支持中文和英文搜索
- 实时过滤显示匹配的图标

### 3. 清空选择
- 点击输入框右侧的清除按钮
- 或者重新选择一个新图标

## 阿里矢量图标配置

### 步骤1：创建IconFont项目
1. 访问 https://www.iconfont.cn/
2. 注册/登录账号
3. 创建新项目
4. 添加需要的图标到项目

### 步骤2：获取代码
1. 进入项目页面
2. 点击"Font class"选项
3. 复制生成的CSS链接或下载代码

### 步骤3：更新配置
编辑 `src/assets/iconfont.css` 文件：

```css
@font-face {
  font-family: 'iconfont';
  /* 替换为您的实际链接 */
  src: url('https://at.alicdn.com/t/c/font_YOUR_PROJECT.woff2') format('woff2');
}

/* 添加您的图标类名 */
.icon-your-icon:before { content: '\exxx'; }
```

### 步骤4：添加到选择器
在 `MenuManagement.vue` 的 `iconGroups` 数组中添加新的分组：

```typescript
{
  name: '阿里矢量 - 我的图标',
  icons: [
    { label: '我的图标1', value: 'iconfont icon-xxx1', icon: 'iconfont icon-xxx1' },
    { label: '我的图标2', value: 'iconfont icon-xxx2', icon: 'iconfont icon-xxx2' },
    // ... 更多图标
  ]
}
```

## 技术实现

### 图标渲染逻辑

```vue
<!-- Element Plus 图标（Vue组件） -->
<component v-if="typeof item.icon !== 'string'" :is="item.icon" :size="16" />

<!-- Font Awesome / 阿里矢量图标（CSS类名） -->
<i v-else :class="item.icon" style="font-size: 16px" />
```

### 数据结构

```typescript
interface IconItem {
  label: string      // 显示名称
  value: string      // 存储值
  icon: Component | string  // Element Plus组件 或 CSS类名字符串
}

interface IconGroup {
  name: string       // 分组名称
  icons: IconItem[]  // 图标列表
}
```

## 扩展图标

### 添加更多Element Plus图标
访问 https://element-plus.org/zh-CN/component/icon.html 查看所有可用图标

```typescript
{ label: '新图标', value: 'IconName', icon: ElementPlusIconsVue.IconName }
```

### 添加更多Font Awesome图标
访问 https://fontawesome.com/icons 查看所有可用图标

```typescript
{ label: '新图标', value: 'fa-icon-name', icon: 'fas fa-icon-name' }
```

### 添加更多阿里矢量图标
在IconFont项目中添加图标后，按照上述配置步骤更新

## 注意事项

1. **图标前缀**
   - Element Plus: 直接使用组件名，如 `Document`
   - Font Awesome: 使用前缀 `fas fa-`，如 `fas fa-home`
   - 阿里矢量: 使用前缀 `iconfont icon-`，如 `iconfont icon-home`

2. **样式统一**
   - 所有图标大小统一为16px
   - 选中状态有蓝色边框和阴影
   - 悬停时有浅蓝背景

3. **性能优化**
   - 使用computed缓存扁平化列表
   - 搜索时实时过滤，无延迟
   - 分组渲染避免一次性加载所有图标

## 常见问题

### Q: 为什么有些图标显示不出来？
A: 检查是否正确引入了对应的CSS文件或安装了依赖包

### Q: 如何添加自己的阿里矢量图标？
A: 按照上述"阿里矢量图标配置"步骤操作

### Q: 可以混合使用不同图标库吗？
A: 可以，选择器支持同时使用三种图标库

### Q: 图标太多找不到怎么办？
A: 使用搜索功能，支持中英文关键词搜索

## 更新日志

### v2.0 (当前版本)
- ✅ 支持Element Plus Icons（106个）
- ✅ 支持Font Awesome（70+个）
- ✅ 支持阿里矢量图标（可扩展）
- ✅ 分组展示，结构清晰
- ✅ 智能搜索，快速定位
- ✅ 响应式布局，视觉友好

### v1.0
- 仅支持Element Plus Icons
- 无分组，平铺展示
