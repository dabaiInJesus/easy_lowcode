package com.dabai.easy_lowcode.common.util;

import com.dabai.easy_lowcode.common.annotation.Sensitive;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;
import java.util.regex.Pattern;

/**
 * 数据脱敏工具类
 */
@Slf4j
@Component
public class SensitiveUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^(.{1})(.*)(@.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(.{3})(.*)(.{4})$");
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("^(.{6})(.*)(.{4})$");
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("^(.{6})(.*)(.{4})$");

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 对对象中的敏感字段进行脱敏
     */
    public <T> T mask(T obj) {
        if (obj == null) return null;
        
        try {
            if (obj instanceof Map) {
                return (T) maskMap((Map<?, ?>) obj);
            }
            
            // 处理单个对象
            return maskObject(obj);
        } catch (Exception e) {
            log.warn("Sensitive mask failed: {}", e.getMessage());
            return obj;
        }
    }

    /**
     * 脱敏Map
     */
    public Map<String, Object> maskMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) {
                // 根据key名称判断脱敏类型
                String key = String.valueOf(entry.getKey());
                result.put(key, maskValueByKey(key, (String) value));
            } else if (value instanceof Map) {
                result.put(String.valueOf(entry.getKey()), maskMap((Map<?, ?>) value));
            } else if (value instanceof List) {
                result.put(String.valueOf(entry.getKey()), maskList((List<?>) value));
            } else {
                result.put(String.valueOf(entry.getKey()), value);
            }
        }
        return result;
    }

    /**
     * 脱敏列表
     */
    public List<Object> maskList(List<?> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(maskMap((Map<?, ?>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    /**
     * 脱敏单个对象（通过反射处理注解字段）
     */
    @SuppressWarnings("unchecked")
    private <T> T maskObject(T obj) throws Exception {
        // 深拷贝对象
        String json = objectMapper.writeValueAsString(obj);
        T result = (T) objectMapper.readValue(json, obj.getClass());
        
        Class<?> clazz = result.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.isAnnotationPresent(Sensitive.class)) {
                    field.setAccessible(true);
                    Sensitive sensitive = field.getAnnotation(Sensitive.class);
                    Object value = field.get(result);
                    
                    if (value instanceof String) {
                        String masked = mask((String) value, sensitive);
                        field.set(result, masked);
                    }
                }
            }
            clazz = clazz.getSuperclass();
        }
        
        return result;
    }

    /**
     * 根据key名称选择脱敏方式
     */
    private String maskValueByKey(String key, String value) {
        if (value == null || value.isEmpty()) return value;
        
        String lowerKey = key.toLowerCase();
        
        if (lowerKey.contains("phone") || lowerKey.contains("mobile")) {
            return maskPhone(value);
        } else if (lowerKey.contains("email")) {
            return maskEmail(value);
        } else if (lowerKey.contains("idcard") || lowerKey.contains("id_card") || lowerKey.contains("card")) {
            return maskIdCard(value);
        } else if (lowerKey.contains("name") || lowerKey.contains("username")) {
            return maskName(value);
        } else if (lowerKey.contains("bank")) {
            return maskBankCard(value);
        } else if (lowerKey.contains("address")) {
            return maskAddress(value);
        } else if (lowerKey.contains("password") || lowerKey.contains("pwd")) {
            return "******";
        }
        
        return value;
    }

    /**
     * 通用脱敏方法
     */
    public String mask(String value, Sensitive sensitive) {
        if (value == null || value.isEmpty()) return value;
        
        switch (sensitive.type()) {
            case PHONE:
                return maskPhone(value);
            case EMAIL:
                return maskEmail(value);
            case ID_CARD:
                return maskIdCard(value);
            case NAME:
                return maskName(value);
            case BANK_CARD:
                return maskBankCard(value);
            case ADDRESS:
                return maskAddress(value);
            case PASSWORD:
                return "******";
            case CUSTOM:
                return maskCustom(value, sensitive.customRule(), sensitive.maskChar());
            case NONE:
                return value;
            default:
                return maskDefault(value, sensitive.maskChar());
        }
    }

    /**
     * 手机号脱敏
     */
    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.replaceFirst(PHONE_PATTERN.pattern(), "$1***$3");
    }

    /**
     * 邮箱脱敏
     */
    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        return email.replaceFirst(EMAIL_PATTERN.pattern(), "$1***$3");
    }

    /**
     * 身份证脱敏
     */
    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 10) return idCard;
        return idCard.replaceFirst(ID_CARD_PATTERN.pattern(), "$1**********$3");
    }

    /**
     * 姓名脱敏
     */
    public String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return name;
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 1);
    }

    /**
     * 银行卡脱敏
     */
    public String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 10) return bankCard;
        return bankCard.replaceFirst(BANK_CARD_PATTERN.pattern(), "$1****$3");
    }

    /**
     * 地址脱敏（只显示省市区）
     */
    public String maskAddress(String address) {
        if (address == null || address.length() <= 6) return address;
        // 保留前6个字符（省市区）
        return address.substring(0, 6) + "***";
    }

    /**
     * 默认脱敏（前后各保留1位）
     */
    public String maskDefault(String value, char maskChar) {
        if (value == null || value.length() <= 2) return value;
        int visibleCount = 1;
        return value.substring(0, visibleCount) 
             + String.valueOf(maskChar).repeat(value.length() - visibleCount * 2)
             + value.substring(value.length() - visibleCount);
    }

    /**
     * 自定义脱敏规则
     * @param value 原值
     * @param rule 格式: 前缀保留:后缀保留，如 "3:4"
     * @param maskChar 脱敏字符
     */
    public String maskCustom(String value, String rule, char maskChar) {
        if (value == null || value.isEmpty()) return value;
        
        String[] parts = rule.split(":");
        if (parts.length != 2) {
            return maskDefault(value, maskChar);
        }
        
        try {
            int prefix = Integer.parseInt(parts[0].trim());
            int suffix = Integer.parseInt(parts[1].trim());
            
            if (prefix + suffix >= value.length()) {
                return maskDefault(value, maskChar);
            }
            
            String masked = value.substring(0, prefix)
                + String.valueOf(maskChar).repeat(value.length() - prefix - suffix)
                + value.substring(value.length() - suffix);
            
            return masked;
        } catch (NumberFormatException e) {
            return maskDefault(value, maskChar);
        }
    }
}