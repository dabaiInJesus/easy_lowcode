-- 检查表资源的数据源ID
SELECT 
    tr.id,
    tr.table_name,
    tr.resource_code,
    tr.datasource_id,
    tr.api_path,
    ds.name as datasource_name,
    ds.code as datasource_code
FROM collector_table_resource tr
LEFT JOIN collector_datasource ds ON tr.datasource_id = ds.id AND ds.deleted = 0
WHERE tr.deleted = 0
ORDER BY tr.create_time DESC;

-- 如果 datasource_id 为 NULL，需要更新
-- 找到可用的数据源
SELECT id, name, code FROM collector_datasource WHERE deleted = 0;

-- 更新 datasource_id 为 NULL 的记录（假设使用第一个数据源，请根据实际情况修改ID）
-- UPDATE collector_table_resource 
-- SET datasource_id = 1  -- 替换为实际的数据源ID
-- WHERE datasource_id IS NULL AND deleted = 0;
