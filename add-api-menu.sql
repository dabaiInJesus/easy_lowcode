-- 检查是否存在resource父菜单
SELECT id, menu_name, menu_code FROM sys_menu WHERE menu_code = 'resource';

-- 如果不存在，先创建资源管理目录菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, perms, icon, sort, visible, deleted, create_time, update_time, create_by, update_by)
SELECT 
    COALESCE((SELECT MAX(id) FROM sys_menu), 0) + 1,
    0,
    '资源管理',
    'resource',
    1,
    '/resource',
    'Layout',
    NULL,
    'Folder',
    2,
    1,
    0,
    NOW(),
    NOW(),
    1,
    1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'resource');

-- 插入API管理菜单
INSERT INTO sys_menu (id, parent_id, menu_name, menu_code, menu_type, path, component, perms, icon, sort, visible, deleted, create_time, update_time, create_by, update_by)
SELECT 
    COALESCE((SELECT MAX(id) FROM sys_menu), 0) + 1,
    (SELECT id FROM sys_menu WHERE menu_code = 'resource' LIMIT 1),
    'API管理',
    'api_management',
    2,
    '/resource/api',
    'resource/ApiManagement',
    'resource:api:list',
    'Document',
    3,
    1,
    0,
    NOW(),
    NOW(),
    1,
    1
WHERE NOT EXISTS (SELECT 1 FROM sys_menu WHERE menu_code = 'api_management');

-- 验证菜单是否创建成功
SELECT id, parent_id, menu_name, menu_code, menu_type, path FROM sys_menu WHERE menu_code IN ('resource', 'api_management') ORDER BY id;
