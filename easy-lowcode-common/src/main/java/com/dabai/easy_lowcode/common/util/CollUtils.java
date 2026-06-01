package com.dabai.easy_lowcode.common.util;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 集合工具类
 */
@Slf4j
public class CollUtils {

    private CollUtils() {}

    // ==================== 判空 ====================

    /**
     * 判断集合是否为空
     */
    public static boolean isEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * 判断集合是否不为空
     */
    public static boolean isNotEmpty(Collection<?> coll) {
        return !isEmpty(coll);
    }

    /**
     * 判断Map是否为空
     */
    public static boolean isEmpty(Map<?, ?> map) {
        return map == null || map.isEmpty();
    }

    /**
     * 判断Map是否不为空
     */
    public static boolean isNotEmpty(Map<?, ?> map) {
        return !isEmpty(map);
    }

    // ==================== 转换 ====================

    /**
     * 将集合转换为逗号分隔的字符串
     */
    public static String join(Collection<?> coll) {
        return join(coll, ",");
    }

    /**
     * 将集合转换为指定分隔符的字符串
     */
    public static String join(Collection<?> coll, String separator) {
        if (isEmpty(coll)) {
            return "";
        }
        return coll.stream()
                .map(Object::toString)
                .collect(Collectors.joining(separator));
    }

    /**
     * 字符串转集合
     */
    public static List<String> split(String str, String separator) {
        if (StrUtil.isBlank(str)) {
            return new ArrayList<>();
        }
        return Arrays.stream(str.split(separator))
                .map(String::trim)
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    /**
     * 集合转逗号分隔字符串
     */
    public static String toCommaString(Collection<?> coll) {
        return join(coll, ",");
    }

    // ==================== 安全操作 ====================

    /**
     * 获取集合第一个元素
     */
    public static <T> T getFirst(List<T> list) {
        return isEmpty(list) ? null : list.get(0);
    }

    /**
     * 获取集合最后一个元素
     */
    public static <T> T getLast(List<T> list) {
        return isEmpty(list) ? null : list.get(list.size() - 1);
    }

    /**
     * 安全获取元素，索引越界返回null
     */
    public static <T> T getOrNull(List<T> list, int index) {
        if (isEmpty(list) || index < 0 || index >= list.size()) {
            return null;
        }
        return list.get(index);
    }

    /**
     * 安全获取Map值
     */
    public static <K, V> V getOrDefault(Map<K, V> map, K key, V defaultValue) {
        if (isEmpty(map) || key == null) {
            return defaultValue;
        }
        return map.getOrDefault(key, defaultValue);
    }

    // ==================== 去重与合并 ====================

    /**
     * 集合去重
     */
    public static <T> List<T> distinct(List<T> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(list));
    }

    /**
     * 根据key去重
     */
    public static <T> List<T> distinctByKey(List<T> list, Function<T, ?> keyExtractor) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(
                list.stream()
                        .collect(Collectors.toMap(
                                keyExtractor,
                                Function.identity(),
                                (v1, v2) -> v1,
                                LinkedHashMap::new
                        ))
                        .values()
        );
    }

    /**
     * 集合交集
     */
    @SafeVarargs
    public static <T> List<T> intersection(Collection<T>... colls) {
        if (colls == null || colls.length == 0) {
            return new ArrayList<>();
        }

        Set<T> result = new HashSet<>(colls[0]);
        for (int i = 1; i < colls.length; i++) {
            if (isNotEmpty(colls[i])) {
                result.retainAll(new HashSet<>(colls[i]));
            } else {
                return new ArrayList<>();
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 集合并集
     */
    @SafeVarargs
    public static <T> List<T> union(Collection<T>... colls) {
        if (colls == null || colls.length == 0) {
            return new ArrayList<>();
        }

        Set<T> result = new HashSet<>();
        for (Collection<T> coll : colls) {
            if (isNotEmpty(coll)) {
                result.addAll(coll);
            }
        }
        return new ArrayList<>(result);
    }

    /**
     * 集合差集（A - B）
     */
    public static <T> List<T> difference(Collection<T> a, Collection<T> b) {
        if (isEmpty(a)) {
            return new ArrayList<>();
        }
        if (isEmpty(b)) {
            return new ArrayList<>(a);
        }

        Set<T> setB = new HashSet<>(b);
        return a.stream()
                .filter(item -> !setB.contains(item))
                .collect(Collectors.toList());
    }

    // ==================== 分组 ====================

    /**
     * 按key分组
     */
    public static <K, V> Map<K, List<V>> groupBy(Collection<V> coll, Function<V, K> keyExtractor) {
        if (isEmpty(coll)) {
            return new HashMap<>();
        }
        return coll.stream().collect(Collectors.groupingBy(keyExtractor));
    }

    /**
     * 按key分组，转为Map
     */
    public static <K, V> Map<K, V> toMap(Collection<V> coll, Function<V, K> keyExtractor) {
        if (isEmpty(coll)) {
            return new HashMap<>();
        }
        return coll.stream().collect(Collectors.toMap(
                keyExtractor,
                Function.identity(),
                (v1, v2) -> v1
        ));
    }

    /**
     * 按key分组，转为Map（保留重复key）
     */
    public static <K, V> Map<K, V> toMapKeepLast(Collection<V> coll, Function<V, K> keyExtractor) {
        if (isEmpty(coll)) {
            return new HashMap<>();
        }
        return coll.stream().collect(Collectors.toMap(
                keyExtractor,
                Function.identity(),
                (v1, v2) -> v2
        ));
    }

    // ==================== 过滤 ====================

    /**
     * 过滤集合
     */
    public static <T> List<T> filter(Collection<T> coll, java.util.function.Predicate<T> predicate) {
        if (isEmpty(coll) || predicate == null) {
            return new ArrayList<>();
        }
        return coll.stream().filter(predicate).collect(Collectors.toList());
    }

    /**
     * 过滤null元素
     */
    public static <T> List<T> filterNull(List<T> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 过滤空字符串
     */
    public static List<String> filterBlank(List<String> list) {
        if (isEmpty(list)) {
            return new ArrayList<>();
        }
        return list.stream()
                .filter(StrUtil::isNotBlank)
                .collect(Collectors.toList());
    }

    // ==================== 映射 ====================

    /**
     * 提取集合中的某个属性
     */
    public static <T, R> List<R> map(Collection<T> coll, Function<T, R> mapper) {
        if (isEmpty(coll) || mapper == null) {
            return new ArrayList<>();
        }
        return coll.stream().map(mapper).collect(Collectors.toList());
    }

    /**
     * 提取集合中的某个属性（去重）
     */
    public static <T, R> List<R> mapDistinct(Collection<T> coll, Function<T, R> mapper) {
        if (isEmpty(coll) || mapper == null) {
            return new ArrayList<>();
        }
        return coll.stream()
                .map(mapper)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * 提取为逗号分隔的字符串
     */
    public static <T> String mapToString(Collection<T> coll, Function<T, ?> mapper) {
        if (isEmpty(coll) || mapper == null) {
            return "";
        }
        return coll.stream()
                .map(mapper)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    // ==================== 其他 ====================

    /**
     * 集合判所有元素满足条件
     */
    public static <T> boolean allMatch(Collection<T> coll, java.util.function.Predicate<T> predicate) {
        if (isEmpty(coll)) {
            return true;
        }
        return coll.stream().allMatch(predicate);
    }

    /**
     * 集合判任意元素满足条件
     */
    public static <T> boolean anyMatch(Collection<T> coll, java.util.function.Predicate<T> predicate) {
        if (isEmpty(coll)) {
            return false;
        }
        return coll.stream().anyMatch(predicate);
    }

    /**
     * 集合判没有元素满足条件
     */
    public static <T> boolean noneMatch(Collection<T> coll, java.util.function.Predicate<T> predicate) {
        if (isEmpty(coll)) {
            return true;
        }
        return coll.stream().noneMatch(predicate);
    }

    /**
     * 分页
     */
    public static <T> List<T> page(List<T> list, int page, int pageSize) {
        if (isEmpty(list) || page < 1 || pageSize < 1) {
            return new ArrayList<>();
        }
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, list.size());
        if (start >= list.size()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(list.subList(start, end));
    }
}
