package com.dabai.easy_lowcode.ai.engine.node;

import com.alibaba.fastjson.JSON;
import com.dabai.easy_lowcode.ai.engine.WorkflowNodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * HTTP 节点执行器
 * <p>
 * 调用外部 HTTP API
 * 配置: { "url": "https://api.example.com", "method": "POST", "headers": {}, "body": {} }
 */
@Slf4j
@Component
public class HttpNodeExecutor implements WorkflowNodeExecutor {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getNodeType() {
        return "http";
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> execute(String nodeId, Map<String, Object> config,
                                       Map<String, Object> input, Map<String, Object> context) {
        String url = (String) config.get("url");
        String method = (String) config.getOrDefault("method", "GET");
        Map<String, String> headers = (Map<String, String>) config.getOrDefault("headers", Map.of());
        Object body = config.get("body");

        log.info("HTTP节点执行: method={}, url={}", method, url);

        try {
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));

            // 设置请求头
            for (Map.Entry<String, String> header : headers.entrySet()) {
                requestBuilder.header(header.getKey(), header.getValue());
            }

            // 设置请求体
            if ("POST".equalsIgnoreCase(method) || "PUT".equalsIgnoreCase(method)) {
                String jsonBody = body != null ? JSON.toJSONString(body) : "";
                requestBuilder.POST(HttpRequest.BodyPublishers.ofString(jsonBody));
            } else if ("DELETE".equalsIgnoreCase(method)) {
                requestBuilder.DELETE();
            } else {
                requestBuilder.GET();
            }

            HttpResponse<String> response = httpClient.send(
                    requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            Map<String, Object> output = new LinkedHashMap<>();
            output.put("statusCode", response.statusCode());
            output.put("body", response.body());

            // 尝试解析 JSON 响应
            try {
                output.put("jsonBody", JSON.parse(response.body()));
            } catch (Exception ignored) {
                // 非 JSON 响应，保持原始 body
            }

            log.info("HTTP节点完成: statusCode={}", response.statusCode());
            return output;

        } catch (Exception e) {
            log.error("HTTP节点执行失败", e);
            throw new RuntimeException("HTTP请求失败: " + e.getMessage(), e);
        }
    }
}
