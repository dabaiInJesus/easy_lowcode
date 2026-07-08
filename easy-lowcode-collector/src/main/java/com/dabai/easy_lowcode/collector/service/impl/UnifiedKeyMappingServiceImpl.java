package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import com.dabai.easy_lowcode.collector.entity.UnifiedKeyMapping;
import com.dabai.easy_lowcode.collector.mapper.TableResourceMapper;
import com.dabai.easy_lowcode.collector.mapper.UnifiedKeyMappingMapper;
import com.dabai.easy_lowcode.collector.service.UnifiedKeyMappingService;
import com.dabai.easy_lowcode.database.model.DataSourceInfo;
import com.dabai.easy_lowcode.database.service.DataSourceProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UnifiedKeyMappingServiceImpl extends ServiceImpl<UnifiedKeyMappingMapper, UnifiedKeyMapping> implements UnifiedKeyMappingService {

    private final DataSourceProvider dataSourceProvider;
    private final TableResourceMapper tableResourceMapper;

    private static final Map<String, List<String>> SYNONYM_MAP = Map.of(
            "email", List.of("email", "mail", "邮箱", "邮件"),
            "phone", List.of("phone", "mobile", "tel", "telephone", "手机", "电话", "移动电话"),
            "name", List.of("name", "username", "real_name", "realname", "nickname", "姓名", "名称", "用户名", "昵称"),
            "id", List.of("id", "user_id", "编号", "ID"),
            "status", List.of("status", "state", "状态"),
            "date", List.of("date", "time", "create_time", "update_time", "日期", "时间", "创建时间"),
            "address", List.of("address", "addr", "地址", "住址"),
            "gender", List.of("gender", "sex", "性别")
    );

    @Override
    public List<UnifiedKeyMapping> getDistinctKeys() {
        return baseMapper.selectDistinctKeys();
    }

    @Override
    public List<UnifiedKeyMapping> getMappingsByKey(String unifiedKey) {
        return baseMapper.selectByUnifiedKey(unifiedKey);
    }

    @Override
    public List<UnifiedKeyMapping> getMappingsByResourceCode(String resourceCode) {
        return baseMapper.selectByResourceCode(resourceCode);
    }

    @Override
    public List<Map<String, Object>> suggestMappings(String unifiedKey, String displayName) {
        List<String> synonyms = SYNONYM_MAP.getOrDefault(unifiedKey.toLowerCase(),
                List.of(unifiedKey.toLowerCase()));
        List<Map<String, Object>> suggestions = new ArrayList<>();

        LambdaQueryWrapper<TableResource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(TableResource::getStatus, 1);
        List<TableResource> resources = tableResourceMapper.selectList(wrapper);

        for (TableResource resource : resources) {
            DataSourceInfo ds = dataSourceProvider.getById(resource.getDatasourceId());
            if (ds == null) continue;

            try (Connection conn = dataSourceProvider.getConnection(ds)) {
                DatabaseMetaData meta = conn.getMetaData();
                try (ResultSet columns = meta.getColumns(null, null, resource.getTableName(), null)) {
                    while (columns.next()) {
                        String colName = columns.getString("COLUMN_NAME").toLowerCase();
                        String colComment = columns.getString("REMARKS");
                        String dbDataType = columns.getString("TYPE_NAME");

                        for (String syn : synonyms) {
                            if (colName.contains(syn) || (colComment != null && colComment.toLowerCase().contains(syn))) {
                                Map<String, Object> suggestion = new LinkedHashMap<>();
                                suggestion.put("resourceCode", resource.getResourceCode());
                                suggestion.put("resourceName", resource.getTableComment() != null ? resource.getTableComment() : resource.getTableName());
                                suggestion.put("fieldName", colName);
                                suggestion.put("columnComment", colComment);
                                suggestion.put("dataType", mapDataType(dbDataType));
                                suggestion.put("suggested", true);
                                suggestions.add(suggestion);
                                break;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                log.warn("扫描资源 {} 表结构失败: {}", resource.getResourceCode(), e.getMessage());
            }
        }

        return suggestions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchSave(List<UnifiedKeyMapping> mappings) {
        for (UnifiedKeyMapping mapping : mappings) {
            LambdaQueryWrapper<UnifiedKeyMapping> check = new LambdaQueryWrapper<>();
            check.eq(UnifiedKeyMapping::getUnifiedKey, mapping.getUnifiedKey())
                    .eq(UnifiedKeyMapping::getResourceCode, mapping.getResourceCode())
                    .eq(UnifiedKeyMapping::getFieldName, mapping.getFieldName());
            if (baseMapper.selectCount(check) == 0) {
                save(mapping);
            }
        }
    }

    private String mapDataType(String dbDataType) {
        if (dbDataType == null) return "string";
        String t = dbDataType.toUpperCase();
        if (t.contains("INT") || t.contains("FLOAT") || t.contains("DOUBLE") || t.contains("DECIMAL") || t.contains("NUMERIC")) {
            return "number";
        }
        if (t.contains("DATE") || t.contains("TIME") || t.contains("TIMESTAMP")) {
            return "date";
        }
        return "string";
    }
}
