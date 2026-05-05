import request from '@/utils/request'
import type { ApiResponse } from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResponse {
  token: string
}

export interface UserInfo {
  id: number
  username: string
  nickname: string
  realName: string
  phone: string
  email: string
  avatar: string
  gender: number
  status: number
  deptId: number
}

/**
 * 用户登录
 */
export function login(data: LoginParams) {
  return request({
    url: '/auth/login',
    method: 'post',
    data,
  })
}

/**
 * 用户登出
 */
export function logout() {
  return request({
    url: '/auth/logout',
    method: 'post',
  })
}

/**
 * 获取当前用户信息
 */
export function getCurrentUser() {
  return request({
    url: '/auth/current',
    method: 'get',
  })
}

/**
 * 分页查询用户列表
 */
export function getUserPage(current: number, size: number, keyword?: string) {
  return request({
    url: '/auth/user/page',
    method: 'get',
    params: { current, size, keyword },
  })
}

/**
 * 创建用户
 */
export function createUser(data: any) {
  return request({
    url: '/auth/user',
    method: 'post',
    data,
  })
}

/**
 * 更新用户
 */
export function updateUser(data: any) {
  return request({
    url: '/auth/user',
    method: 'put',
    data,
  })
}

/**
 * 删除用户
 */
export function deleteUser(id: number) {
  return request({
    url: `/auth/user/${id}`,
    method: 'delete',
  })
}

/**
 * 重置密码
 */
export function resetPassword(id: number, newPassword: string) {
  return request({
    url: `/auth/user/${id}/reset-password`,
    method: 'post',
    data: { newPassword },
  })
}

/**
 * 获取统计数据
 */
export function getStatistics() {
  return request({
    url: '/auth/statistics',
    method: 'get',
  })
}

/**
 * 获取菜单列表
 */
export function getMenuList() {
  return request({
    url: '/auth/menu/list',
    method: 'get',
  })
}

/**
 * 创建菜单
 */
export function createMenu(data: any) {
  return request({
    url: '/auth/menu',
    method: 'post',
    data,
  })
}

/**
 * 更新菜单
 */
export function updateMenu(data: any) {
  return request({
    url: '/auth/menu',
    method: 'put',
    data,
  })
}

/**
 * 删除菜单
 */
export function deleteMenu(id: number) {
  return request({
    url: `/auth/menu/${id}`,
    method: 'delete',
  })
}

/**
 * 获取角色列表
 */
export function getRoleList() {
  return request({
    url: '/auth/role/list',
    method: 'get',
    silentError: true, // 静默错误，由调用方统一处理
  })
}

/**
 * 创建角色
 */
export function createRole(data: any) {
  return request({
    url: '/auth/role',
    method: 'post',
    data,
    silentError: true, // 静默错误，由调用方统一处理
  })
}

/**
 * 更新角色
 */
export function updateRole(data: any) {
  return request({
    url: '/auth/role',
    method: 'put',
    data,
    silentError: true, // 静默错误，由调用方统一处理
  })
}

/**
 * 删除角色
 */
export function deleteRole(id: number) {
  return request({
    url: `/auth/role/${id}`,
    method: 'delete',
    silentError: true, // 静默错误，由调用方统一处理
  })
}

/**
 * 获取部门列表
 */
export function getDeptList(): Promise<ApiResponse<any>> {
  return request({
    url: '/auth/dept/list',
    method: 'get',
  })
}

/**
 * 创建部门
 */
export function createDept(data: any) {
  return request({
    url: '/auth/dept',
    method: 'post',
    data,
  })
}

/**
 * 更新部门
 */
export function updateDept(data: any) {
  return request({
    url: '/auth/dept',
    method: 'put',
    data,
  })
}

/**
 * 删除部门
 */
export function deleteDept(id: number) {
  return request({
    url: `/auth/dept/${id}`,
    method: 'delete',
  })
}

/**
 * 获取应用列表
 */
export function getAppList() {
  return request({
    url: '/auth/app/list',
    method: 'get',
  })
}

/**
 * 分页查询应用列表
 */
export function getAppPage(current: number, size: number, keyword?: string) {
  return request({
    url: '/auth/app/page',
    method: 'get',
    params: { current, size, keyword },
  })
}

/**
 * 创建应用
 */
export function createApp(data: any) {
  return request({
    url: '/auth/app',
    method: 'post',
    data,
  })
}

/**
 * 更新应用
 */
export function updateApp(data: any) {
  return request({
    url: '/auth/app',
    method: 'put',
    data,
  })
}

/**
 * 删除应用
 */
export function deleteApp(id: number) {
  return request({
    url: `/auth/app/${id}`,
    method: 'delete',
  })
}

/**
 * 获取用户列表（包含角色信息）
 */
export function getUsersWithRoles() {
  return request({
    url: '/auth/authorization/users',
    method: 'get',
    silentError: true, // 静默错误，不显示提示
  })
}

/**
 * 获取角色列表（包含菜单数量）
 */
export function getRolesWithMenuCount() {
  return request({
    url: '/auth/authorization/roles',
    method: 'get',
    silentError: true, // 静默错误，不显示提示
  })
}

/**
 * 获取所有角色
 */
export function getAllRoles() {
  return request({
    url: '/auth/authorization/all-roles',
    method: 'get',
    silentError: true, // 静默错误，不显示提示
  })
}

/**
 * 获取用户的角色
 */
export function getUserRoles(userId: number) {
  return request({
    url: `/auth/authorization/user/${userId}/roles`,
    method: 'get',
  })
}

/**
 * 为用户分配角色
 */
export function assignRolesToUser(userId: number, roleIds: number[]) {
  return request({
    url: `/auth/authorization/user/${userId}/roles`,
    method: 'post',
    data: { roleIds },
  })
}

/**
 * 获取角色的菜单
 */
export function getRoleMenus(roleId: number) {
  return request({
    url: `/auth/authorization/role/${roleId}/menus`,
    method: 'get',
  })
}

/**
 * 获取菜单树
 */
export function getMenuTree() {
  return request({
    url: '/auth/authorization/menus/tree',
    method: 'get',
  })
}

/**
 * 为角色分配菜单
 */
export function assignMenusToRole(roleId: number, menuIds: number[]) {
  return request({
    url: `/auth/authorization/role/${roleId}/menus`,
    method: 'post',
    data: { menuIds },
  })
}
