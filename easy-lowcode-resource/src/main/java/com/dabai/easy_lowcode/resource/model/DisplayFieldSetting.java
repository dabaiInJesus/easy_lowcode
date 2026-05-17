package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

import java.util.Map;

@Data
public class DisplayFieldSetting {
    private boolean visible = true;
    private String label;
    private Integer width;
    private String align;
    private String fixed;
    private String format;
    private String numberFormat;
    private Map<String, String> enumMapping;
    private boolean sortable = true;
}
