package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collector_fulltext_index_config")
public class FulltextIndexConfig extends BaseEntity {
    private String resourceCode;
    private String fieldName;
    private Integer indexed;
    private String searchEngine;
    private Float weight;
}
