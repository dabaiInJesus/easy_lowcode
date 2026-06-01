package com.dabai.easy_lowcode.common.aspect;

import com.dabai.easy_lowcode.common.annotation.RateLimit;
import com.dabai.easy_lowcode.common.util.CacheUtil;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 限流切面
 * 基于Redis实现滑动窗口限流
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final CacheUtil cacheUtil;

    @Around("@annotation(com.dabai.easy_lowcode.common.annotation.RateLimit)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        RateLimit rateLimit = method.getAnnotation(RateLimit.class);
        
        if (rateLimit == null) {
            return point.proceed();
        }

        HttpServletRequest request = getCurrentRequest();
        if (request == null) {
            return point.proceed();
        }

        // 构建限流key
        String limitKey = buildLimitKey(rateLimit, request, method);
        
        // 获取当前请求计数
        Long currentCount = cacheUtil.increment(limitKey);
        
        if (currentCount == null) {
            // Redis不可用时，放过请求
            return point.proceed();
        }

        // 第一次请求，设置过期时间
        if (currentCount == 1) {
            cacheUtil.expire(limitKey, rateLimit.window(), TimeUnit.SECONDS);
        }

        // 检查是否超过限制
        if (currentCount > rateLimit.maxRequests()) {
            log.warn("Rate limit exceeded: key={}, count={}, limit={}", 
                limitKey, currentCount, rateLimit.maxRequests());
            throw new BusinessException(rateLimit.message());
        }

        log.debug("Rate limit check passed: key={}, count={}, limit={}", 
            limitKey, currentCount, rateLimit.maxRequests());
        
        return point.proceed();
    }

    private String buildLimitKey(RateLimit rateLimit, HttpServletRequest request, Method method) {
        String prefix = rateLimit.key();
        if (prefix == null || prefix.isEmpty()) {
            prefix = method.getName();
        }
        
        // 尝试获取用户标识（IP或用户ID）
        String identifier = getClientIp(request);
        
        return "rate-limit:" + prefix + ":" + identifier;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}