package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collector_unified_key_mapping")
public class UnifiedKeyMapping extends BaseEntity {
    private String unifiedKey;
    private String displayName;
    private String description;
    private String resourceCode;
    private String fieldName;
    private String dataType;
    private String queryType;
    private Integer sortOrder;
}
