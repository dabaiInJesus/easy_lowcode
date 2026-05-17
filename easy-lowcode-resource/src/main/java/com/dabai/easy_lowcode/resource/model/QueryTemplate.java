package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class QueryTemplate {
    private String name;
    private String label;
    private String description;
    private String sql;
    private boolean enabled = true;
    private boolean isDefault;
    private List<TemplateParam> parameters = new ArrayList<>();

    @Data
    public static class TemplateParam {
        private String name;
        private String type = "string";
        private boolean required;
        private String defaultValue;
        private String label;
    }
}
