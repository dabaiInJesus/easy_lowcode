package com.dabai.easy_lowcode.resource.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ConfigJson {
    private List<FieldConfig> fields = new ArrayList<>();
    private List<ProcessorConfig> parameterProcessors = new ArrayList<>();
    private List<ProcessorConfig> resultProcessors = new ArrayList<>();
    private List<QueryTemplate> queryTemplates = new ArrayList<>();
    private DisplaySettings displaySettings = new DisplaySettings();

    public java.util.Set<String> getAllowedColumnNames() {
        java.util.Set<String> cols = new java.util.HashSet<>();
        for (FieldConfig f : fields) {
            if (f.getColumnName() != null) {
                cols.add(f.getColumnName().toLowerCase());
            }
        }
        return cols;
    }

    public boolean hasTemplate(String name) {
        if (name == null || queryTemplates == null) return false;
        return queryTemplates.stream().anyMatch(t -> name.equals(t.getName()) && t.isEnabled());
    }

    public QueryTemplate getTemplate(String name) {
        if (name == null || queryTemplates == null) return null;
        return queryTemplates.stream()
                .filter(t -> name.equals(t.getName()) && t.isEnabled())
                .findFirst().orElse(null);
    }

    public QueryTemplate getDefaultTemplate() {
        if (queryTemplates == null) return null;
        return queryTemplates.stream()
                .filter(QueryTemplate::isDefault)
                .findFirst().orElse(null);
    }
}
