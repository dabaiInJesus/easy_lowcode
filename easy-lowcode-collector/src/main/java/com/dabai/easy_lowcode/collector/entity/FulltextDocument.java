package com.dabai.easy_lowcode.collector.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collector_fulltext_document")
public class FulltextDocument {
    private Long id;
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
    private Integer deleted;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private Long createBy;
    private Long updateBy;
}
