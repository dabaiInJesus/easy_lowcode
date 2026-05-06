<template>
  <div class="menu-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>菜单管理</span>
          <el-button type="primary" @click="handleAdd">
            <el-icon><Plus /></el-icon>
            新增菜单
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe row-key="id" default-expand-all>
        <el-table-column type="index" label="序号" width="60" align="center" />
        <el-table-column prop="menuCode" label="菜单编码" min-width="150" show-overflow-tooltip />
        <el-table-column prop="menuName" label="菜单名称" min-width="150" />
        <el-table-column prop="path" label="路径" min-width="150" />
        <el-table-column prop="component" label="组件" min-width="150" />
        <el-table-column prop="icon" label="图标" width="100" />
        <el-table-column prop="menuType" label="类型" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.menuType === 1" type="primary">目录</el-tag>
            <el-tag v-else-if="row.menuType === 2" type="success">菜单</el-tag>
            <el-tag v-else type="info">按钮</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" />
        <el-table-column prop="visible" label="显示" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.visible === 1" type="success">显示</el-tag>
            <el-tag v-else type="danger">隐藏</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button link type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="dialogTitle"
      width="600px"
      top="100px"
      @close="handleDialogClose"
    >
      <div class="dialog-content">
        <el-form
          ref="formRef"
          :model="formData"
          :rules="formRules"
          label-width="100px"
        >
          <el-form-item label="父级菜单" prop="parentId">
            <el-tree-select
              v-model="formData.parentId"
              :data="treeSelectData"
              :props="{ label: 'menuName', value: 'id', children: 'children' }"
              placeholder="请选择父级菜单"
              check-strictly
              clearable
              style="width: 100%"
            />
          </el-form-item>
          <el-form-item label="菜单名称" prop="menuName">
            <el-input
              v-model="formData.menuName"
              placeholder="请输入菜单名称"
              @input="handleMenuNameInput"
            />
          </el-form-item>
          <el-form-item label="菜单编码" prop="menuCode">
            <el-input
              v-model="formData.menuCode"
              placeholder="请输入菜单编码（英文）"
            />
            <div class="form-tip">建议使用英文，如：api_management</div>
          </el-form-item>
          <el-form-item label="菜单类型" prop="menuType">
            <el-radio-group v-model="formData.menuType">
              <el-radio :value="1">目录</el-radio>
              <el-radio :value="2">菜单</el-radio>
              <el-radio :value="3">按钮</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="路由地址" prop="path">
            <el-input v-model="formData.path" placeholder="/resource/api" />
          </el-form-item>
          <el-form-item label="组件路径" prop="component">
            <el-input v-model="formData.component" placeholder="resource/ApiManagement" />
          </el-form-item>
          <el-form-item label="权限标识" prop="perms">
            <el-input v-model="formData.perms" placeholder="resource:api:list" />
          </el-form-item>
          <el-form-item label="图标" prop="icon">
            <el-popover
              v-model:visible="iconPickerVisible"
              placement="bottom-start"
              :width="400"
              trigger="click"
            >
              <template #reference>
                <el-input
                  v-model="formData.icon"
                  placeholder="请选择图标"
                  readonly
                  clearable
                  @clear="formData.icon = ''"
                >
                  <template #prefix>
                    <!-- Element Plus 图标 -->
                    <component
                      v-if="formData.icon && !formData.icon.startsWith('fa') && !formData.icon.startsWith('iconfont')"
                      :is="getIconComponent(formData.icon)"
                      style="width: 16px; height: 16px"
                    />
                    <!-- Font Awesome / 阿里矢量图标 -->
                    <i
                      v-else-if="formData.icon"
                      :class="formData.icon"
                      style="font-size: 14px"
                    />
                  </template>
                </el-input>
              </template>
              
              <div class="icon-picker">
                <el-input
                  v-model="iconSearchKeyword"
                  placeholder="搜索图标..."
                  clearable
                  prefix-icon="Search"
                  style="margin-bottom: 10px"
                />
                
                <!-- 分组显示模式 -->
                <div v-if="!iconSearchKeyword" class="icon-groups">
                  <div v-for="group in iconGroups" :key="group.name" class="icon-group">
                    <div class="group-title">{{ group.name }}</div>
                    <div class="icon-grid">
                      <div
                        v-for="item in group.icons"
                        :key="item.value"
                        class="icon-item"
                        :class="{ selected: formData.icon === item.value }"
                        @click="selectIcon(item.value)"
                      >
                        <!-- Element Plus 图标 -->
                        <component
                          v-if="typeof item.icon !== 'string'"
                          :is="item.icon"
                          :size="16"
                        />
                        <!-- Font Awesome / 阿里矢量图标 -->
                        <i v-else :class="item.icon" style="font-size: 16px" />
                        <span class="icon-label">{{ item.label }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 搜索结果模式 -->
                <div v-else class="icon-grid">
                  <div
                    v-for="item in filteredIcons"
                    :key="item.value"
                    class="icon-item"
                    :class="{ selected: formData.icon === item.value }"
                    @click="selectIcon(item.value)"
                  >
                    <!-- Element Plus 图标 -->
                    <component
                      v-if="typeof item.icon !== 'string'"
                      :is="item.icon"
                      :size="16"
                    />
                    <!-- Font Awesome / 阿里矢量图标 -->
                    <i v-else :class="item.icon" style="font-size: 16px" />
                    <span class="icon-label">{{ item.label }}</span>
                  </div>
                </div>
              </div>
            </el-popover>
          </el-form-item>
          <el-form-item label="排序" prop="sort">
            <el-input-number v-model="formData.sort" :min="0" :max="999" />
          </el-form-item>
          <el-form-item label="显示状态" prop="visible">
            <el-radio-group v-model="formData.visible">
              <el-radio :value="1">显示</el-radio>
              <el-radio :value="0">隐藏</el-radio>
            </el-radio-group>
          </el-form-item>
        </el-form>
      </div>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import { getMenuList, createMenu, updateMenu, deleteMenu } from '@/api/auth'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

// 图标选项类型
interface IconOption {
  label: string
  value: string
  icon: any
}

// 常用图标选项 - 按分组组织
const iconGroups: { name: string; icons: IconOption[] }[] = [
  {
    name: 'Element Plus - 基础',
    icons: [
      { label: '文档', value: 'Document', icon: ElementPlusIconsVue.Document },
      { label: '设置', value: 'Setting', icon: ElementPlusIconsVue.Setting },
      { label: '用户', value: 'User', icon: ElementPlusIconsVue.User },
      { label: '主页', value: 'HomeFilled', icon: ElementPlusIconsVue.HomeFilled },
      { label: '菜单', value: 'Menu', icon: ElementPlusIconsVue.Menu },
      { label: '文件夹', value: 'Folder', icon: ElementPlusIconsVue.Folder },
      { label: '文件夹打开', value: 'FolderOpened', icon: ElementPlusIconsVue.FolderOpened },
    ]
  },
  {
    name: 'Element Plus - 数据',
    icons: [
      { label: '数据板', value: 'DataBoard', icon: ElementPlusIconsVue.DataBoard },
      { label: '数据表', value: 'Grid', icon: ElementPlusIconsVue.Grid },
      { label: '图表', value: 'TrendCharts', icon: ElementPlusIconsVue.TrendCharts },
      { label: '饼图', value: 'PieChart', icon: ElementPlusIconsVue.PieChart },
      { label: '折线图', value: 'LineChart', icon: ElementPlusIconsVue.PieChart },
      { label: '柱状图', value: 'Histogram', icon: ElementPlusIconsVue.Histogram },
      { label: '数据分析', value: 'DataAnalysis', icon: ElementPlusIconsVue.DataAnalysis },
      { label: '数据库', value: 'Coin', icon: ElementPlusIconsVue.Coin },
    ]
  },
  {
    name: 'Element Plus - 工具',
    icons: [
      { label: '工具', value: 'Tools', icon: ElementPlusIconsVue.Tools },
      { label: '搜索', value: 'Search', icon: ElementPlusIconsVue.Search },
      { label: '编辑', value: 'Edit', icon: ElementPlusIconsVue.Edit },
      { label: '删除', value: 'Delete', icon: ElementPlusIconsVue.Delete },
      { label: '添加', value: 'CirclePlus', icon: ElementPlusIconsVue.CirclePlus },
      { label: '刷新', value: 'Refresh', icon: ElementPlusIconsVue.Refresh },
      { label: '上传', value: 'Upload', icon: ElementPlusIconsVue.Upload },
      { label: '下载', value: 'Download', icon: ElementPlusIconsVue.Download },
      { label: '打印', value: 'Printer', icon: ElementPlusIconsVue.Printer },
      { label: '相机', value: 'Camera', icon: ElementPlusIconsVue.Camera },
      { label: '放大镜', value: 'ZoomIn', icon: ElementPlusIconsVue.ZoomIn },
    ]
  },
  {
    name: 'Element Plus - 导航',
    icons: [
      { label: '链接', value: 'Link', icon: ElementPlusIconsVue.Link },
      { label: '位置', value: 'Location', icon: ElementPlusIconsVue.Location },
      { label: '指南针', value: 'Compass', icon: ElementPlusIconsVue.Compass },
      { label: '地图', value: 'MapLocation', icon: ElementPlusIconsVue.MapLocation },
      { label: '箭头右', value: 'ArrowRight', icon: ElementPlusIconsVue.ArrowRight },
      { label: '箭头左', value: 'ArrowLeft', icon: ElementPlusIconsVue.ArrowLeft },
      { label: '箭头上', value: 'ArrowUp', icon: ElementPlusIconsVue.ArrowUp },
      { label: '箭头下', value: 'ArrowDown', icon: ElementPlusIconsVue.ArrowDown },
    ]
  },
  {
    name: 'Element Plus - 状态',
    icons: [
      { label: '检查', value: 'Check', icon: ElementPlusIconsVue.Check },
      { label: '关闭', value: 'Close', icon: ElementPlusIconsVue.Close },
      { label: '成功', value: 'SuccessFilled', icon: ElementPlusIconsVue.SuccessFilled },
      { label: '错误', value: 'CircleCloseFilled', icon: ElementPlusIconsVue.CircleCloseFilled },
      { label: '警告', value: 'Warning', icon: ElementPlusIconsVue.Warning },
      { label: '信息', value: 'InfoFilled', icon: ElementPlusIconsVue.InfoFilled },
      { label: '问号', value: 'QuestionFilled', icon: ElementPlusIconsVue.QuestionFilled },
      { label: '加载', value: 'Loading', icon: ElementPlusIconsVue.Loading },
    ]
  },
  {
    name: 'Element Plus - 时间',
    icons: [
      { label: '时钟', value: 'Clock', icon: ElementPlusIconsVue.Clock },
      { label: '日历', value: 'Calendar', icon: ElementPlusIconsVue.Calendar },
      { label: '闹钟', value: 'AlarmClock', icon: ElementPlusIconsVue.AlarmClock },
    ]
  },
  {
    name: 'Element Plus - 通知',
    icons: [
      { label: '铃铛', value: 'Bell', icon: ElementPlusIconsVue.Bell },
      { label: '消息', value: 'ChatDotRound', icon: ElementPlusIconsVue.ChatDotRound },
      { label: '评论', value: 'ChatLineRound', icon: ElementPlusIconsVue.ChatLineRound },
    ]
  },
  {
    name: 'Element Plus - 收藏分享',
    icons: [
      { label: '星号', value: 'Star', icon: ElementPlusIconsVue.Star },
      { label: '收藏', value: 'Collection', icon: ElementPlusIconsVue.Collection },
      { label: '分享', value: 'Share', icon: ElementPlusIconsVue.Share },
      { label: '点赞', value: 'ThumbUp', icon: ElementPlusIconsVue.Bottom },
      { label: '点踩', value: 'ThumbDown', icon: ElementPlusIconsVue.Top },
    ]
  },
  {
    name: 'Element Plus - 服务',
    icons: [
      { label: '服务', value: 'Service', icon: ElementPlusIconsVue.Service },
      { label: '旗帜', value: 'Flag', icon: ElementPlusIconsVue.Flag },
      { label: '奖杯', value: 'Trophy', icon: ElementPlusIconsVue.Trophy },
      { label: '奖牌', value: 'Medal', icon: ElementPlusIconsVue.Medal },
    ]
  },
  {
    name: 'Element Plus - 安全',
    icons: [
      { label: '锁', value: 'Lock', icon: ElementPlusIconsVue.Lock },
      { label: '解锁', value: 'Unlock', icon: ElementPlusIconsVue.Unlock },
      { label: '钥匙', value: 'Key', icon: ElementPlusIconsVue.Key },
      { label: '安全', value: 'Shield', icon: ElementPlusIconsVue.Key },
    ]
  },
  {
    name: 'Element Plus - 设备',
    icons: [
      { label: '手机', value: 'Cellphone', icon: ElementPlusIconsVue.Cellphone },
      { label: '电脑', value: 'Monitor', icon: ElementPlusIconsVue.Monitor },
      { label: '平板', value: 'Ipad', icon: ElementPlusIconsVue.Monitor },
      { label: '电视', value: 'VideoPlay', icon: ElementPlusIconsVue.VideoPlay },
    ]
  },
  {
    name: 'Element Plus - 文件',
    icons: [
      { label: '图片', value: 'Picture', icon: ElementPlusIconsVue.Picture },
      { label: '视频', value: 'VideoCamera', icon: ElementPlusIconsVue.VideoCamera },
      { label: '音乐', value: 'Headset', icon: ElementPlusIconsVue.Headset },
      { label: '麦克风', value: 'Microphone', icon: ElementPlusIconsVue.Microphone },
      { label: '文件', value: 'Files', icon: ElementPlusIconsVue.Files },
      { label: '笔记', value: 'Notebook', icon: ElementPlusIconsVue.Notebook },
    ]
  },
  {
    name: 'Element Plus - 其他',
    icons: [
      { label: '购物车', value: 'ShoppingCart', icon: ElementPlusIconsVue.ShoppingCart },
      { label: '商品', value: 'Goods', icon: ElementPlusIconsVue.Goods },
      { label: '钱包', value: 'Wallet', icon: ElementPlusIconsVue.Wallet },
      { label: '礼物', value: 'Present', icon: ElementPlusIconsVue.Present },
      { label: '太阳', value: 'Sunny', icon: ElementPlusIconsVue.Sunny },
      { label: '月亮', value: 'Moon', icon: ElementPlusIconsVue.Moon },
      { label: '云朵', value: 'Cloudy', icon: ElementPlusIconsVue.Cloudy },
      { label: '飞机', value: 'Airplane', icon: ElementPlusIconsVue.Van },
      { label: '汽车', value: 'Van', icon: ElementPlusIconsVue.Van },
    ]
  },
  {
    name: 'Font Awesome - 常用',
    icons: [
      { label: '主页', value: 'fa-home', icon: 'fas fa-home' },
      { label: '用户', value: 'fa-user', icon: 'fas fa-user' },
      { label: '设置', value: 'fa-cog', icon: 'fas fa-cog' },
      { label: '菜单', value: 'fa-bars', icon: 'fas fa-bars' },
      { label: '文件夹', value: 'fa-folder', icon: 'fas fa-folder' },
      { label: '文件', value: 'fa-file', icon: 'fas fa-file' },
      { label: '图片', value: 'fa-image', icon: 'fas fa-image' },
      { label: '视频', value: 'fa-video', icon: 'fas fa-video' },
      { label: '音乐', value: 'fa-music', icon: 'fas fa-music' },
    ]
  },
  {
    name: 'Font Awesome - 操作',
    icons: [
      { label: '搜索', value: 'fa-search', icon: 'fas fa-search' },
      { label: '编辑', value: 'fa-edit', icon: 'fas fa-edit' },
      { label: '删除', value: 'fa-trash', icon: 'fas fa-trash' },
      { label: '添加', value: 'fa-plus', icon: 'fas fa-plus' },
      { label: '刷新', value: 'fa-sync', icon: 'fas fa-sync' },
      { label: '上传', value: 'fa-upload', icon: 'fas fa-upload' },
      { label: '下载', value: 'fa-download', icon: 'fas fa-download' },
      { label: '打印', value: 'fa-print', icon: 'fas fa-print' },
      { label: '保存', value: 'fa-save', icon: 'fas fa-save' },
    ]
  },
  {
    name: 'Font Awesome - 导航',
    icons: [
      { label: '链接', value: 'fa-link', icon: 'fas fa-link' },
      { label: '位置', value: 'fa-map-marker', icon: 'fas fa-map-marker-alt' },
      { label: '箭头右', value: 'fa-arrow-right', icon: 'fas fa-arrow-right' },
      { label: '箭头左', value: 'fa-arrow-left', icon: 'fas fa-arrow-left' },
      { label: '箭头上', value: 'fa-arrow-up', icon: 'fas fa-arrow-up' },
      { label: '箭头下', value: 'fa-arrow-down', icon: 'fas fa-arrow-down' },
      { label: '外部链接', value: 'fa-external-link', icon: 'fas fa-external-link-alt' },
    ]
  },
  {
    name: 'Font Awesome - 状态',
    icons: [
      { label: '检查', value: 'fa-check', icon: 'fas fa-check' },
      { label: '关闭', value: 'fa-times', icon: 'fas fa-times' },
      { label: '成功', value: 'fa-check-circle', icon: 'fas fa-check-circle' },
      { label: '错误', value: 'fa-times-circle', icon: 'fas fa-times-circle' },
      { label: '警告', value: 'fa-exclamation-triangle', icon: 'fas fa-exclamation-triangle' },
      { label: '信息', value: 'fa-info-circle', icon: 'fas fa-info-circle' },
      { label: '问号', value: 'fa-question-circle', icon: 'fas fa-question-circle' },
      { label: '加载中', value: 'fa-spinner', icon: 'fas fa-spinner' },
    ]
  },
  {
    name: 'Font Awesome - 时间',
    icons: [
      { label: '时钟', value: 'fa-clock', icon: 'fas fa-clock' },
      { label: '日历', value: 'fa-calendar', icon: 'fas fa-calendar-alt' },
      { label: '历史', value: 'fa-history', icon: 'fas fa-history' },
    ]
  },
  {
    name: 'Font Awesome - 通知',
    icons: [
      { label: '铃铛', value: 'fa-bell', icon: 'fas fa-bell' },
      { label: '消息', value: 'fa-comment', icon: 'fas fa-comment' },
      { label: '邮件', value: 'fa-envelope', icon: 'fas fa-envelope' },
    ]
  },
  {
    name: 'Font Awesome - 社交',
    icons: [
      { label: '星号', value: 'fa-star', icon: 'fas fa-star' },
      { label: '心形', value: 'fa-heart', icon: 'fas fa-heart' },
      { label: '点赞', value: 'fa-thumbs-up', icon: 'fas fa-thumbs-up' },
      { label: '分享', value: 'fa-share', icon: 'fas fa-share-alt' },
      { label: '收藏', value: 'fa-bookmark', icon: 'fas fa-bookmark' },
    ]
  },
  {
    name: 'Font Awesome - 安全',
    icons: [
      { label: '锁', value: 'fa-lock', icon: 'fas fa-lock' },
      { label: '解锁', value: 'fa-unlock', icon: 'fas fa-unlock' },
      { label: '钥匙', value: 'fa-key', icon: 'fas fa-key' },
      { label: '盾牌', value: 'fa-shield', icon: 'fas fa-shield-alt' },
    ]
  },
  {
    name: 'Font Awesome - 设备',
    icons: [
      { label: '手机', value: 'fa-mobile', icon: 'fas fa-mobile-alt' },
      { label: '电脑', value: 'fa-desktop', icon: 'fas fa-desktop' },
      { label: '平板', value: 'fa-tablet', icon: 'fas fa-tablet-alt' },
      { label: '笔记本', value: 'fa-laptop', icon: 'fas fa-laptop' },
    ]
  },
  {
    name: 'Font Awesome - 商务',
    icons: [
      { label: '购物车', value: 'fa-shopping-cart', icon: 'fas fa-shopping-cart' },
      { label: '商品', value: 'fa-box', icon: 'fas fa-box' },
      { label: '钱包', value: 'fa-wallet', icon: 'fas fa-wallet' },
      { label: '信用卡', value: 'fa-credit-card', icon: 'fas fa-credit-card' },
      { label: '图表', value: 'fa-chart-bar', icon: 'fas fa-chart-bar' },
      { label: '数据', value: 'fa-database', icon: 'fas fa-database' },
    ]
  },
  {
    name: '阿里矢量 - 示例',
    icons: [
      { label: '主页', value: 'iconfont icon-home', icon: 'iconfont icon-home' },
      { label: '用户', value: 'iconfont icon-user', icon: 'iconfont icon-user' },
      { label: '设置', value: 'iconfont icon-setting', icon: 'iconfont icon-setting' },
      { label: '菜单', value: 'iconfont icon-menu', icon: 'iconfont icon-menu' },
      { label: '文件夹', value: 'iconfont icon-folder', icon: 'iconfont icon-folder' },
      { label: '文件', value: 'iconfont icon-file', icon: 'iconfont icon-file' },
      { label: '搜索', value: 'iconfont icon-search', icon: 'iconfont icon-search' },
      { label: '编辑', value: 'iconfont icon-edit', icon: 'iconfont icon-edit' },
      { label: '删除', value: 'iconfont icon-delete', icon: 'iconfont icon-delete' },
      { label: '添加', value: 'iconfont icon-add', icon: 'iconfont icon-add' },
      { label: '刷新', value: 'iconfont icon-refresh', icon: 'iconfont icon-refresh' },
      { label: '上传', value: 'iconfont icon-upload', icon: 'iconfont icon-upload' },
      { label: '下载', value: 'iconfont icon-download', icon: 'iconfont icon-download' },
      { label: '图表', value: 'iconfont icon-chart', icon: 'iconfont icon-chart' },
      { label: '数据', value: 'iconfont icon-data', icon: 'iconfont icon-data' },
      { label: '锁', value: 'iconfont icon-lock', icon: 'iconfont icon-lock' },
      { label: '星号', value: 'iconfont icon-star', icon: 'iconfont icon-star' },
      { label: '心形', value: 'iconfont icon-heart', icon: 'iconfont icon-heart' },
      { label: '点赞', value: 'iconfont icon-like', icon: 'iconfont icon-like' },
      { label: '分享', value: 'iconfont icon-share', icon: 'iconfont icon-share' },
    ]
  },
]

// 扁平化的图标列表（用于搜索）
const iconOptions = computed(() => {
  return iconGroups.flatMap(group => group.icons)
})

interface MenuItem {
  id: number
  parentId: number
  menuName: string
  menuCode?: string
  menuType: number
  path: string
  component: string
  icon: string
  sort: number
  visible: number
  perms?: string
  children?: MenuItem[]
}

const tableData = ref<MenuItem[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const submitLoading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 图标选择器相关
const iconPickerVisible = ref(false)
const iconSearchKeyword = ref('')

// 过滤后的图标列表
const filteredIcons = computed(() => {
  if (!iconSearchKeyword.value) {
    return iconOptions.value
  }
  const keyword = iconSearchKeyword.value.toLowerCase()
  return iconOptions.value.filter(item => 
    item.label.toLowerCase().includes(keyword) || 
    item.value.toLowerCase().includes(keyword)
  )
})

// 选择图标
const selectIcon = (iconName: string) => {
  formData.value.icon = iconName
  iconPickerVisible.value = false
}

// Helper to get icon component safely
const getIconComponent = (iconName: string) => {
  if (!iconName || iconName.startsWith('fa') || iconName.startsWith('iconfont')) return null
  return (ElementPlusIconsVue as any)[iconName]
}

// 表单数据
const formData = ref({
  id: 0,
  parentId: 0,
  menuName: '',
  menuCode: '',
  menuType: 2,
  path: '',
  component: '',
  perms: '',
  icon: '',
  sort: 1,
  visible: 1
})

// 表单验证规则
const formRules: FormRules = {
  menuName: [
    { required: true, message: '请输入菜单名称', trigger: 'blur' }
  ],
  menuCode: [
    { required: true, message: '请输入菜单编码', trigger: 'blur' },
    { pattern: /^[a-zA-Z][a-zA-Z0-9_]*$/, message: '菜单编码必须以字母开头，只能包含字母、数字和下划线', trigger: 'blur' }
  ],
  menuType: [
    { required: true, message: '请选择菜单类型', trigger: 'change' }
  ]
}

// 对话框标题
const dialogTitle = computed(() => isEdit.value ? '编辑菜单' : '新增菜单')

// 树形选择器数据
const treeSelectData = computed(() => {
  // 添加一个根节点选项
  return [{
    id: 0,
    menuName: '顶级菜单',
    children: tableData.value
  }]
})

// 加载菜单列表
const loadMenus = async () => {
  loading.value = true
  try {
    const res: any = await getMenuList()
    // 响应拦截器已经解包，res 就是数组
    tableData.value = res || []
  } catch (error) {
    ElMessage.error('加载菜单列表失败')
  } finally {
    loading.value = false
  }
}

// 新增菜单
const handleAdd = () => {
  isEdit.value = false
  resetForm()
  dialogVisible.value = true
}

// 编辑菜单
const handleEdit = (row: MenuItem) => {
  isEdit.value = true
  formData.value = {
    id: row.id,
    parentId: row.parentId,
    menuName: row.menuName,
    menuCode: row.menuCode || '',
    menuType: row.menuType,
    path: row.path,
    component: row.component,
    perms: row.perms || '',
    icon: row.icon,
    sort: row.sort,
    visible: row.visible
  }
  dialogVisible.value = true
}

// 删除菜单
const handleDelete = async (row: MenuItem) => {
  try {
    await ElMessageBox.confirm(`确定要删除菜单 "${row.menuName}" 吗？`, '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning',
    })
    
    await deleteMenu(row.id)
    ElMessage.success('删除成功')
    loadMenus()
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  
  await formRef.value.validate(async (valid) => {
    if (!valid) return
    
    submitLoading.value = true
    try {
      if (isEdit.value) {
        await updateMenu(formData.value)
        ElMessage.success('更新成功')
      } else {
        await createMenu(formData.value)
        ElMessage.success('创建成功')
      }
      dialogVisible.value = false
      loadMenus()
    } catch (error: any) {
      ElMessage.error(error.message || '操作失败')
    } finally {
      submitLoading.value = false
    }
  })
}

// 重置表单
const resetForm = () => {
  formData.value = {
    id: 0,
    parentId: 0,
    menuName: '',
    menuCode: '',
    menuType: 2,
    path: '',
    component: '',
    perms: '',
    icon: '',
    sort: 1,
    visible: 1
  }
  formRef.value?.clearValidate()
}

// 对话框关闭
const handleDialogClose = () => {
  resetForm()
}

// 菜单名称输入时自动生成menu_code
const handleMenuNameInput = (value: string) => {
  // 如果用户还没有手动修改过menu_code，则自动生成
  if (!isEdit.value && !formData.value.menuCode) {
    formData.value.menuCode = generateMenuCode(value)
  }
}

// 生成菜单编码
const generateMenuCode = (name: string): string => {
  if (!name || name.trim() === '') {
    return ''
  }
  
  // 简单处理：移除空格和特殊字符，转为小写
  let code = name.replace(/[\s\u3000]+/g, '_')  // 替换空格为下划线
                .replace(/[^a-zA-Z0-9_\u4e00-\u9fa5]/g, '')  // 移除特殊字符
                .toLowerCase()
  
  // 如果是中文，建议用户手动输入英文
  if (/[\u4e00-\u9fa5]/.test(code)) {
    // 返回原样，让用户自己输入英文
    return code
  }
  
  // 确保以字母开头
  if (/^\d/.test(code)) {
    code = 'menu_' + code
  }
  
  return code
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped>
.menu-management { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

/* 对话框内容区域固定高度和滚动 */
.dialog-content {
  max-height: 450px;
  overflow-y: auto;
  padding-right: 10px;
}

/* 自定义滚动条样式 */
.dialog-content::-webkit-scrollbar {
  width: 6px;
}

.dialog-content::-webkit-scrollbar-thumb {
  background-color: #dcdfe6;
  border-radius: 3px;
}

.dialog-content::-webkit-scrollbar-thumb:hover {
  background-color: #c0c4cc;
}

/* 图标选择器样式 */
.icon-picker {
  max-height: 450px;
  overflow-y: auto;
}

.icon-groups {
  padding-bottom: 10px;
}

.icon-group {
  margin-bottom: 20px;
}

.group-title {
  font-size: 13px;
  font-weight: 600;
  color: #303133;
  padding: 8px 0;
  margin-bottom: 8px;
  border-bottom: 1px solid #e4e7ed;
}

.icon-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 6px;
}

.icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 8px 4px;
  border: 1px solid #dcdfe6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.3s;
  min-height: 60px;
}

.icon-item:hover {
  border-color: #409eff;
  background-color: #ecf5ff;
}

.icon-item.selected {
  border-color: #409eff;
  background-color: #ecf5ff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

.icon-label {
  font-size: 11px;
  color: #606266;
  margin-top: 3px;
  text-align: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}
</style>
