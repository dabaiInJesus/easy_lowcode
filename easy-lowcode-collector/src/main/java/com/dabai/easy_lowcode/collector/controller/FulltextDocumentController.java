package com.dabai.easy_lowcode.collector.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.dabai.easy_lowcode.collector.entity.FulltextDocument;
import com.dabai.easy_lowcode.collector.service.FulltextDocumentService;
import com.dabai.easy_lowcode.common.result.Result;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Tag(name = "全文检索管理", description = "文件上传、全文搜索、文档管理")
@Slf4j
@RestController
@RequestMapping("/api/collector/fulltext")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class FulltextDocumentController {

    private final FulltextDocumentService documentService;

    @Operation(summary = "上传文件并索引")
    @PostMapping("/upload")
    public Result<FulltextDocument> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String resourceCode) {
        if (file.isEmpty()) {
            return Result.error("请选择文件");
        }
        try {
            FulltextDocument doc = documentService.uploadFile(
                    file.getOriginalFilename(),
                    file.getSize(),
                    file.getInputStream(),
                    file.getContentType(),
                    resourceCode
            );
            return Result.success(doc);
        } catch (IOException e) {
            log.error("文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "全文搜索")
    @GetMapping("/search")
    public Result<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(required = false) String resourceCode) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.error("请输入搜索关键词");
        }
        Map<String, Object> result = documentService.searchDocuments(keyword.trim(), page, pageSize, resourceCode);
        return Result.success(result);
    }

    @Operation(summary = "文档列表")
    @GetMapping("/page")
    public Result<Page<FulltextDocument>> page(
            @RequestParam(defaultValue = "1") int current,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword) {
        Page<FulltextDocument> page = new Page<>(current, size);
        LambdaQueryWrapper<FulltextDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FulltextDocument::getDeleted, 0);
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like(FulltextDocument::getFileName, keyword);
        }
        wrapper.orderByDesc(FulltextDocument::getCreateTime);
        return Result.success(documentService.page(page, wrapper));
    }

    @Operation(summary = "文档详情")
    @GetMapping("/{id}")
    public Result<FulltextDocument> getById(@PathVariable Long id) {
        FulltextDocument doc = documentService.getById(id);
        if (doc == null) return Result.error("文档不存在");
        return Result.success(doc);
    }

    @Operation(summary = "重新索引")
    @PostMapping("/{id}/reindex")
    public Result<Void> reindex(@PathVariable Long id) {
        documentService.reindexDocument(id);
        return Result.success("重新索引成功");
    }

    @Operation(summary = "删除文档")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('admin')")
    public Result<Void> delete(@PathVariable Long id) {
        documentService.deleteDocument(id);
        return Result.success("删除成功");
    }
}
