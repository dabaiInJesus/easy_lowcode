package com.dabai.easy_lowcode.resource.service;

import com.dabai.easy_lowcode.resource.model.FieldConfig;

import java.util.List;
import java.util.Map;

/**
 * 资源检索服务接口
 * 支持单资源检索、多资源统一检索、全文检索
 */
public interface ResourceSearchService {
    
    // ==================== 单资源检索 ====================
    
    /**
     * 单资源检索 - 支持分页、排序、多条件过滤
     * 
     * @param resourceCode 资源编码
     * @param params 查询参数
     * @return 分页结果
     */
    SearchResult singleSearch(String resourceCode, SearchParams params);
    
    /**
     * 单资源检索 - 根据ID获取单条记录
     * 
     * @param resourceCode 资源编码
     * @param id 主键ID
     * @return 单条记录
     */
    Map<String, Object> singleGetById(String resourceCode, Long id);
    
    // ==================== 多资源统一检索 ====================
    
    /**
     * 多资源统一检索 - 同时查询多个资源并合并结果
     * 
     * @param resourceCodes 资源编码列表
     * @param params 查询参数（统一过滤条件）
     * @return 合并后的分页结果
     */
    SearchResult multiSearch(List<String> resourceCodes, SearchParams params);
    
    /**
     * 多资源关联检索 - 支持跨资源关联查询
     * 
     * @param joinConfig 关联配置
     * @param params 查询参数
     * @return 关联查询结果
     */
    SearchResult joinSearch(JoinConfig joinConfig, SearchParams params);
    
    // ==================== 全文检索 ====================
    
    /**
     * 全文检索 - 在指定资源的所有文本字段中搜索关键词
     * 
     * @param resourceCode 资源编码
     * @param keyword 搜索关键词
     * @param params 分页参数
     * @return 搜索结果
     */
    SearchResult fullTextSearch(String resourceCode, String keyword, SearchParams params);
    
    /**
     * 多资源全文检索 - 同时在多个资源中搜索
     * 
     * @param resourceCodes 资源编码列表
     * @param keyword 搜索关键词
     * @param params 分页参数
     * @return 搜索结果
     */
    SearchResult multiFullTextSearch(List<String> resourceCodes, String keyword, SearchParams params);
    
    // ==================== 辅助方法 ====================
    
    /**
     * 获取资源可用字段列表（用于前端构建查询条件）
     * 
     * @param resourceCode 资源编码
     * @return 字段配置列表
     */
    List<FieldConfig> getResourceFields(String resourceCode);
    
    // ==================== 内部类 ====================
    
    /**
     * 搜索参数
     */
    class SearchParams {
        private Integer page = 1;
        private Integer pageSize = 20;
        private String orderField;
        private String orderDirection = "ASC";
        private Map<String, Object> filters; // field -> value
        private List<String> selectFields; // 指定返回字段
        private String keyword; // 全文检索关键词
        
        public Integer getPage() { return page; }
        public void setPage(Integer page) { this.page = page; }
        public Integer getPageSize() { return pageSize; }
        public void setPageSize(Integer pageSize) { this.pageSize = Math.min(pageSize, 100); }
        public String getOrderField() { return orderField; }
        public void setOrderField(String orderField) { this.orderField = orderField; }
        public String getOrderDirection() { return orderDirection; }
        public void setOrderDirection(String orderDirection) { this.orderDirection = orderDirection; }
        public Map<String, Object> getFilters() { return filters; }
        public void setFilters(Map<String, Object> filters) { this.filters = filters; }
        public List<String> getSelectFields() { return selectFields; }
        public void setSelectFields(List<String> selectFields) { this.selectFields = selectFields; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
    }
    
    /**
     * 搜索结果
     */
    class SearchResult {
        private long total;
        private int page;
        private int pageSize;
        private int totalPages;
        private List<Map<String, Object>> records;
        private String sourceResource; // 数据来源资源（单资源时）
        private List<String> sourceResources; // 数据来源资源列表（多资源时）
        
        public SearchResult() {}
        
        public SearchResult(long total, int page, int pageSize, List<Map<String, Object>> records) {
            this.total = total;
            this.page = page;
            this.pageSize = pageSize;
            this.totalPages = (int) Math.ceil((double) total / pageSize);
            this.records = records;
        }
        
        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }
        public int getPage() { return page; }
        public void setPage(int page) { this.page = page; }
        public int getPageSize() { return pageSize; }
        public void setPageSize(int pageSize) { this.pageSize = pageSize; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
        public List<Map<String, Object>> getRecords() { return records; }
        public void setRecords(List<Map<String, Object>> records) { this.records = records; }
        public String getSourceResource() { return sourceResource; }
        public void setSourceResource(String sourceResource) { this.sourceResource = sourceResource; }
        public List<String> getSourceResources() { return sourceResources; }
        public void setSourceResources(List<String> sourceResources) { this.sourceResources = sourceResources; }
    }
    
    /**
     * 关联配置（用于多资源关联查询）
     */
    class JoinConfig {
        private String leftResource;
        private String rightResource;
        private String leftField;
        private String rightField;
        private String joinType = "LEFT"; // LEFT, RIGHT, INNER
        
        public String getLeftResource() { return leftResource; }
        public void setLeftResource(String leftResource) { this.leftResource = leftResource; }
        public String getRightResource() { return rightResource; }
        public void setRightResource(String rightResource) { this.rightResource = rightResource; }
        public String getLeftField() { return leftField; }
        public void setLeftField(String leftField) { this.leftField = leftField; }
        public String getRightField() { return rightField; }
        public void setRightField(String rightField) { this.rightField = rightField; }
        public String getJoinType() { return joinType; }
        public void setJoinType(String joinType) { this.joinType = joinType; }
    }
    
}
