package com.dabai.easy_lowcode.resource.model;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConfigParser {

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static ConfigJson parse(String configJsonStr) {
        if (configJsonStr == null || configJsonStr.isBlank()) {
            return new ConfigJson();
        }
        try {
            return MAPPER.readValue(configJsonStr, ConfigJson.class);
        } catch (Exception e) {
            log.warn("解析 configJson 失败: {}", e.getMessage());
            return new ConfigJson();
        }
    }

    public static String toJson(ConfigJson config) {
        try {
            return MAPPER.writeValueAsString(config);
        } catch (Exception e) {
            log.error("序列化 configJson 失败", e);
            return "{}";
        }
    }
}
