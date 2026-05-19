package com.dabai.easy_lowcode.resource.controller;

import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.resource.service.ResourceSearchService;
import com.dabai.easy_lowcode.resource.service.ResourceSearchService.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 资源检索控制器
 * 支持单资源检索、多资源统一检索、全文检索
 */
@Slf4j
@RestController
@RequestMapping("/api/resource/search")
@RequiredArgsConstructor
public class ResourceSearchController {

    private final ResourceSearchService searchService;

    // ==================== 单资源检索 ====================

    /**
     * 单资源检索
     * GET /api/resource/search/single/{resourceCode}?page=1&pageSize=20&orderField=id&orderDirection=DESC
     */
    @GetMapping("/single/{resourceCode}")
    public Result<SearchResult> singleSearch(
            @PathVariable String resourceCode,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String orderField,
            @RequestParam(required = false) String orderDirection) {
        
        SearchParams params = new SearchParams();
        params.setPage(page);
        params.setPageSize(pageSize);
        params.setOrderField(orderField);
        params.setOrderDirection(orderDirection);
        
        SearchResult result = searchService.singleSearch(resourceCode, params);
        return Result.success(result);
    }

    /**
     * 单资源检索（带过滤条件）
     * POST /api/resource/search/single/{resourceCode}
     * Body: {"page":1,"pageSize":20,"filters":{"name":"测试","status":1}}
     */
    @PostMapping("/single/{resourceCode}")
    public Result<SearchResult> singleSearchWithFilters(
            @PathVariable String resourceCode,
            @RequestBody SearchParams params) {
        
        SearchResult result = searchService.singleSearch(resourceCode, params);
        return Result.success(result);
    }

    /**
     * 根据ID获取单条记录
     */
    @GetMapping("/single/{resourceCode}/{id}")
    public Result<Map<String, Object>> getById(
            @PathVariable String resourceCode,
            @PathVariable Long id) {
        
        Map<String, Object> record = searchService.singleGetById(resourceCode, id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        return Result.success(record);
    }

    // ==================== 多资源统一检索 ====================

    /**
     * 多资源统一检索
     * POST /api/resource/search/multi
     * Body: {"resourceCodes":["user","role"],"page":1,"pageSize":20}
     */
    @PostMapping("/multi")
    public Result<SearchResult> multiSearch(@RequestBody MultiSearchRequest request) {
        if (request.getResourceCodes() == null || request.getResourceCodes().isEmpty()) {
            return Result.error("请选择至少一个资源");
        }
        SearchResult result = searchService.multiSearch(request.getResourceCodes(), request.getParams());
        return Result.success(result);
    }

    /**
     * 多资源关联检索
     * POST /api/resource/search/join
     * Body: {"leftResource":"user","rightResource":"role","leftField":"role_id","rightField":"id","joinType":"LEFT"}
     */
    @PostMapping("/join")
    public Result<SearchResult> joinSearch(@RequestBody JoinSearchRequest request) {
        JoinConfig config = new JoinConfig();
        config.setLeftResource(request.getLeftResource());
        config.setRightResource(request.getRightResource());
        config.setLeftField(request.getLeftField());
        config.setRightField(request.getRightField());
        config.setJoinType(request.getJoinType());
        
        SearchResult result = searchService.joinSearch(config, request.getParams());
        return Result.success(result);
    }

    // ==================== 全文检索 ====================

    /**
     * 单资源全文检索
     * GET /api/resource/search/fulltext/{resourceCode}?keyword=关键词&page=1&pageSize=20
     */
    @GetMapping("/fulltext/{resourceCode}")
    public Result<SearchResult> fullTextSearch(
            @PathVariable String resourceCode,
            @RequestParam String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer pageSize) {
        
        SearchParams params = new SearchParams();
        params.setPage(page);
        params.setPageSize(pageSize);
        params.setKeyword(keyword);
        
        SearchResult result = searchService.fullTextSearch(resourceCode, keyword, params);
        return Result.success(result);
    }

    /**
     * 多资源全文检索
     * POST /api/resource/search/fulltext/multi
     * Body: {"resourceCodes":["user","order"],"keyword":"关键词","page":1,"pageSize":20}
     */
    @PostMapping("/fulltext/multi")
    public Result<SearchResult> multiFullTextSearch(@RequestBody MultiFullTextRequest request) {
        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            return Result.error("请输入搜索关键词");
        }
        SearchResult result = searchService.multiFullTextSearch(
                request.getResourceCodes(), request.getKeyword(), request.getParams());
        return Result.success(result);
    }

    // ==================== 数据导出 ====================

    /**
     * 导出搜索结果为CSV
     */
    @PostMapping("/export/{resourceCode}")
    public void exportCSV(
            @PathVariable String resourceCode,
            @RequestBody SearchParams params,
            jakarta.servlet.http.HttpServletResponse response) {
        try {
            params.setPage(1);
            params.setPageSize(10000);
            SearchResult result = searchService.singleSearch(resourceCode, params);
            List<Map<String, Object>> records = result.getRecords();

            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader("Content-Disposition", "attachment;filename=" + resourceCode + "_export.csv");

            jakarta.servlet.ServletOutputStream out = response.getOutputStream();
            out.write('\uFEFF');
            if (!records.isEmpty()) {
                Set<String> headers = new java.util.LinkedHashSet<>();
                for (Map<String, Object> record : records) {
                    headers.addAll(record.keySet());
                }
                out.write((String.join(",", headers) + "\n").getBytes("UTF-8"));
                for (Map<String, Object> record : records) {
                    List<String> row = new java.util.ArrayList<>();
                    for (String h : headers) {
                        Object val = record.get(h);
                        if (val == null) {
                            row.add("");
                        } else {
                            String s = val.toString().replace("\"", "\"\"");
                            if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
                                row.add("\"" + s + "\"");
                            } else {
                                row.add(s);
                            }
                        }
                    }
                    out.write((String.join(",", row) + "\n").getBytes("UTF-8"));
                }
            }
            out.flush();
            out.close();
        } catch (Exception e) {
            log.error("导出CSV失败", e);
        }
    }

    // ==================== 辅助接口 ====================

    /**
     * 获取资源字段列表（用于前端构建查询表单）
     */
    @GetMapping("/fields/{resourceCode}")
    public Result<List<FieldConfig>> getResourceFields(@PathVariable String resourceCode) {
        List<FieldConfig> fields = searchService.getResourceFields(resourceCode);
        return Result.success(fields);
    }

    // ==================== 请求DTO ====================

    public static class MultiSearchRequest {
        private List<String> resourceCodes;
        private SearchParams params = new SearchParams();
        
        public List<String> getResourceCodes() { return resourceCodes; }
        public void setResourceCodes(List<String> resourceCodes) { this.resourceCodes = resourceCodes; }
        public SearchParams getParams() { return params; }
        public void setParams(SearchParams params) { this.params = params; }
    }

    public static class JoinSearchRequest {
        private String leftResource;
        private String rightResource;
        private String leftField;
        private String rightField;
        private String joinType = "LEFT";
        private SearchParams params = new SearchParams();
        
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
        public SearchParams getParams() { return params; }
        public void setParams(SearchParams params) { this.params = params; }
    }

    public static class MultiFullTextRequest {
        private List<String> resourceCodes;
        private String keyword;
        private SearchParams params = new SearchParams();
        
        public List<String> getResourceCodes() { return resourceCodes; }
        public void setResourceCodes(List<String> resourceCodes) { this.resourceCodes = resourceCodes; }
        public String getKeyword() { return keyword; }
        public void setKeyword(String keyword) { this.keyword = keyword; }
        public SearchParams getParams() { return params; }
        public void setParams(SearchParams params) { this.params = params; }
    }
}
