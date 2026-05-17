package com.dabai.easy_lowcode.resource.processor;

import com.dabai.easy_lowcode.collector.entity.DataSourceConfig;
import com.dabai.easy_lowcode.collector.entity.TableResource;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@Data
@Builder
public class ProcessorContext {
    private TableResource tableResource;
    private DataSourceConfig dataSourceConfig;
    private Set<String> allowedColumns;
    private String dbType;
    private Long userId;

    @Builder.Default
    private Map<String, Object> extendedProps = new HashMap<>();
}
