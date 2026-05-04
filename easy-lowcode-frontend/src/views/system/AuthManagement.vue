<template>
  <div class="auth-management">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>授权管理</span>
        </div>
      </template>

      <el-tabs v-model="activeTab">
        <el-tab-pane label="用户角色授权" name="user-role">
          <el-table :data="userRoleData" border stripe>
            <el-table-column prop="username" label="用户名" />
            <el-table-column prop="nickname" label="昵称" />
            <el-table-column label="已分配角色">
              <template #default="{ row }">
                <el-tag v-for="role in row.roles" :key="role" size="small" style="margin-right: 5px">
                  {{ role }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleAuthUser(row)">
                  授权
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>

        <el-tab-pane label="角色菜单授权" name="role-menu">
          <el-table :data="roleMenuData" border stripe>
            <el-table-column prop="roleName" label="角色名称" />
            <el-table-column prop="roleCode" label="角色编码" />
            <el-table-column label="已分配菜单数">
              <template #default="{ row }">
                {{ row.menuCount }} 个菜单
              </template>
            </el-table-column>
            <el-table-column label="操作" width="150">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="handleAuthRole(row)">
                  授权
                </el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'

const activeTab = ref('user-role')

const userRoleData = ref([
  { id: 1, username: 'admin', nickname: '管理员', roles: ['超级管理员'] },
  { id: 2, username: 'user1', nickname: '用户1', roles: ['普通用户'] },
])

const roleMenuData = ref([
  { id: 1, roleName: '超级管理员', roleCode: 'admin', menuCount: 10 },
  { id: 2, roleName: '普通用户', roleCode: 'user', menuCount: 3 },
])

const handleAuthUser = (row: any) => {
  console.log('用户角色授权', row)
}

const handleAuthRole = (row: any) => {
  console.log('角色菜单授权', row)
}
</script>

<style scoped>
.auth-management { height: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
