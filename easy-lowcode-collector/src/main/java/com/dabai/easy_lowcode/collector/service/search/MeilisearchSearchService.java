package com.dabai.easy_lowcode.collector.service.search;

import com.meilisearch.sdk.Client;
import com.meilisearch.sdk.Config;
import com.meilisearch.sdk.Index;
import com.meilisearch.sdk.SearchRequest;
import com.meilisearch.sdk.model.SearchResult;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "easy-lowcode.search.engine", havingValue = "meilisearch", matchIfMissing = true)
public class MeilisearchSearchService implements SearchService {

    @Setter
    @Value("${easy-lowcode.search.meilisearch.host:http://localhost:7700}")
    private String host;

    @Setter
    @Value("${easy-lowcode.search.meilisearch.api-key:}")
    private String apiKey;

    @Setter
    @Value("${easy-lowcode.search.meilisearch.index-name:fulltext_docs}")
    private String indexName;

    private Client client;
    private Index index;

    @PostConstruct
    public void init() {
        try {
            client = new Client(new Config(host, apiKey));
            client.createIndex(indexName, "id");
            index = client.index(indexName);
            index.updateSearchableAttributesSettings(new String[]{"fileName", "content", "fileType"});
            index.updateFilterableAttributesSettings(new String[]{"fileType", "resourceCode"});
            log.info("Meilisearch 初始化完成: {}", host);
        } catch (Exception e) {
            log.warn("Meilisearch 初始化失败(可稍后重试): {}", e.getMessage());
        }
    }

    @Override
    public void indexDocument(String docId, String fileName, String content, String fileType, String resourceCode) {
        try {
            Map<String, Object> doc = new LinkedHashMap<>();
            doc.put("id", docId);
            doc.put("fileName", fileName);
            doc.put("content", content);
            doc.put("fileType", fileType);
            doc.put("resourceCode", resourceCode != null ? resourceCode : "");
            if (index != null) {
                index.addDocumentsJson(meilisearchJson(doc));
            }
        } catch (Exception e) {
            log.error("Meilisearch 索引失败: {}", docId, e);
            throw new RuntimeException("索引失败: " + e.getMessage());
        }
    }

    @Override
    public void batchIndexDocuments(List<IndexDocRequest> docs) {
        try {
            List<Map<String, Object>> documents = new ArrayList<>();
            for (IndexDocRequest doc : docs) {
                Map<String, Object> d = new LinkedHashMap<>();
                d.put("id", doc.docId());
                d.put("fileName", doc.fileName());
                d.put("content", doc.content());
                d.put("fileType", doc.fileType());
                d.put("resourceCode", doc.resourceCode() != null ? doc.resourceCode() : "");
                documents.add(d);
            }
            if (index != null) {
                index.addDocumentsJson(meilisearchJsonArray(documents));
            }
        } catch (Exception e) {
            log.error("Meilisearch 批量索引失败", e);
            throw new RuntimeException("批量索引失败: " + e.getMessage());
        }
    }

    @Override
    public void deleteDocument(String docId) {
        try {
            if (index != null) {
                index.deleteDocument(docId);
            }
        } catch (Exception e) {
            log.warn("Meilisearch 删除文档失败: {}", docId, e);
        }
    }

    @Override
    public SearchResult search(String keyword, int page, int pageSize, String resourceCode) {
        try {
            if (index == null) {
                return new SearchResult(Collections.emptyList(), 0, page, pageSize);
            }

            var requestBuilder = SearchRequest.builder()
                    .setQuery(keyword)
                    .setLimit(pageSize)
                    .setOffset((page - 1) * pageSize)
                    .setAttributesToHighlight(new String[]{"content"});

            if (resourceCode != null && !resourceCode.isEmpty()) {
                requestBuilder.setFilter("resourceCode = \"" + resourceCode.replace("\"", "\\\"") + "\"");
            }

            SearchResult result = index.search(requestBuilder.build());

            List<Map<String, Object>> records = new ArrayList<>();
            for (var hit : result.getHits()) {
                @SuppressWarnings("unchecked")
                Map<String, Object> hitMap = (Map<String, Object>) hit;
                Map<String, Object> record = new LinkedHashMap<>(hitMap);

                @SuppressWarnings("unchecked")
                Map<String, Object> formatted = (Map<String, Object>) hitMap.get("_formatted");
                if (formatted != null) {
                    record.put("snippet", formatted.getOrDefault("content", ""));
                }

                record.remove("_formatted");
                records.add(record);
            }

            return new SearchResult(records, result.getTotalHits(), page, pageSize);
        } catch (Exception e) {
            log.error("Meilisearch 搜索失败: {}", keyword, e);
            return new SearchResult(Collections.emptyList(), 0, page, pageSize);
        }
    }

    @Override
    public void createIndex(String name) {
        try {
            client.createIndex(name, "id");
        } catch (Exception e) {
            log.warn("创建Meilisearch索引失败: {}", name, e);
        }
    }

    @Override
    public String getEngineType() {
        return "meilisearch";
    }

    private String meilisearchJson(Map<String, Object> doc) {
        StringBuilder sb = new StringBuilder("[{");
        boolean first = true;
        for (var entry : doc.entrySet()) {
            if (!first) sb.append(",");
            first = false;
            sb.append("\"").append(entry.getKey()).append("\":");
            if (entry.getValue() instanceof String) {
                sb.append("\"").append(escapeJson((String) entry.getValue())).append("\"");
            } else {
                sb.append(entry.getValue());
            }
        }
        sb.append("}]");
        return sb.toString();
    }

    private String meilisearchJsonArray(List<Map<String, Object>> docs) {
        StringBuilder sb = new StringBuilder("[");
        boolean first = true;
        for (var doc : docs) {
            if (!first) sb.append(",");
            first = false;
            sb.append("{");
            boolean innerFirst = true;
            for (var entry : doc.entrySet()) {
                if (!innerFirst) sb.append(",");
                innerFirst = false;
                sb.append("\"").append(entry.getKey()).append("\":");
                if (entry.getValue() instanceof String) {
                    sb.append("\"").append(escapeJson((String) entry.getValue())).append("\"");
                } else {
                    sb.append(entry.getValue());
                }
            }
            sb.append("}");
        }
        sb.append("]");
        return sb.toString();
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
