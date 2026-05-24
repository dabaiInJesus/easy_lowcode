package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

/**
 * 字段配置信息
 */
@Data
public class FieldConfig {
    private String columnName;
    private String dataType;
    private String columnComment;
    private String fieldLabel;
    private String fieldType;
    private boolean searchable;
    private boolean sortable;
    private boolean exactQuery;
    private boolean fuzzyQuery;
}
