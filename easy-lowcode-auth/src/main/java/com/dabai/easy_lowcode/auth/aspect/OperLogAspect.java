package com.dabai.easy_lowcode.auth.aspect;

import com.dabai.easy_lowcode.auth.entity.SysAuditLog;
import com.dabai.easy_lowcode.auth.service.SysAuditLogService;
import com.dabai.easy_lowcode.common.annotation.OperLog;
import com.dabai.easy_lowcode.common.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 操作日志切面
 * 自动记录带有 @OperLog 注解的方法调用
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperLogAspect {

    private final SysAuditLogService auditLogService;
    
    private final ExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer nameDiscoverer = new DefaultParameterNameDiscoverer();

    @Around("@annotation(com.dabai.easy_lowcode.common.annotation.OperLog)")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        OperLog operLog = method.getAnnotation(OperLog.class);
        
        if (operLog == null) {
            return point.proceed();
        }

        Object result = null;
        Exception error = null;
        
        try {
            result = point.proceed();
            return result;
        } catch (Exception e) {
            error = e;
            throw e;
        } finally {
            try {
                recordOperLog(point, method, operLog, result, error, startTime);
            } catch (Exception e) {
                log.warn("Record oper log failed: {}", e.getMessage());
            }
        }
    }

    @Async
    protected void recordOperLog(ProceedingJoinPoint point, Method method, OperLog operLog, 
                                 Object result, Exception error, long startTime) {
        try {
            // 获取当前用户
            Long userId = null;
            String username = null;
            try {
                org.springframework.security.core.Authentication auth = 
                    org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
                    userId = loginUser.getUserId();
                    username = loginUser.getUsername();
                }
            } catch (Exception e) {
                // 忽略获取用户失败
            }

            // 获取请求信息
            HttpServletRequest request = getCurrentRequest();
            String ipAddress = request != null ? getClientIp(request) : null;
            String requestUrl = request != null ? request.getRequestURI() : null;
            String requestMethod = request != null ? request.getMethod() : null;

            // 构建描述
            String description = buildDescription(operLog.description(), point, method);

            // 构建请求参数
            String requestParams = null;
            if (operLog.logParams()) {
                requestParams = buildRequestParams(point, method, operLog.sensitiveParams());
            }

            // 构建审计日志
            SysAuditLog auditLog = new SysAuditLog();
            auditLog.setUserId(userId);
            auditLog.setUsername(username);
            auditLog.setModule(operLog.module());
            auditLog.setAction(operLog.type());
            auditLog.setDescription(description);
            auditLog.setRequestMethod(requestMethod);
            auditLog.setRequestUrl(requestUrl);
            auditLog.setRequestParams(truncate(requestParams, 2000));
            auditLog.setIpAddress(ipAddress);
            auditLog.setUserAgent(request != null ? truncate(request.getHeader("User-Agent"), 500) : null);
            auditLog.setExecutionTime(System.currentTimeMillis() - startTime);
            auditLog.setCreateTime(LocalDateTime.now());
            
            if (error != null) {
                auditLog.setOperationStatus("FAILURE");
                auditLog.setErrorMessage(truncate(error.getMessage(), 1000));
            } else {
                auditLog.setOperationStatus("SUCCESS");
            }

            // 记录日志
            auditLogService.recordLog(auditLog);
            
            log.debug("OperLog recorded: module={}, action={}, user={}", 
                operLog.module(), operLog.type(), username);
                
        } catch (Exception e) {
            log.warn("Record oper log error: {}", e.getMessage());
        }
    }

    private String buildDescription(String descriptionTemplate, ProceedingJoinPoint point, Method method) {
        if (descriptionTemplate == null || descriptionTemplate.isEmpty()) {
            return method.getName();
        }
        
        // 解析SpEL表达式
        if (descriptionTemplate.contains("#")) {
            try {
                EvaluationContext context = createEvaluationContext(point, method);
                Object value = parser.parseExpression(descriptionTemplate).getValue(context);
                return value != null ? String.valueOf(value) : descriptionTemplate;
            } catch (Exception e) {
                log.warn("SpEL parse failed for description: {}", e.getMessage());
            }
        }
        
        return descriptionTemplate;
    }

    private String buildRequestParams(ProceedingJoinPoint point, Method method, String[] sensitiveParams) {
        try {
            Object[] args = point.getArgs();
            String[] paramNames = nameDiscoverer.getParameterNames(method);
            
            Set<String> sensitiveSet = new HashSet<>(Arrays.asList(sensitiveParams));
            sensitiveSet.add("password");
            sensitiveSet.add("pwd");
            sensitiveSet.add("secret");
            sensitiveSet.add("token");
            
            StringBuilder sb = new StringBuilder();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length && i < args.length; i++) {
                    Object arg = args[i];
                    if (arg == null) continue;
                    
                    String value;
                    String paramNameLower = paramNames[i].toLowerCase();
                    boolean isSensitive = false;
                    for (String s : sensitiveSet) {
                        if (paramNameLower.contains(s.toLowerCase())) {
                            isSensitive = true;
                            break;
                        }
                    }
                    if (isSensitive) {
                        value = "******";
                    } else if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                        value = String.valueOf(arg);
                    } else {
                        // JSON序列化其他对象
                        try {
                            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                            value = mapper.writeValueAsString(arg);
                        } catch (Exception e) {
                            value = arg.getClass().getSimpleName();
                        }
                    }
                    
                    if (sb.length() > 0) sb.append(", ");
                    sb.append(paramNames[i]).append("=").append(value);
                }
            }
            
            return sb.toString();
        } catch (Exception e) {
            return "参数解析失败";
        }
    }

    private EvaluationContext createEvaluationContext(ProceedingJoinPoint point, Method method) {
        StandardEvaluationContext context = new StandardEvaluationContext();
        
        Object[] args = point.getArgs();
        String[] paramNames = nameDiscoverer.getParameterNames(method);
        
        if (paramNames != null) {
            for (int i = 0; i < paramNames.length && i < args.length; i++) {
                context.setVariable(paramNames[i], args[i]);
            }
        }
        
        return context;
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String getClientIp(HttpServletRequest request) {
        if (request == null) return null;
        
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}
