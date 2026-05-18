package com.dabai.easy_lowcode.etl.model;

import lombok.Data;

@Data
public class TransformRule {
    private String sourceField;
    private String targetField;
    private String transformType;
    private String expression;
    private String defaultValue;
}
