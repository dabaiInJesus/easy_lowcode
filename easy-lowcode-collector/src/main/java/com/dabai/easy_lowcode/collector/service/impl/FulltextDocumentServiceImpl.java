package com.dabai.easy_lowcode.collector.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.dabai.easy_lowcode.collector.entity.FulltextDocument;
import com.dabai.easy_lowcode.collector.mapper.FulltextDocumentMapper;
import com.dabai.easy_lowcode.collector.service.FulltextDocumentService;
import com.dabai.easy_lowcode.collector.service.TikaContentExtractor;
import com.dabai.easy_lowcode.collector.service.search.SearchService;
import com.dabai.easy_lowcode.collector.service.storage.StorageService;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class FulltextDocumentServiceImpl extends ServiceImpl<FulltextDocumentMapper, FulltextDocument> implements FulltextDocumentService {

    private final StorageService storageService;
    private final SearchService searchService;
    private final TikaContentExtractor tikaExtractor;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FulltextDocument uploadFile(String fileName, long fileSize, InputStream content, String contentType, String resourceCode) {
        // 1. 读取内容到字节数组（同时用于存储和Tika，避免两次网络往返）
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        try { while ((n = content.read(data, 0, data.length)) != -1) buffer.write(data, 0, n); } catch (Exception e) { throw new RuntimeException("读取文件失败", e); }
        byte[] fileBytes = buffer.toByteArray();

        // 2. 存储文件
        String storagePath = storageService.upload(fileName, new java.io.ByteArrayInputStream(fileBytes), fileSize, contentType);

        // 3. Tika提取内容（使用内存中的字节数组，无需重新下载）
        TikaContentExtractor.TikaResult tikaResult = tikaExtractor.extract(new java.io.ByteArrayInputStream(fileBytes), fileName);

        // 4. 保存记录
        FulltextDocument doc = new FulltextDocument();
        doc.setFileName(fileName);
        doc.setFileType(tikaResult.getDetectedType());
        doc.setFileSize(fileSize);
        doc.setStorageType(storageService.getStorageType());
        doc.setStoragePath(storagePath);
        doc.setContentText(tikaResult.getContentText());
        doc.setResourceCode(resourceCode);
        doc.setSearchEngine(searchService.getEngineType());
        doc.setIndexed(0);
        doc.setDeleted(0);
        save(doc);

        // 5. 索引到搜索引擎
        try {
            searchService.indexDocument(
                    String.valueOf(doc.getId()),
                    fileName,
                    tikaResult.getContentText(),
                    tikaResult.getDetectedType(),
                    resourceCode
            );
            doc.setIndexed(1);
            doc.setIndexError(null);
        } catch (Exception e) {
            log.error("索引失败: {} - {}", fileName, e.getMessage());
            doc.setIndexed(2);
            doc.setIndexError(e.getMessage());
        }
        updateById(doc);

        return doc;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reindexDocument(Long id) {
        FulltextDocument doc = getById(id);
        if (doc == null) throw new BusinessException("文档不存在");

        try {
            searchService.indexDocument(
                    String.valueOf(doc.getId()),
                    doc.getFileName(),
                    doc.getContentText() != null ? doc.getContentText() : "",
                    doc.getFileType(),
                    doc.getResourceCode()
            );
            doc.setIndexed(1);
            doc.setIndexError(null);
        } catch (Exception e) {
            doc.setIndexed(2);
            doc.setIndexError(e.getMessage());
        }
        updateById(doc);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long id) {
        FulltextDocument doc = getById(id);
        if (doc == null) return;

        // 删除搜索引擎中的文档
        try {
            searchService.deleteDocument(String.valueOf(id));
        } catch (Exception e) {
            log.warn("删除搜索引擎文档失败: {}", id, e);
        }

        // 删除存储文件
        storageService.delete(doc.getStoragePath());

        // 逻辑删除记录
        lambdaUpdate().set(FulltextDocument::getDeleted, 1)
                .eq(FulltextDocument::getId, id)
                .update();
    }

    @Override
    public Map<String, Object> searchDocuments(String keyword, int page, int pageSize, String resourceCode) {
        // 优先使用搜索引擎
        try {
            SearchService.SearchResult searchResult = searchService.search(keyword, page, pageSize, resourceCode);
            List<Map<String, Object>> records = searchResult.records();

            // 补充文件ID对应的数据
            for (Map<String, Object> record : records) {
                String docId = String.valueOf(record.get("id"));
                if (docId != null) {
                    FulltextDocument doc = getById(Long.parseLong(docId));
                    if (doc != null) {
                        record.put("fileName", doc.getFileName());
                        record.put("fileType", doc.getFileType());
                        record.put("fileSize", doc.getFileSize());
                        record.put("storagePath", doc.getStoragePath());
                        record.put("fileUrl", storageService.getFileUrl(doc.getStoragePath()));
                    }
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records", records);
            result.put("total", searchResult.total());
            result.put("page", searchResult.page());
            result.put("pageSize", searchResult.pageSize());
            return result;
        } catch (Exception e) {
            log.warn("搜索引擎不可用，降级为数据库LIKE搜索: {}", e.getMessage());
            // 降级方案
            IPage<FulltextDocument> mpPage = new Page<>(page, pageSize);
            List<FulltextDocument> docs = baseMapper.searchByContentLike(mpPage, keyword);
            long total = mpPage.getTotal();
            if (docs.isEmpty() && total > 0) {
                docs = baseMapper.searchByContentLike(new Page<>(1, pageSize), keyword);
            }

            List<Map<String, Object>> records = new ArrayList<>();
            for (FulltextDocument doc : docs) {
                Map<String, Object> record = new LinkedHashMap<>();
                record.put("id", String.valueOf(doc.getId()));
                record.put("fileName", doc.getFileName());
                record.put("fileType", doc.getFileType());
                record.put("fileSize", doc.getFileSize());
                record.put("fileUrl", storageService.getFileUrl(doc.getStoragePath()));
                record.put("snippet", truncateWithKeyword(doc.getContentText(), keyword, 200));
                records.add(record);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("records", records);
            result.put("total", total);
            result.put("page", page);
            result.put("pageSize", pageSize);
            return result;
        }
    }

    @Override
    public List<FulltextDocument> listPendingIndex(int limit) {
        return baseMapper.selectPendingIndex(new Page<>(1, limit));
    }

    private String truncateWithKeyword(String text, String keyword, int maxLen) {
        if (text == null || text.isEmpty()) return "";
        int idx = text.toLowerCase().indexOf(keyword.toLowerCase());
        if (idx < 0) return text.substring(0, Math.min(text.length(), maxLen));
        int start = Math.max(0, idx - 60);
        int end = Math.min(text.length(), idx + keyword.length() + 60);
        String snippet = text.substring(start, end);
        if (start > 0) snippet = "..." + snippet;
        if (end < text.length()) snippet += "...";
        return snippet.length() > maxLen ? snippet.substring(0, maxLen) + "..." : snippet;
    }
}
