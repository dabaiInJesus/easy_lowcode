package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("collector_unified_key_mapping")
public class UnifiedKeyMapping {
    private Long id;
    private String unifiedKey;
    private String displayName;
    private String description;
    private String resourceCode;
    private String fieldName;
    private String dataType;
    private String queryType;
    private Integer sortOrder;
    private Integer deleted;
}
