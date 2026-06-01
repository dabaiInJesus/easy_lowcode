-- Liquibase checksum 修复脚本
-- 用于修复因文件变更导致的 checksum 不匹配问题

-- 清除所有已记录的 checksum，让 Liquibase 重新验证
-- 注意：这意味着 Liquibase 会重新执行所有变更，需要谨慎使用

-- 如果只是跳过某些特定 changeset 的验证，可以使用以下方法：
-- 1. 直接从 databasechangelog 表中删除对应记录
-- 2. 让 Liquibase 重新执行这些变更

-- 或者使用 Liquibase 的 clearCheckSums 命令（在命令行中执行）：
-- liquibase clearCheckSums

-- 如果你确定文件内容是正确的，只是需要在数据库中更新 checksum，
-- 可以执行以下 SQL（需要根据实际 checksum 值修改）：

-- 查看当前 checksum 值：
-- SELECT id, author, filename, md5sum FROM databasechangelog 
-- WHERE filename LIKE '%006-create-collector-tables.xml%' 
--    OR filename LIKE '%007-create-sys-resource-table.xml%';

-- 以下是常见的处理方法：

-- 方法1: 删除旧记录，让 Liquibase 重新执行
-- DELETE FROM databasechangelog 
-- WHERE filename LIKE '%006-create-collector-tables.xml%' 
--    OR filename LIKE '%007-create-sys-resource-table.xml%';

-- 方法2: 如果只想跳过验证，可以添加 runOnChange=true 的标签
-- 但这通常需要创建一个新的 changeset

-- 建议：在开发环境中，使用方法1更直接