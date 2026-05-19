package com.dabai.easy_lowcode.ai.util;

import com.dabai.easy_lowcode.ai.dto.ChatResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * Spring AI ChatResponse 提取工具类
 * <p>
 * 统一从 Spring AI 的 ChatResponse 中提取内容。
 * 使用反射调用标准方法链：
 * response.getResult().getOutput().getTextContent()
 * 同时对不同 Spring AI 版本做兼容处理。
 */
@Slf4j
public final class ChatResponseUtil {

    // 缓存方法引用，避免重复反射查找
    private static Method methodGetResult;
    private static Method methodGetOutput;
    private static Method methodGetTextContent;
    private static Method methodGetContent;

    static {
        initMethods();
    }

    private static void initMethods() {
        try {
            Class<?> clazzChatResponse = org.springframework.ai.chat.model.ChatResponse.class;
            methodGetResult = clazzChatResponse.getMethod("getResult");

            Class<?> clazzResult = methodGetResult.getReturnType();
            methodGetOutput = clazzResult.getMethod("getOutput");

            Class<?> clazzOutput = methodGetOutput.getReturnType();
            // Spring AI 1.1.x: AssistantContent → getTextContent()
            try {
                methodGetTextContent = clazzOutput.getMethod("getTextContent");
            } catch (NoSuchMethodException e) {
                // Fallback: AssistantMessage implements Message → getContent()
                methodGetContent = clazzOutput.getMethod("getContent");
            }
        } catch (Exception e) {
            log.warn("初始化 ChatResponse 反射方法失败，将使用兜底方式提取内容: {}", e.getMessage());
        }
    }

    private ChatResponseUtil() {
    }

    /**
     * 从 Spring AI ChatResponse 提取文本内容
     *
     * @param response Spring AI 标准响应（非空）
     * @return 文本内容，提取失败返回空字符串
     */
    public static String extractText(org.springframework.ai.chat.model.ChatResponse response) {
        if (response == null) {
            return "";
        }
        try {
            Object result = methodGetResult.invoke(response);
            if (result == null) {
                return "";
            }
            Object output = methodGetOutput.invoke(result);
            if (output == null) {
                return "";
            }
            if (methodGetTextContent != null) {
                return (String) methodGetTextContent.invoke(output);
            } else if (methodGetContent != null) {
                return (String) methodGetContent.invoke(output);
            }
            return "";
        } catch (Exception e) {
            log.warn("从 ChatResponse 提取文本内容失败: {}", e.getMessage());
            return "";
        }
    }

    /**
     * 从 Spring AI ChatResponse 提取文本内容并封装到项目 DTO
     *
     * @param response  Spring AI 标准响应
     * @param modelName 模型名称
     * @return 项目 DTO ChatResponse
     */
    public static ChatResponse toDto(org.springframework.ai.chat.model.ChatResponse response, String modelName) {
        ChatResponse dto = new ChatResponse();
        dto.setModel(modelName);
        dto.setContent(extractText(response));
        return dto;
    }
}
