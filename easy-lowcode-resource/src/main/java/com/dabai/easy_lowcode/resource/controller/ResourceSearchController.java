package com.dabai.easy_lowcode.resource.controller;

import com.dabai.easy_lowcode.common.result.Result;
import com.dabai.easy_lowcode.resource.model.FieldConfig;
import com.dabai.easy_lowcode.resource.service.ResourceSearchService;
import org.springframework.security.access.prepost.PreAuthorize;
import com.dabai.easy_lowcode.resource.service.ResourceSearchService.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "资源检索", description = "单资源检索、多资源检索、关联检索、全文检索及数据导出")
@Slf4j
@RestController
@RequestMapping("/api/resource/search")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ResourceSearchController {

    private final ResourceSearchService searchService;

    @Operation(summary = "单资源检索（GET）", description = "根据资源编码检索数据，支持分页和排序")
    @ApiResponse(responseCode = "200", description = "检索成功")
    @GetMapping("/single/{resourceCode}")
    public Result<SearchResult> singleSearch(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize,
            @Parameter(description = "排序字段") @RequestParam(required = false) String orderField,
            @Parameter(description = "排序方向（ASC/DESC）") @RequestParam(required = false) String orderDirection) {
        
        SearchParams params = new SearchParams();
        params.setPage(page);
        params.setPageSize(pageSize);
        params.setOrderField(orderField);
        params.setOrderDirection(orderDirection);
        
        SearchResult result = searchService.singleSearch(resourceCode, params);
        return Result.success(result);
    }

    @Operation(summary = "单资源检索（POST）", description = "根据资源编码检索数据，支持过滤条件")
    @ApiResponse(responseCode = "200", description = "检索成功")
    @PostMapping("/single/{resourceCode}")
    public Result<SearchResult> singleSearchWithFilters(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
            @RequestBody SearchParams params) {
        
        SearchResult result = searchService.singleSearch(resourceCode, params);
        return Result.success(result);
    }

    @Operation(summary = "根据ID获取记录", description = "根据资源编码和ID获取单条记录详情")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/single/{resourceCode}/{id}")
    public Result<Map<String, Object>> getById(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
            @Parameter(description = "记录ID") @PathVariable Long id) {
        
        Map<String, Object> record = searchService.singleGetById(resourceCode, id);
        if (record == null) {
            return Result.error("记录不存在");
        }
        return Result.success(record);
    }

    @Operation(summary = "多资源统一检索", description = "同时在多个资源中检索数据")
    @ApiResponse(responseCode = "200", description = "检索成功")
    @PostMapping("/multi")
    public Result<SearchResult> multiSearch(@RequestBody MultiSearchRequest request) {
        if (request.getResourceCodes() == null || request.getResourceCodes().isEmpty()) {
            return Result.error("请选择至少一个资源");
        }
        SearchResult result = searchService.multiSearch(request.getResourceCodes(), request.getParams());
        return Result.success(result);
    }

    @Operation(summary = "多资源关联检索", description = "执行两个资源之间的关联查询")
    @ApiResponse(responseCode = "200", description = "检索成功")
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

    @Operation(summary = "单资源全文检索", description = "在指定资源中进行全文关键词检索")
    @ApiResponse(responseCode = "200", description = "检索成功")
    @GetMapping("/fulltext/{resourceCode}")
    public Result<SearchResult> fullTextSearch(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
            @Parameter(description = "搜索关键词", required = true) @RequestParam String keyword,
            @Parameter(description = "页码") @RequestParam(required = false) Integer page,
            @Parameter(description = "每页条数") @RequestParam(required = false) Integer pageSize) {
        
        SearchParams params = new SearchParams();
        params.setPage(page);
        params.setPageSize(pageSize);
        params.setKeyword(keyword);
        
        SearchResult result = searchService.fullTextSearch(resourceCode, keyword, params);
        return Result.success(result);
    }

    @Operation(summary = "多资源全文检索", description = "在多个资源中同时进行全文关键词检索")
    @ApiResponse(responseCode = "200", description = "检索成功")
    @PostMapping("/fulltext/multi")
    public Result<SearchResult> multiFullTextSearch(@RequestBody MultiFullTextRequest request) {
        if (request.getKeyword() == null || request.getKeyword().trim().isEmpty()) {
            return Result.error("请输入搜索关键词");
        }
        SearchResult result = searchService.multiFullTextSearch(
                request.getResourceCodes(), request.getKeyword(), request.getParams());
        return Result.success(result);
    }

    @Operation(summary = "统一Key搜索", description = "根据统一Key跨资源精确查询，如按邮箱查询所有包含邮箱字段的资源")
    @ApiResponse(responseCode = "200", description = "搜索成功")
    @PostMapping("/unified")
    public Result<SearchResult> unifiedSearch(@RequestBody UnifiedSearchRequest request) {
        if (request.getUnifiedKey() == null || request.getUnifiedKey().isEmpty()) {
            return Result.error("请指定统一Key");
        }
        if (request.getValue() == null || request.getValue().isEmpty()) {
            return Result.error("请输入查询值");
        }
        SearchParams params = request.getParams() != null ? request.getParams() : new SearchParams();
        SearchResult result = searchService.unifiedKeySearch(request.getUnifiedKey(), request.getValue(), params);
        return Result.success(result);
    }

    @Operation(summary = "导出搜索结果为CSV", description = "将搜索结果导出为CSV文件")
    @ApiResponse(responseCode = "200", description = "导出成功")
    @PostMapping("/export/{resourceCode}")
    public void exportCSV(
            @Parameter(description = "资源编码") @PathVariable String resourceCode,
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

    @Operation(summary = "获取资源字段列表", description = "获取指定资源的字段配置（含显示配置），用于前端构建查询表单")
    @ApiResponse(responseCode = "200", description = "获取成功")
    @GetMapping("/fields/{resourceCode}")
    public Result<Map<String, Object>> getResourceFields(@Parameter(description = "资源编码") @PathVariable String resourceCode) {
        Map<String, Object> result = searchService.getResourceFieldInfo(resourceCode);
        return Result.success(result);
    }

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

    public static class UnifiedSearchRequest {
        private String unifiedKey;
        private String value;
        private SearchParams params = new SearchParams();

        public String getUnifiedKey() { return unifiedKey; }
        public void setUnifiedKey(String unifiedKey) { this.unifiedKey = unifiedKey; }
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
        public SearchParams getParams() { return params; }
        public void setParams(SearchParams params) { this.params = params; }
    }
}
