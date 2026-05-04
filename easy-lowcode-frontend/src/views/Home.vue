 <template>
  <div class="home">
    <el-row :gutter="20">
      <el-col :span="24">
        <el-card>
          <template #header>
            <div class="welcome-header">
              <h2>欢迎使用低代码平台</h2>
              <p>您好，{{ userStore.nickname || userStore.username }}！</p>
            </div>
          </template>
          <div class="stats">
            <el-row :gutter="20">
              <el-col :span="6">
                <el-statistic title="用户总数" :value="statistics.userCount">
                  <template #prefix>
                    <el-icon><User /></el-icon>
                  </template>
                </el-statistic>
              </el-col>
              <el-col :span="6">
                <el-statistic title="角色数量" :value="statistics.roleCount">
                  <template #prefix>
                    <el-icon><UserFilled /></el-icon>
                  </template>
                </el-statistic>
              </el-col>
              <el-col :span="6">
                <el-statistic title="菜单数量" :value="statistics.menuCount">
                  <template #prefix>
                    <el-icon><Menu /></el-icon>
                  </template>
                </el-statistic>
              </el-col>
              <el-col :span="6">
                <el-statistic title="部门数量" :value="statistics.deptCount">
                  <template #prefix>
                    <el-icon><OfficeBuilding /></el-icon>
                  </template>
                </el-statistic>
              </el-col>
            </el-row>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card>
          <template #header>
            <h3>快捷入口</h3>
          </template>
          <div class="quick-links">
            <el-button type="primary" @click="$router.push('/system/user')">
              <el-icon><User /></el-icon>
              用户管理
            </el-button>
            <el-button type="success" @click="$router.push('/system/role')">
              <el-icon><UserFilled /></el-icon>
              角色管理
            </el-button>
            <el-button type="warning" @click="$router.push('/system/menu')">
              <el-icon><Menu /></el-icon>
              菜单管理
            </el-button>
            <el-button type="info" @click="$router.push('/system/dept')">
              <el-icon><OfficeBuilding /></el-icon>
              部门管理
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores'
import { getStatistics } from '@/api/auth'
import { User, UserFilled, Menu, OfficeBuilding } from '@element-plus/icons-vue'

const userStore = useUserStore()

const statistics = ref({
  userCount: 0,
  roleCount: 0,
  menuCount: 0,
  deptCount: 0,
})

// 加载统计数据
const loadStatistics = async () => {
  try {
    const res = await getStatistics()
    // 响应拦截器已经解包，res 就是 ApiResponse.data
    statistics.value = res
  } catch (error) {
    console.error('加载统计数据失败:', error)
  }
}

onMounted(() => {
  loadStatistics()
})
</script>

<style scoped>
.home {
  padding: 20px;
}

.welcome-header h2 {
  margin: 0 0 10px 0;
  color: #303133;
}

.welcome-header p {
  margin: 0;
  color: #909399;
}

.stats {
  padding: 20px 0;
}

.quick-links {
  display: flex;
  gap: 15px;
  flex-wrap: wrap;
}

.quick-links .el-button {
  min-width: 120px;
}
</style>
