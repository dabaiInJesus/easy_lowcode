/**
 * User entity matching SysUser.java
 */
export interface User {
  id?: number
  username: string
  nickname: string
  realName?: string
  phone?: string
  email?: string
  avatar?: string
  gender?: number
  status: number
  deptId?: number
  createTime?: string
  updateTime?: string
}

/**
 * Role entity matching SysRole.java
 */
export interface Role {
  id?: number
  roleName: string
  roleCode: string
  description?: string
  status: number
  sort: number
  createTime?: string
  updateTime?: string
}

/**
 * Menu entity matching SysMenu.java
 */
export interface Menu {
  id?: number
  parentId?: number
  menuName: string
  menuCode: string
  menuType: number
  path?: string
  component?: string
  perms?: string
  icon?: string
  sort: number
  visible: number
  createTime?: string
  updateTime?: string
  children?: Menu[]
}

/**
 * Department entity matching SysDept.java
 */
export interface Dept {
  id?: number
  parentId?: number
  deptName: string
  deptCode: string
  sort: number
  leader?: string
  phone?: string
  email?: string
  status: number
  createTime?: string
  updateTime?: string
  children?: Dept[]
}

/**
 * Application entity matching SysApp.java
 */
export interface App {
  id?: number
  appName: string
  appCode: string
  appIcon?: string
  appUrl?: string
  clientId?: string
  clientSecret?: string
  redirectUri?: string
  status: number
  sort: number
  createTime?: string
  updateTime?: string
}
