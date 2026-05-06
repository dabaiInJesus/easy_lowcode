-- ============================================
-- 系统菜单初始化数据 - Easy Lowcode (修正版)
-- 数据库: PostgreSQL
-- menu_type: 1-目录/菜单, 2-按钮
-- ============================================

-- 1. 首页（顶级菜单）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (1, 0, '首页', 'home', 1, '/home', '/Home', 'House', 1, 1, NULL, NOW(), NOW(), 0);

-- 2. 系统管理（一级目录）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (2, 0, '系统管理', 'system', 1, '/system', 'Layout', 'Setting', 2, 1, NULL, NOW(), NOW(), 0);

-- 2.1 用户管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (21, 2, '用户管理', 'userManagement', 1, '/system/user', '/system/UserManagement', 'User', 1, 1, NULL, NOW(), NOW(), 0);

-- 2.2 角色管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (22, 2, '角色管理', 'roleManagement', 1, '/system/role', '/system/RoleManagement', 'UserFilled', 2, 1, NULL, NOW(), NOW(), 0);

-- 2.3 菜单管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (23, 2, '菜单管理', 'menuManagement', 1, '/system/menu', '/system/MenuManagement', 'Menu', 3, 1, NULL, NOW(), NOW(), 0);

-- 2.4 部门管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (24, 2, '部门管理', 'deptManagement', 1, '/system/dept', '/system/DeptManagement', 'OfficeBuilding', 4, 1, NULL, NOW(), NOW(), 0);

-- 2.5 授权管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (25, 2, '授权管理', 'authManagement', 1, '/system/auth', '/system/AuthManagement', 'Key', 5, 1, NULL, NOW(), NOW(), 0);

-- 2.6 应用管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (26, 2, '应用管理', 'appManagement', 1, '/system/app', '/system/AppManagement', 'Monitor', 6, 1, NULL, NOW(), NOW(), 0);

-- 3. 资源配置（一级目录）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (3, 0, '资源配置', 'resource', 1, '/resource', 'Layout', 'Collection', 3, 1, NULL, NOW(), NOW(), 0);

-- 3.1 数据源管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (31, 3, '数据源管理', 'datasourceManagement', 1, '/resource/datasource', '/resource/DataSourceManagement', 'Connection', 1, 1, NULL, NOW(), NOW(), 0);

-- 3.2 表资源注册
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (32, 3, '表资源注册', 'tableResourceManagement', 1, '/resource/table', '/resource/TableResourceManagement', 'Document', 2, 1, NULL, NOW(), NOW(), 0);

-- 3.3 API管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (33, 3, 'API管理', 'apiManagement', 1, '/resource/api', '/resource/ApiManagement', 'Document', 3, 1, NULL, NOW(), NOW(), 0);

-- 4. ETL管理（一级目录）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (4, 0, 'ETL管理', 'etl', 1, '/etl', 'Layout', 'Operation', 4, 1, NULL, NOW(), NOW(), 0);

-- 4.1 ETL任务
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (41, 4, 'ETL任务', 'etlTaskManagement', 1, '/etl/task', '/etl/EtlTaskManagement', 'List', 1, 1, NULL, NOW(), NOW(), 0);

-- 5. 数据大屏（一级目录）
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (5, 0, '数据大屏', 'dashboard', 1, '/dashboard', 'Layout', 'Monitor', 5, 1, NULL, NOW(), NOW(), 0);

-- 5.1 大屏管理
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, icon, sort, visible, perms, create_time, update_time, deleted)
VALUES (51, 5, '大屏管理', 'dashboardManagement', 1, '/dashboard/manage', '/dashboard/DashboardManagement', 'Grid', 1, 1, NULL, NOW(), NOW(), 0);

-- 为 admin 角色分配所有菜单权限（假设 admin 角色 ID 为 1）
INSERT INTO sys_role_menu (role_id, menu_id)
SELECT 1, id FROM sys_menu WHERE deleted = 0;

-- 查询验证
SELECT COUNT(*) as menu_count FROM sys_menu WHERE deleted = 0;
SELECT id, parent_id, menu_name, menu_code, path FROM sys_menu WHERE deleted = 0 ORDER BY sort, id;
