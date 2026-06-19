package com.dabai.easy_lowcode.common.exception;

import com.dabai.easy_lowcode.common.result.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleBusinessException_returnsCorrectCodeAndMessage() {
        BusinessException e = new BusinessException(400, "用户不存在");
        Result<Void> result = handler.handleBusinessException(e);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("用户不存在");
        assertThat(result.getData()).isNull();
    }

    @Test
    void handleBusinessException_defaultCodeIs500() {
        BusinessException e = new BusinessException("操作失败");
        Result<Void> result = handler.handleBusinessException(e);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("操作失败");
    }

    @Test
    void handleMethodArgumentNotValid_returns400WithFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError1 = new FieldError("obj", "name", "名称不能为空");
        FieldError fieldError2 = new FieldError("obj", "email", "邮箱格式不正确");
        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError1, fieldError2));

        MethodArgumentNotValidException e = new MethodArgumentNotValidException(null, bindingResult);
        Result<Void> result = handler.handleMethodArgumentNotValid(e);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("name: 名称不能为空");
        assertThat(result.getMessage()).contains("email: 邮箱格式不正确");
    }

    @Test
    void handleMethodArgumentNotValid_noFieldErrors_returnsDefaultMessage() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of());

        MethodArgumentNotValidException e = new MethodArgumentNotValidException(null, bindingResult);
        Result<Void> result = handler.handleMethodArgumentNotValid(e);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).isEqualTo("参数校验失败");
    }

    @Test
    void handleBindException_returns400WithFieldErrors() {
        org.springframework.validation.BindException bindException =
                new org.springframework.validation.BindException(new Object(), "obj");
        bindException.addError(new FieldError("obj", "username", "用户名不能为空"));

        Result<Void> result = handler.handleBindException(bindException);

        assertThat(result.getCode()).isEqualTo(400);
        assertThat(result.getMessage()).contains("username: 用户名不能为空");
    }

    @Test
    void handleException_returns500WithGenericMessage() {
        RuntimeException e = new RuntimeException("something went wrong");
        Result<Void> result = handler.handleException(e);

        assertThat(result.getCode()).isEqualTo(500);
        assertThat(result.getMessage()).isEqualTo("系统异常，请联系管理员");
    }
}
