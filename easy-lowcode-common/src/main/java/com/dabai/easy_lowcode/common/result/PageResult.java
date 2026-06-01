package com.dabai.easy_lowcode.common.result;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页结果封装
 */
@Data
public class PageResult<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 当前页码
     */
    private long current;

    /**
     * 每页大小
     */
    private long size;

    /**
     * 总记录数
     */
    private long total;

    /**
     * 总页数
     */
    private long pages;

    /**
     * 数据列表
     */
    private List<T> records;

    /**
     * 是否有上一页
     */
    private boolean hasPrevious;

    /**
     * 是否有下一页
     */
    private boolean hasNext;

    public PageResult() {
        this.records = new ArrayList<>();
    }

    public PageResult(List<T> records, long total, long current, long size) {
        this.records = records;
        this.total = total;
        this.current = current;
        this.size = size;
        this.pages = (total + size - 1) / size;
        this.hasPrevious = current > 1;
        this.hasNext = current < pages;
    }

    /**
     * 从MyBatis Plus Page转换
     */
    public static <T> PageResult<T> of(Page<T> page) {
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                page.getCurrent(),
                page.getSize()
        );
    }

    /**
     * 转换实体类型
     */
    public <R> PageResult<R> map(Function<T, R> mapper) {
        List<R> mappedRecords = this.records.stream()
                .map(mapper)
                .collect(Collectors.toList());
        return new PageResult<>(mappedRecords, this.total, this.current, this.size);
    }

    /**
     * 获取开始行号（从1开始）
     */
    public long getStartRow() {
        if (size == 0 || total == 0) {
            return 0;
        }
        return (current - 1) * size + 1;
    }

    /**
     * 获取结束行号
     */
    public long getEndRow() {
        return Math.min(current * size, total);
    }

    /**
     * 是否为空
     */
    public boolean isEmpty() {
        return records == null || records.isEmpty();
    }

    /**
     * 是否不为空
     */
    public boolean isNotEmpty() {
        return !isEmpty();
    }

    /**
     * 是否是第一页
     */
    public boolean isFirst() {
        return current == 1;
    }

    /**
     * 是否是最后一页
     */
    public boolean isLast() {
        return current >= pages;
    }

    /**
     * 获取上一页页码
     */
    public long getPrevPage() {
        return hasPrevious ? current - 1 : 1;
    }

    /**
     * 获取下一页页码
     */
    public long getNextPage() {
        return hasNext ? current + 1 : pages;
    }

    /**
     * 设置自定义页码范围（如显示的页码按钮）
     */
    public List<Long> getPageRange(int showCount) {
        List<Long> range = new ArrayList<>();
        if (pages <= showCount) {
            for (long i = 1; i <= pages; i++) {
                range.add(i);
            }
        } else {
            long half = showCount / 2;
            long start = Math.max(1, current - half);
            long end = Math.min(pages, start + showCount - 1);
            if (end - start + 1 < showCount) {
                start = Math.max(1, end - showCount + 1);
            }
            for (long i = start; i <= end; i++) {
                range.add(i);
            }
        }
        return range;
    }
}
