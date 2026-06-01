package com.dabai.easy_lowcode.common.aspect;

import com.dabai.easy_lowcode.common.annotation.CacheEvict;
import com.dabai.easy_lowcode.common.annotation.Cacheable;
import com.dabai.easy_lowcode.common.util.CacheUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

/**
 * 缓存切面
 * 实现自动缓存读取、更新、失效
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class CacheAspect {

    private final CacheUtil cacheUtil;
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    /**
     * Cacheable 缓存读取切面
     */
    @Around("@annotation(com.dabai.easy_lowcode.common.annotation.Cacheable)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        Cacheable cacheable = method.getAnnotation(Cacheable.class);
        
        if (cacheable == null) {
            return point.proceed();
        }

        // 生成缓存key
        String cacheKey = buildCacheKey(cacheable.key(), point, method);
        
        // 尝试从缓存获取
        try {
            String cachedValue = cacheUtil.get(cacheKey);
            if (cachedValue != null) {
                log.debug("Cache hit: key={}", cacheKey);
                return parseJsonValue(cachedValue, method);
            }
        } catch (Exception e) {
            log.warn("Cache read failed: key={}, error={}", cacheKey, e.getMessage());
        }

        log.debug("Cache miss: key={}", cacheKey);
        
        // 分布式锁防止缓存击穿
        String lockKey = "lock:" + cacheKey;
        if (cacheable.distributedLock()) {
            boolean locked = cacheUtil.tryLock(lockKey, "1", cacheable.lockTimeout(), TimeUnit.SECONDS);
            if (!locked) {
                // 等待后重试
                Thread.sleep(100);
                String retryValue = cacheUtil.get(cacheKey);
                if (retryValue != null) {
                    return parseJsonValue(retryValue, method);
                }
            }
        }

        try {
            // 执行方法
            Object result = point.proceed();
            
            // 缓存结果
            if (result != null && cacheable.fallback()) {
                try {
                    String jsonValue = toJsonValue(result);
                    cacheUtil.set(cacheKey, jsonValue, cacheable.expire(), TimeUnit.SECONDS);
                    log.debug("Cache saved: key={}, expire={}s", cacheKey, cacheable.expire());
                } catch (Exception e) {
                    log.warn("Cache write failed: key={}, error={}", cacheKey, e.getMessage());
                }
            }
            
            return result;
        } finally {
            if (cacheable.distributedLock()) {
                cacheUtil.releaseLock(lockKey, "1");
            }
        }
    }

    /**
     * CacheEvict 缓存失效切面
     */
    @Around("@annotation(com.dabai.easy_lowcode.common.annotation.CacheEvict)")
    public Object evictAround(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        CacheEvict cacheEvict = method.getAnnotation(CacheEvict.class);
        
        if (cacheEvict == null) {
            return point.proceed();
        }

        // 执行前清除
        if (cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, point, method);
        }

        Object result = point.proceed();

        // 执行后清除
        if (!cacheEvict.beforeInvocation()) {
            evictCache(cacheEvict, point, method);
        }

        return result;
    }

    private void evictCache(CacheEvict cacheEvict, ProceedingJoinPoint point, Method method) {
        try {
            String pattern = buildCacheKey(cacheEvict.key(), point, method);
            
            if (cacheEvict.allEntries()) {
                Long deleted = cacheUtil.deleteByPattern(pattern);
                log.debug("Cache evicted by pattern: pattern={}, count={}", pattern, deleted);
            } else {
                cacheUtil.delete(pattern);
                log.debug("Cache evicted: key={}", pattern);
            }
        } catch (Exception e) {
            log.warn("Cache evict failed: error={}", e.getMessage());
        }
    }

    private String buildCacheKey(String keyTemplate, ProceedingJoinPoint point, Method method) {
        // 解析SpEL表达式
        EvaluationContext context = createEvaluationContext(point, method);
        
        if (keyTemplate.contains("#")) {
            try {
                Object value = parser.parseExpression(keyTemplate).getValue(context);
                return String.valueOf(value);
            } catch (Exception e) {
                log.warn("SpEL parse failed, using raw key: {}", e.getMessage());
            }
        }
        
        return keyTemplate;
    }

    private EvaluationContext createEvaluationContext(ProceedingJoinPoint point, Method method) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        // 设置方法参数
        Object[] args = point.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        
        return context;
    }

    private String toJsonValue(Object value) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            return String.valueOf(value);
        }
    }

    private Object parseJsonValue(String json, Method method) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Class<?> returnType = method.getReturnType();
            if (returnType.isAssignableFrom(String.class)) {
                return json;
            }
            return mapper.readValue(json, returnType);
        } catch (Exception e) {
            log.warn("JSON parse failed: {}", e.getMessage());
            return json;
        }
    }
}