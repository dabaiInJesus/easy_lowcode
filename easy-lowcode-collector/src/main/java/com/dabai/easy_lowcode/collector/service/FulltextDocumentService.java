package com.dabai.easy_lowcode.collector.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.dabai.easy_lowcode.collector.entity.FulltextDocument;

import java.util.List;
import java.util.Map;

public interface FulltextDocumentService extends IService<FulltextDocument> {

    FulltextDocument uploadFile(String fileName, long fileSize, java.io.InputStream content, String contentType, String resourceCode);

    void reindexDocument(Long id);

    void deleteDocument(Long id);

    Map<String, Object> searchDocuments(String keyword, int page, int pageSize, String resourceCode);

    List<FulltextDocument> listPendingIndex(int limit);
}
