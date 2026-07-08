package com.dabai.easy_lowcode.collector.service.search;

import java.util.List;
import java.util.Map;

public interface SearchService {

    void indexDocument(String docId, String fileName, String content, String fileType, String resourceCode);

    void batchIndexDocuments(List<IndexDocRequest> docs);

    void deleteDocument(String docId);

    SearchResult search(String keyword, int page, int pageSize, String resourceCode);

    void createIndex(String indexName);

    String getEngineType();

    record IndexDocRequest(String docId, String fileName, String content, String fileType, String resourceCode) {}

    record SearchResult(List<Map<String, Object>> records, long total, int page, int pageSize) {}
}
