package com.dabai.easy_lowcode.common.aspect;

import com.dabai.easy_lowcode.common.annotation.RateLimit;
import com.dabai.easy_lowcode.common.exception.BusinessException;
import com.dabai.easy_lowcode.common.util.CacheUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitAspectTest {

    @Mock
    private CacheUtil cacheUtil;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private RateLimitAspect rateLimitAspect;

    private Method sampleMethod;

    @BeforeEach
    void setUp() throws NoSuchMethodException {
        sampleMethod = TestController.class.getMethod("testEndpoint");
        lenient().when(joinPoint.getSignature()).thenReturn(methodSignature);
        lenient().when(methodSignature.getMethod()).thenReturn(sampleMethod);
    }

    private void setupRequestContext() {
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getHeader("Proxy-Client-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @Test
    void withinLimit_proceedsExecution() throws Throwable {
        setupRequestContext();
        when(cacheUtil.increment(anyString())).thenReturn(5L);
        when(joinPoint.proceed()).thenReturn("ok");

        Object result = rateLimitAspect.around(joinPoint);
        assertThat(result).isEqualTo("ok");
        verify(joinPoint).proceed();
    }

    @Test
    void firstRequest_setsExpiry() throws Throwable {
        setupRequestContext();
        when(cacheUtil.increment(anyString())).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        rateLimitAspect.around(joinPoint);

        verify(cacheUtil).expire(anyString(), eq(60L), eq(TimeUnit.SECONDS));
        verify(joinPoint).proceed();
    }

    @Test
    void exceededLimit_throwsBusinessException() throws Throwable {
        setupRequestContext();
        when(cacheUtil.increment(anyString())).thenReturn(101L);

        assertThatThrownBy(() -> rateLimitAspect.around(joinPoint))
                .isInstanceOf(BusinessException.class)
                .hasMessage("请求过于频繁，请稍后再试");

        verify(joinPoint, never()).proceed();
    }

    @Test
    void exceededLimit_customMessage() throws Throwable {
        Method customMethod = TestController.class.getMethod("customLimitedEndpoint");
        when(methodSignature.getMethod()).thenReturn(customMethod);
        setupRequestContext();
        when(cacheUtil.increment(anyString())).thenReturn(6L);

        assertThatThrownBy(() -> rateLimitAspect.around(joinPoint))
                .isInstanceOf(BusinessException.class)
                .hasMessage("自定义限流消息");

        verify(joinPoint, never()).proceed();
    }

    @Test
    void redisUnavailable_proceeds() throws Throwable {
        setupRequestContext();
        when(cacheUtil.increment(anyString())).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("fallback");

        Object result = rateLimitAspect.around(joinPoint);
        assertThat(result).isEqualTo("fallback");
        verify(joinPoint).proceed();
    }

    @Test
    void noRequestContext_proceeds() throws Throwable {
        RequestContextHolder.resetRequestAttributes();
        when(joinPoint.proceed()).thenReturn("noContext");

        Object result = rateLimitAspect.around(joinPoint);
        assertThat(result).isEqualTo("noContext");
    }

    @Test
    void usesXForwardedFor_asIdentifier() throws Throwable {
        ServletRequestAttributes attrs = new ServletRequestAttributes(request);
        RequestContextHolder.setRequestAttributes(attrs);
        when(request.getHeader("X-Forwarded-For")).thenReturn("10.0.0.1, 192.168.1.1");
        when(cacheUtil.increment(anyString())).thenReturn(1L);
        when(joinPoint.proceed()).thenReturn("ok");

        rateLimitAspect.around(joinPoint);

        verify(cacheUtil).increment(contains("rate-limit"));
    }

    // Test controller with annotated methods
    static class TestController {
        @RateLimit
        public String testEndpoint() {
            return "ok";
        }

        @RateLimit(key = "custom", window = 10, maxRequests = 5, message = "自定义限流消息")
        public String customLimitedEndpoint() {
            return "ok";
        }
    }
}
