package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.dabai.easy_lowcode.database.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("collector_fulltext_document")
public class FulltextDocument extends BaseEntity {
    private String fileName;
    private String fileType;
    private Long fileSize;
    private String storageType;
    private String storagePath;
    private String contentText;
    private String resourceCode;
    private String searchEngine;
    private Integer indexed;
    private String indexError;
}
