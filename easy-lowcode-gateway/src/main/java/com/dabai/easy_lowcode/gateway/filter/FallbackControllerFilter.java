package com.dabai.easy_lowcode.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * 熔断降级处理过滤器
 * 当后端服务不可用时，返回友好的错误信息
 */
@Slf4j
@Component
public class FallbackControllerFilter implements GlobalFilter, Ordered {

    private static final String FALLBACK_URI = "/fallback/";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String CIRCUIT_BREAKER_KEY = "gateway:circuit:breaker:";

    @Autowired
    private StringRedisTemplate redisTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 如果是降级请求
        if (path.startsWith(FALLBACK_URI)) {
            String serviceName = path.replace(FALLBACK_URI, "");
            return handleFallback(exchange, serviceName);
        }

        return chain.filter(exchange);
    }

    private Mono<Void> handleFallback(ServerWebExchange exchange, String serviceName) {
        ServerHttpResponse response = exchange.getResponse();
        
        // 设置响应头
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        
        // 记录熔断事件
        recordCircuitBreakerEvent(serviceName);

        Map<String, Object> result = new HashMap<>();
        result.put("code", 503);
        result.put("message", "服务暂时不可用，请稍后重试");
        result.put("service", serviceName);
        result.put("timestamp", LocalDateTime.now().format(FORMATTER));
        result.put("suggestion", "请检查服务状态或稍后重试");

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(result);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception e) {
            log.error("Fallback处理失败", e);
            return response.setComplete();
        }
    }

    /**
     * 记录熔断事件到Redis
     */
    private void recordCircuitBreakerEvent(String serviceName) {
        try {
            String key = CIRCUIT_BREAKER_KEY + serviceName;
            redisTemplate.opsForValue().increment(key);
            redisTemplate.expire(key, 24, TimeUnit.HOURS);
            log.warn("服务 {} 发生熔断降级", serviceName);
        } catch (Exception e) {
            log.warn("记录熔断事件失败: {}", e.getMessage());
        }
    }

    @Override
    public int getOrder() {
        return -100; // 高优先级
    }
}
