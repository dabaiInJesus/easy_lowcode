package com.dabai.easy_lowcode.common.util;

import cn.hutool.json.JSONUtil;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * JSON 工具类
 */
public class JsonUtil {
    
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 对象转 JSON 字符串
     */
    public static String toJson(Object obj) {
        return JSONUtil.toJsonStr(obj);
    }
    
    /**
     * JSON 字符串转对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return JSONUtil.toBean(json, clazz);
    }
    
    /**
     * 格式化当前时间
     */
    public static String formatNow() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
