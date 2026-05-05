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
          <el-table :data="userRoleData" border stripe v-loading="userLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="username" label="用户名" width="150" />
            <el-table-column prop="nickname" label="昵称" width="150" />
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
          <el-table :data="roleMenuData" border stripe v-loading="roleLoading">
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="roleName" label="角色名称" width="150" />
            <el-table-column prop="roleCode" label="角色编码" width="150" />
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

    <!-- 用户角色授权对话框 -->
    <el-dialog
      v-model="userAuthVisible"
      title="用户角色授权"
      width="600px"
    >
      <div v-if="currentUser">
        <p><strong>用户：</strong>{{ currentUser.username }} ({{ currentUser.nickname }})</p>
        <el-divider />
        <el-checkbox-group v-model="selectedRoles">
          <el-checkbox 
            v-for="role in allRoles" 
            :key="role.id" 
            :value="role.id"
            style="display: block; margin: 10px 0;"
          >
            {{ role.roleName }} ({{ role.roleCode }})
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <template #footer>
        <el-button @click="userAuthVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitUserAuth" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 角色菜单授权对话框 -->
    <el-dialog
      v-model="roleAuthVisible"
      title="角色菜单授权"
      width="700px"
    >
      <div v-if="currentRole">
        <p><strong>角色：</strong>{{ currentRole.roleName }} ({{ currentRole.roleCode }})</p>
        <el-divider />
        <el-tree
          ref="menuTreeRef"
          :data="menuTree"
          show-checkbox
          node-key="id"
          :props="treeProps"
          default-expand-all
          :check-strictly="true"
        />
      </div>
      <template #footer>
        <el-button @click="roleAuthVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmitRoleAuth" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { TreeInstance } from 'element-plus'
import {
  getUsersWithRoles,
  getRolesWithMenuCount,
  getAllRoles,
  getUserRoles,
  assignRolesToUser,
  getRoleMenus,
  getMenuTree,
  assignMenusToRole,
} from '@/api/auth'

const activeTab = ref('user-role')

// 用户角色授权相关
const userLoading = ref(false)
const userRoleData = ref<any[]>([])
const userAuthVisible = ref(false)
const currentUser = ref<any>(null)
const allRoles = ref<any[]>([])
const selectedRoles = ref<number[]>([])

// 角色菜单授权相关
const roleLoading = ref(false)
const roleMenuData = ref<any[]>([])
const roleAuthVisible = ref(false)
const currentRole = ref<any>(null)
const menuTree = ref<any[]>([])
const menuTreeRef = ref<TreeInstance>()
const treeProps = {
  children: 'children',
  label: 'menuName',
}

const submitLoading = ref(false)

// 加载用户列表
const loadUsers = async () => {
  userLoading.value = true
  try {
    const res = await getUsersWithRoles()
    if (res) {
      userRoleData.value = res
    }
  } catch (error) {
    console.error('加载用户列表失败:', error)
  } finally {
    userLoading.value = false
  }
}

// 加载角色列表
const loadRoles = async () => {
  roleLoading.value = true
  try {
    const res = await getRolesWithMenuCount()
    if (res) {
      roleMenuData.value = res
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
  } finally {
    roleLoading.value = false
  }
}

// 加载所有角色（用于下拉选择）
const loadAllRoles = async () => {
  try {
    const res = await getAllRoles()
    if (res) {
      allRoles.value = res
    }
  } catch (error) {
    console.error('加载角色列表失败:', error)
  }
}

// 加载菜单树
const loadMenuTree = async () => {
  try {
    console.log('开始加载菜单树...')
    const res = await getMenuTree()
    console.log('菜单树响应数据:', res)
    if (res) {
      menuTree.value = res
      console.log('菜单树长度:', menuTree.value.length)
      if (menuTree.value.length === 0) {
        console.warn('菜单树为空，请检查数据库中是否有菜单数据')
      }
    } else {
      console.warn('菜单树响应为空')
      ElMessage.warning('菜单树数据为空')
    }
  } catch (error: any) {
    console.error('加载菜单树失败:', error)
    console.error('错误详情:', error.message, error.response)
    ElMessage.error('加载菜单树失败: ' + (error.message || '未知错误'))
  }
}

// 用户授权
const handleAuthUser = async (row: any) => {
  currentUser.value = row
  userAuthVisible.value = true
  
  // 加载该用户的角色
  try {
    const roles = await getUserRoles(row.id)
    selectedRoles.value = roles.map((r: any) => r.id)
  } catch (error) {
    console.error('加载用户角色失败:', error)
  }
}

// 提交用户授权
const handleSubmitUserAuth = async () => {
  if (!currentUser.value) return
  
  submitLoading.value = true
  try {
    await assignRolesToUser(currentUser.value.id, selectedRoles.value)
    ElMessage.success('授权成功')
    userAuthVisible.value = false
    loadUsers() // 刷新用户列表
  } catch (error) {
    console.error('授权失败:', error)
    ElMessage.error('授权失败')
  } finally {
    submitLoading.value = false
  }
}

// 角色授权
const handleAuthRole = async (row: any) => {
  currentRole.value = row
  roleAuthVisible.value = true
  
  // 先清空树形组件的选中状态
  setTimeout(() => {
    if (menuTreeRef.value) {
      menuTreeRef.value.setCheckedKeys([])
    }
  }, 50)
  
  // 加载该角色的菜单
  try {
    const menuIds = await getRoleMenus(row.id)
    console.log('角色已分配的菜单IDs:', menuIds)
    // 等待DOM更新后设置选中的节点
    setTimeout(() => {
      if (menuTreeRef.value && menuIds && menuIds.length > 0) {
        // 将数字ID转换为字符串ID
        const stringMenuIds = menuIds.map((id: number) => String(id))
        console.log('转换后的字符串IDs:', stringMenuIds)
        menuTreeRef.value.setCheckedKeys(stringMenuIds)
      }
    }, 100)
  } catch (error) {
    console.error('加载角色菜单失败:', error)
  }
}

// 提交角色授权
const handleSubmitRoleAuth = async () => {
  if (!currentRole.value || !menuTreeRef.value) return
  
  submitLoading.value = true
  try {
    // 获取选中的菜单ID（包括半选中状态）
    const checkedKeys = menuTreeRef.value.getCheckedKeys(false) as string[]
    const halfCheckedKeys = menuTreeRef.value.getHalfCheckedKeys() as string[]
    const allCheckedKeys = [...checkedKeys, ...halfCheckedKeys]
    
    console.log('选中的菜单IDs:', allCheckedKeys)
    
    await assignMenusToRole(currentRole.value.id, allCheckedKeys)
    ElMessage.success('授权成功')
    roleAuthVisible.value = false
    loadRoles() // 刷新角色列表
  } catch (error) {
    console.error('授权失败:', error)
    ElMessage.error('授权失败')
  } finally {
    submitLoading.value = false
  }
}

onMounted(() => {
  loadUsers()
  loadRoles()
  loadAllRoles()
  loadMenuTree()
})
</script>

<style scoped>
.auth-management {
  height: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
