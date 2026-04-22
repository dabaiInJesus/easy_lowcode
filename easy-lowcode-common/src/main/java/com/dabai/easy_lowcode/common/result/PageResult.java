package com.dabai.easy_lowcode.common.result;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 总记录数
     */
    private Long total;
    
    /**
     * 当前页码
     */
    private Long current;
    
    /**
     * 每页大小
     */
    private Long size;
    
    /**
     * 总页数
     */
    private Long pages;
    
    /**
     * 数据列表
     */
    private List<T> records;
    
    public PageResult() {
    }
    
    public PageResult(Long total, Long current, Long size, List<T> records) {
        this.total = total;
        this.current = current;
        this.size = size;
        this.records = records;
        this.pages = (total + size - 1) / size;
    }
}
