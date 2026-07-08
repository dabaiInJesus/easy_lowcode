package com.dabai.easy_lowcode.collector.service.search;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "easy-lowcode.search.engine", havingValue = "elasticsearch")
public class ElasticsearchSearchService implements SearchService {

    @Setter
    @Value("${easy-lowcode.search.elasticsearch.host:localhost:9200}")
    private String host;

    @Setter
    @Value("${easy-lowcode.search.elasticsearch.username:}")
    private String username;

    @Setter
    @Value("${easy-lowcode.search.elasticsearch.password:}")
    private String password;

    @Setter
    @Value("${easy-lowcode.search.elasticsearch.index-name:fulltext_docs}")
    private String indexName;

    @PostConstruct
    public void init() {
        log.info("Elasticsearch 初始化配置: host={}, index={}", host, indexName);
        log.warn("Elasticsearch 客户端需引入 spring-boot-starter-data-elasticsearch 方可使用");
    }

    @Override
    public void indexDocument(String docId, String fileName, String content, String fileType, String resourceCode) {
        log.warn("ES索引功能需引入ES客户端依赖后实现");
    }

    @Override
    public void batchIndexDocuments(List<IndexDocRequest> docs) {
        log.warn("ES批量索引功能需引入ES客户端依赖后实现");
    }

    @Override
    public void deleteDocument(String docId) {
        log.warn("ES删除功能需引入ES客户端依赖后实现");
    }

    @Override
    public SearchResult search(String keyword, int page, int pageSize, String resourceCode) {
        log.warn("ES搜索功能需引入ES客户端依赖后实现");
        return new SearchResult(Collections.emptyList(), 0, page, pageSize);
    }

    @Override
    public void createIndex(String name) {
        log.warn("ES创建索引功能需引入ES客户端依赖后实现");
    }

    @Override
    public String getEngineType() {
        return "elasticsearch";
    }
}
