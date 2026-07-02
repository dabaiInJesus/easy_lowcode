package com.dabai.easy_lowcode.ai.controller;

import com.dabai.easy_lowcode.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.stream.Collectors;

/**
 * AI 模块全局异常处理器
 */
@Slf4j
@RestControllerAdvice(basePackages = "com.dabai.easy_lowcode.ai.controller")
public class AiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Result<Void> handleIllegalArgument(IllegalArgumentException e) {
        log.warn("参数错误: {}", e.getMessage());
        return Result.error(HttpStatus.BAD_REQUEST.value(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleValidation(MethodArgumentNotValidException e) {
        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        log.warn("参数校验失败: {}", detail);
        return Result.error(HttpStatus.BAD_REQUEST.value(), "参数校验失败: " + detail);
    }

    @ExceptionHandler(IllegalStateException.class)
    public Result<Void> handleIllegalState(IllegalStateException e) {
        log.warn("状态错误: {}", e.getMessage());
        return Result.error(HttpStatus.SERVICE_UNAVAILABLE.value(), e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public Result<Void> handleRuntimeException(RuntimeException e) {
        log.error("运行时异常: {}", e.getMessage(), e);
        // 隐藏内部实现细节
        String message = e.getMessage();
        if (message != null && message.contains("：")) {
            return Result.error(message);
        }
        return Result.error("AI 服务调用失败，请稍后重试");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Result<Void> handleGenericException(Exception e) {
        log.error("未知异常", e);
        return Result.error("服务内部错误，请联系管理员");
    }
}
