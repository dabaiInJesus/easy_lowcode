package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collector_fulltext_index_config")
public class FulltextIndexConfig {
    private Long id;
    private String resourceCode;
    private String fieldName;
    private Integer indexed;
    private String searchEngine;
    private Float weight;
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
