package com.dabai.easy_lowcode.gateway.filter;

import cn.dev33.satoken.reactor.context.SaReactorSyncHolder;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 全局认证过滤器
 */
@Slf4j
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {
    
    @Value("${gateway.auth.whitelist:/api/auth/login,/api/auth/register}")
    private List<String> whiteList;
    
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        
        log.debug("请求路径: {}", path);
        
        // 检查是否在白名单中
        if (isWhiteList(path)) {
            return chain.filter(exchange);
        }
        
        // 验证 Token
        try {
            String token = request.getHeaders().getFirst("satoken");
            
            if (token == null || token.isEmpty()) {
                return unauthorized(exchange.getResponse(), "未登录或Token已过期");
            }
            
            // 验证 Token 有效性
            StpUtil.checkLogin();
            
            log.debug("用户ID: {}", StpUtil.getLoginId());
            
            // 将用户信息传递给下游服务
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", StpUtil.getLoginId().toString())
                .build();
            
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
            
        } catch (Exception e) {
            log.error("认证失败: {}", e.getMessage());
            return unauthorized(exchange.getResponse(), "认证失败: " + e.getMessage());
        }
    }
    
    /**
     * 检查是否在白名单中
     */
    private boolean isWhiteList(String path) {
        return whiteList.stream().anyMatch(path::startsWith);
    }
    
    /**
     * 返回未授权响应
     */
    private Mono<Void> unauthorized(ServerHttpResponse response, String message) {
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        
        String body = String.format("{\"code\":401,\"message\":\"%s\",\"data\":null}", message);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }
    
    @Override
    public int getOrder() {
        return -100;
    }
}
