-- 修复表资源数据源ID为NULL的问题
-- 如果有多个数据源，需要根据实际情况选择正确的数据源ID

-- 1. 先查看现有的数据源
SELECT id, name, code FROM collector_datasource_config WHERE deleted = 0;

-- 2. 查看表资源中 datasource_id 为 NULL 的记录
SELECT id, table_name, resource_code, datasource_id 
FROM collector_table_resource 
WHERE datasource_id IS NULL AND deleted = 0;

-- 3. 如果有数据源存在，更新表资源记录（假设使用第一个数据源）
-- 注意：请根据实际情况修改数据源ID
UPDATE collector_table_resource 
SET datasource_id = (SELECT id FROM collector_datasource_config WHERE deleted = 0 LIMIT 1)
WHERE datasource_id IS NULL AND deleted = 0;

-- 4. 验证更新结果
SELECT id, table_name, resource_code, datasource_id 
FROM collector_table_resource 
WHERE deleted = 0;
