package com.dabai.easy_lowcode.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * 全局日志过滤器
 */
@Slf4j
@Component
public class LoggingGlobalFilter implements GlobalFilter, Ordered {
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String method = request.getMethod().toString();
        String requestId = java.util.UUID.randomUUID().toString().replace("-", "");
        
        LocalDateTime startTime = LocalDateTime.now();
        
        log.info(">>> 请求开始 [{}] {} {} | RequestId: {}", 
            method, path, startTime, requestId);
        
        // 添加 RequestId 到请求头
        ServerHttpRequest modifiedRequest = request.mutate()
            .header("X-Request-Id", requestId)
            .build();
        
        return chain.filter(exchange.mutate().request(modifiedRequest).build())
            .then(Mono.fromRunnable(() -> {
                LocalDateTime endTime = LocalDateTime.now();
                long duration = java.time.Duration.between(startTime, endTime).toMillis();
                
                log.info("<<< 请求结束 [{}] {} | Duration: {}ms | RequestId: {}", 
                    method, path, duration, requestId);
            }));
    }
    
    @Override
    public int getOrder() {
        return -200; // 在认证过滤器之前执行
    }
}
