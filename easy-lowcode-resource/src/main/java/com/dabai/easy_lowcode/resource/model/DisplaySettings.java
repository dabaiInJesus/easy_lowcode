package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class DisplaySettings {
    private int pageSize = 20;
    private boolean stripe = true;
    private boolean border;
    private Map<String, DisplayFieldSetting> fields = new LinkedHashMap<>();
}
