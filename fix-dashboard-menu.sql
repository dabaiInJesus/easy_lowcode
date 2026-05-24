-- =============================================
-- 修复大屏设计页面侧边栏问题的 SQL
-- =============================================

-- 1. 查看当前的 dashboard 菜单
SELECT id, parent_id, menu_name, menu_code, path, component 
FROM sys_menu 
WHERE path LIKE '%dashboard%' OR menu_name LIKE '%大屏%';

-- 2. 删除无效的 /dashboard/design 菜单项
-- 注意：大屏设计器需要大屏ID参数，应该通过"设计"按钮导航，而不是菜单
DELETE FROM sys_menu WHERE path = '/dashboard/design';

-- 3. 同时删除 /dashboard/view 菜单项（预览页面也需要ID）
DELETE FROM sys_menu WHERE path = '/dashboard/view';

-- 4. 验证删除结果
SELECT id, parent_id, menu_name, menu_code, path, component 
FROM sys_menu 
WHERE path LIKE '%dashboard%' OR menu_name LIKE '%大屏%';

-- 5. 查看修改后的角色菜单关联（确保没有孤立记录）
SELECT * FROM sys_role_menu 
WHERE menu_id NOT IN (SELECT id FROM sys_menu);

-- 6. 清理孤立角色菜单关联（如果有的话）
DELETE FROM sys_role_menu 
WHERE menu_id NOT IN (SELECT id FROM sys_menu);
