package com.dabai.easy_lowcode.ai.agent.tool.impl;

import com.alibaba.fastjson.JSON;
import com.dabai.easy_lowcode.ai.agent.tool.AgentTool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * HTTP 请求工具
 */
@Slf4j
@Component
public class HttpRequestTool implements AgentTool {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String getName() {
        return "http_request";
    }

    @Override
    public String getDescription() {
        return "发送 HTTP 请求到外部 API。支持 GET/POST/PUT/DELETE 方法。";
    }

    @Override
    public Map<String, Object> getParametersSchema() {
        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "url", Map.of("type", "string", "description", "请求URL"),
                        "method", Map.of("type", "string", "description", "HTTP方法", "enum", List.of("GET", "POST", "PUT", "DELETE")),
                        "headers", Map.of("type", "object", "description", "请求头"),
                        "body", Map.of("type", "object", "description", "请求体（JSON）")
                ),
                "required", List.of("url", "method")
        );
    }

    @Override
    @SuppressWarnings("unchecked")
    public Object execute(Map<String, Object> params) {
        String url = (String) params.get("url");
        String method = (String) params.getOrDefault("method", "GET");
        Map<String, String> headers = (Map<String, String>) params.getOrDefault("headers", Map.of());
        Object body = params.get("body");

        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofSeconds(30));

            for (Map.Entry<String, String> h : headers.entrySet()) {
                builder.header(h.getKey(), h.getValue());
            }

            switch (method.toUpperCase()) {
                case "POST" -> builder.POST(HttpRequest.BodyPublishers.ofString(
                        body != null ? JSON.toJSONString(body) : ""));
                case "PUT" -> builder.PUT(HttpRequest.BodyPublishers.ofString(
                        body != null ? JSON.toJSONString(body) : ""));
                case "DELETE" -> builder.DELETE();
                default -> builder.GET();
            }

            HttpResponse<String> response = httpClient.send(builder.build(),
                    HttpResponse.BodyHandlers.ofString());

            return Map.of(
                    "statusCode", response.statusCode(),
                    "body", response.body()
            );
        } catch (Exception e) {
            log.warn("HTTP请求失败: {}", e.getMessage());
            return Map.of("error", "请求失败: " + e.getMessage());
        }
    }

    @Override
    public String getToolType() {
        return "http";
    }
}
