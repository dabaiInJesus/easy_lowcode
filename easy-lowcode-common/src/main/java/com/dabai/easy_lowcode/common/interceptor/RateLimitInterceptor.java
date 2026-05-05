package com.dabai.easy_lowcode.common.interceptor;

import com.dabai.easy_lowcode.collector.entity.ApiManagement;
import com.dabai.easy_lowcode.collector.mapper.ApiManagementMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * API限流拦截器。
 * 基于ApiManagement表中配置的rateLimit字段，对每个API路径+方法做访问频率控制。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ApiManagementMapper apiManagementMapper;

    /** API路径+方法 -> 限流计数器 */
    private final Map<String, ApiRateCounter> counterMap = new ConcurrentHashMap<>();

    /** 缓存API配置（路径+方法 -> rateLimit） */
    private final Map<String, Integer> apiRateLimitCache = new ConcurrentHashMap<>();
    private volatile long lastCacheRefresh = 0;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 只拦截 /api/ 开头的动态API
        if (!path.startsWith("/api/")) return true;

        // 获取该API的限流配置
        Integer rateLimit = getRateLimit(path, method);
        if (rateLimit == null || rateLimit <= 0) return true; // 不限流

        String key = method + ":" + path;
        ApiRateCounter counter = counterMap.computeIfAbsent(key, k -> new ApiRateCounter(rateLimit));

        if (counter.tryAcquire()) {
            return true;
        }

        // 限流：返回429
        log.warn("API触发限流: {} {} (限制: {}/分钟)", method, path, rateLimit);
        response.setStatus(429);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\",\"data\":null,\"timestamp\":" + System.currentTimeMillis() + "}");
        return false;
    }

    private Integer getRateLimit(String path, String method) {
        // 缓存刷新：每30秒刷新一次
        long now = System.currentTimeMillis();
        if (now - lastCacheRefresh > 30000) {
            refreshCache();
            lastCacheRefresh = now;
        }

        // 精确匹配
        String key = method + ":" + path;
        Integer limit = apiRateLimitCache.get(key);
        if (limit != null) return limit;

        // 路径模板匹配：/api/collector/table-resource/{id}/preview -> /api/collector/table-resource/*/preview
        String patternKey = method + ":" + path.replaceAll("/\\d+/", "/*/");
        return apiRateLimitCache.get(patternKey);
    }

    private void refreshCache() {
        try {
            apiRateLimitCache.clear();
            java.util.List<ApiManagement> apis = apiManagementMapper.selectList(null);
            for (ApiManagement api : apis) {
                if (api.getRateLimit() != null && api.getRateLimit() > 0) {
                    apiRateLimitCache.put(api.getApiMethod() + ":" + api.getApiPath(), api.getRateLimit());
                }
            }
            log.debug("API限流缓存已刷新，共 {} 条配置", apiRateLimitCache.size());
        } catch (Exception e) {
            log.warn("刷新API限流缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 基于令牌桶的计数器（简化版：固定窗口）
     */
    static class ApiRateCounter {
        private final int maxRequestsPerMinute;
        private final AtomicLong count = new AtomicLong(0);
        private volatile long windowStart = System.currentTimeMillis();

        ApiRateCounter(int maxRequestsPerMinute) {
            this.maxRequestsPerMinute = maxRequestsPerMinute;
        }

        synchronized boolean tryAcquire() {
            long now = System.currentTimeMillis();
            if (now - windowStart > 60000) {
                // 新窗口
                count.set(0);
                windowStart = now;
            }
            if (count.get() < maxRequestsPerMinute) {
                count.incrementAndGet();
                return true;
            }
            return false;
        }
    }
}
