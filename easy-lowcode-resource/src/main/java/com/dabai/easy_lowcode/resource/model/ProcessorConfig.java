package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

import java.util.Map;

@Data
public class ProcessorConfig {
    private String type;
    private boolean enabled = true;
    private int order;
    private Map<String, Object> config;
}
