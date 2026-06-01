package com.dabai.easy_lowcode.auth.aspect;

import com.dabai.easy_lowcode.auth.entity.SysAuditLog;
import com.dabai.easy_lowcode.auth.service.SysAuditLogService;
import com.dabai.easy_lowcode.common.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * 审计日志切面
 * 自动记录Controller层的增删改操作
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditLogAspect {

    private final SysAuditLogService auditLogService;

    /**
     * 定义切点：所有 Controller 包下的增删改方法
     */
    @Pointcut("execution(* com.dabai.easy_lowcode..controller..*(..))")
    public void controllerPointcut() {
    }

    /**
     * 记录审计日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        long startTime = System.currentTimeMillis();
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
                recordAuditLog(point, startTime, error);
            } catch (Exception e) {
                log.warn("记录审计日志失败: {}", e.getMessage());
            }
        }
    }

    private void recordAuditLog(ProceedingJoinPoint point, long startTime, Exception error) {
        // 只记录 POST/PUT/DELETE 请求
        HttpServletRequest request = getCurrentRequest();
        if (request == null) return;
        
        String method = request.getMethod();
        if (!Arrays.asList("POST", "PUT", "DELETE", "PATCH").contains(method)) {
            return;
        }

        // 获取方法信息
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method methodObj = signature.getMethod();
        
        // 获取模块名（从类名推断）
        String className = point.getTarget().getClass().getSimpleName();
        String module = inferModule(className);
        
        // 获取操作名
        String action = inferAction(methodObj.getName(), method);

        // 获取当前用户
        Long userId = null;
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof LoginUser loginUser) {
            userId = loginUser.getUserId();
            username = loginUser.getUsername();
        }

        // 构建审计日志
        SysAuditLog auditLog = new SysAuditLog();
        auditLog.setUserId(userId);
        auditLog.setUsername(username);
        auditLog.setModule(module);
        auditLog.setAction(action);
        auditLog.setDescription(buildDescription(methodObj, point.getArgs()));
        auditLog.setRequestMethod(method);
        auditLog.setRequestUrl(request.getRequestURI());
        auditLog.setRequestParams(buildParams(point.getArgs()));
        auditLog.setIpAddress(getClientIp(request));
        auditLog.setUserAgent(request.getHeader("User-Agent"));
        auditLog.setOperationStatus(error != null ? "FAILURE" : "SUCCESS");
        auditLog.setErrorMessage(error != null ? truncate(error.getMessage(), 1000) : null);
        auditLog.setExecutionTime(System.currentTimeMillis() - startTime);
        auditLog.setCreateTime(LocalDateTime.now());

        // 异步记录日志
        auditLogService.recordLog(auditLog);
    }

    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private String inferModule(String className) {
        // 从类名推断模块
        if (className.contains("User")) return "用户管理";
        if (className.contains("Role")) return "角色管理";
        if (className.contains("Menu")) return "菜单管理";
        if (className.contains("Dept")) return "部门管理";
        if (className.contains("Auth")) return "授权管理";
        if (className.contains("App")) return "应用管理";
        if (className.contains("Etl")) return "ETL任务";
        if (className.contains("Dashboard")) return "数据大屏";
        if (className.contains("Ai")) return "AI助手";
        if (className.contains("DataSource")) return "数据源";
        if (className.contains("Resource")) return "资源管理";
        if (className.contains("Api")) return "API管理";
        return "系统";
    }

    private String inferAction(String methodName, String httpMethod) {
        String action = methodName;
        
        if (httpMethod.equals("POST")) {
            if (methodName.startsWith("create") || methodName.startsWith("add") || methodName.startsWith("save")) {
                action = "新增";
            } else if (methodName.startsWith("login")) {
                action = "登录";
            } else if (methodName.startsWith("execute")) {
                action = "执行";
            }
        } else if (httpMethod.equals("PUT")) {
            if (methodName.startsWith("update") || methodName.startsWith("edit")) {
                action = "更新";
            } else if (methodName.startsWith("toggle") || methodName.startsWith("enable")) {
                action = "状态变更";
            }
        } else if (httpMethod.equals("DELETE")) {
            if (methodName.startsWith("delete") || methodName.startsWith("remove")) {
                action = "删除";
            }
        }
        
        return action;
    }

    private String buildDescription(Method method, Object[] args) {
        // 简化参数描述
        if (args == null || args.length == 0) {
            return method.getName();
        }
        return method.getName() + " (参数数量: " + args.length + ")";
    }

    private String buildParams(Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        try {
            String params = Arrays.stream(args)
                .filter(a -> a != null)
                .map(a -> {
                    if (a instanceof String || a instanceof Number || a instanceof Boolean) {
                        return String.valueOf(a);
                    }
                    return a.getClass().getSimpleName();
                })
                .collect(Collectors.joining(", "));
            return truncate(params, 2000);
        } catch (Exception e) {
            return null;
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
        // 多级代理时取第一个IP
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    private String truncate(String str, int maxLength) {
        if (str == null) return null;
        if (str.length() <= maxLength) return str;
        return str.substring(0, maxLength);
    }
}