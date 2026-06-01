package com.dabai.easy_lowcode.common.interceptor;

import com.dabai.easy_lowcode.common.result.Result;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * API响应日志拦截器
 * 记录所有API请求的响应信息
 */
@Slf4j
@Component
public class ApiResponseInterceptor implements HandlerInterceptor {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, 
                                 Object handler, Exception ex) throws Exception {
        // 异步记录日志，不影响响应速度
        try {
            recordApiLog(request, response, ex);
        } catch (Exception e) {
            // 日志记录失败不影响业务
        }
    }

    private void recordApiLog(HttpServletRequest request, HttpServletResponse response, Exception ex) {
        Map<String, Object> logInfo = new LinkedHashMap<>();
        
        // 请求时间
        logInfo.put("timestamp", LocalDateTime.now().format(FORMATTER));
        
        // 请求信息
        logInfo.put("method", request.getMethod());
        logInfo.put("uri", request.getRequestURI());
        logInfo.put("query", request.getQueryString());
        
        // 响应信息
        logInfo.put("status", response.getStatus());
        
        // 客户端信息
        logInfo.put("ip", getClientIp(request));
        logInfo.put("userAgent", truncate(request.getHeader("User-Agent"), 200));
        
        // 执行时间
        Long startTime = (Long) request.getAttribute("request-start-time");
        if (startTime != null) {
            logInfo.put("duration", System.currentTimeMillis() - startTime + "ms");
        }
        
        // 错误信息
        if (ex != null) {
            logInfo.put("error", truncate(ex.getMessage(), 500));
        }
        
        // 根据状态码决定日志级别
        int status = response.getStatus();
        String logMessage = toJsonString(logInfo);
        
        if (status >= 500) {
            log.error("API Error: {}", logMessage);
        } else if (status >= 400) {
            log.warn("API Warning: {}", logMessage);
        } else if (log.isDebugEnabled()) {
            log.debug("API Request: {}", logMessage);
        }
    }

    private String toJsonString(Map<String, Object> map) {
        try {
            return new ObjectMapper().writeValueAsString(map);
        } catch (Exception e) {
            return map.toString();
        }
    }

    private String getClientIp(HttpServletRequest request) {
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

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        return str.length() <= maxLength ? str : str.substring(0, maxLength);
    }
}