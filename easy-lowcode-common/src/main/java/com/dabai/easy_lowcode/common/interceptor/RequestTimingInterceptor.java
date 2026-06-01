package com.dabai.easy_lowcode.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 请求时间拦截器
 * 记录请求开始时间，用于计算接口执行时长
 */
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

    private static final String REQUEST_START_TIME = "request-start-time";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        long startTime = System.currentTimeMillis();
        request.setAttribute(REQUEST_START_TIME, startTime);
        return true;
    }
}