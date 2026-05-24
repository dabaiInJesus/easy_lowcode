package com.dabai.easy_lowcode.common.result;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Result 统一返回封装测试
 */
class ResultTest {

    @Test
    void testSuccess() {
        Result<Void> result = Result.success();
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertNull(result.getData());
        assertNotNull(result.getTimestamp());
    }

    @Test
    void testSuccessWithData() {
        String data = "test";
        Result<String> result = Result.success(data);
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMessage());
        assertEquals("test", result.getData());
    }

    @Test
    void testSuccessWithCustomMessage() {
        Result<String> result = Result.success("自定义消息", "data");
        assertNotNull(result);
        assertEquals(200, result.getCode());
        assertEquals("自定义消息", result.getMessage());
        assertEquals("data", result.getData());
    }

    @Test
    void testError() {
        Result<Void> result = Result.error("操作失败");
        assertNotNull(result);
        assertEquals(500, result.getCode());
        assertEquals("操作失败", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithCode() {
        Result<Void> result = Result.error(400, "参数错误");
        assertNotNull(result);
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testTimestampIsSet() {
        Result<Void> result = Result.success();
        assertNotNull(result.getTimestamp());
        assertTrue(result.getTimestamp() > 0);
    }
}
