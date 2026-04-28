package com.dabai.easy_lowcode.common.util;

import cn.hutool.core.util.StrUtil;

/**
 * 字符串工具类
 */
public class StringUtil {
    
    /**
     * 判断字符串是否为空
     */
    public static boolean isEmpty(String str) {
        return StrUtil.isEmpty(str);
    }
    
    /**
     * 判断字符串是否不为空
     */
    public static boolean isNotEmpty(String str) {
        return StrUtil.isNotEmpty(str);
    }
    
    /**
     * 驼峰转下划线
     */
    public static String camelToUnderline(String camelCase) {
        if (isEmpty(camelCase)) {
            return camelCase;
        }
        return StrUtil.toUnderlineCase(camelCase);
    }
    
    /**
     * 下划线转驼峰
     */
    public static String underlineToCamel(String underlineCase) {
        if (isEmpty(underlineCase)) {
            return underlineCase;
        }
        return StrUtil.toCamelCase(underlineCase);
    }
}
