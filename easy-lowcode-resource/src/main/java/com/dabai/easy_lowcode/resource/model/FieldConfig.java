package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

@Data
public class FieldConfig {
    private String columnName;
    private String dataType;
    private String columnComment;
    private boolean exactQuery;
    private boolean fuzzyQuery;
}
