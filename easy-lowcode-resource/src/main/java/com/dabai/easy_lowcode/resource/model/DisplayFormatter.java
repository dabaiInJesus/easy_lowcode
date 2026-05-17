package com.dabai.easy_lowcode.resource.model;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

public class DisplayFormatter {

    public static List<Map<String, Object>> format(
            List<Map<String, Object>> data,
            DisplaySettings settings) {

        if (data == null || data.isEmpty() || settings == null) return data;

        Map<String, DisplayFieldSetting> fieldSettings = settings.getFields();
        if (fieldSettings == null || fieldSettings.isEmpty()) return data;

        return data.stream()
                .map(row -> {
                    Map<String, Object> formatted = new LinkedHashMap<>();
                    for (Map.Entry<String, Object> entry : row.entrySet()) {
                        String fieldName = entry.getKey();
                        Object value = entry.getValue();
                        DisplayFieldSetting fs = fieldSettings.get(fieldName);
                        if (fs == null) {
                            formatted.put(fieldName, value);
                            continue;
                        }
                        if (!fs.isVisible()) continue;

                        Object displayValue = value;

                        if (fs.getFormat() != null && value != null) {
                            displayValue = formatDateValue(value, fs.getFormat());
                        }

                        if (fs.getEnumMapping() != null && value != null) {
                            String mapped = fs.getEnumMapping().get(value.toString());
                            if (mapped != null) {
                                displayValue = mapped;
                            }
                        }

                        formatted.put(fs.getLabel() != null ? fs.getLabel() : fieldName, displayValue);
                    }
                    return formatted;
                })
                .collect(Collectors.toList());
    }

    private static String formatDateValue(Object value, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            if (value instanceof java.util.Date) {
                return LocalDateTime.ofInstant(((java.util.Date) value).toInstant(), ZoneId.systemDefault())
                        .format(formatter);
            }
            if (value instanceof java.sql.Timestamp) {
                return ((java.sql.Timestamp) value).toLocalDateTime().format(formatter);
            }
            if (value instanceof java.sql.Date) {
                return ((java.sql.Date) value).toLocalDate().format(DateTimeFormatter.ofPattern(pattern));
            }
            if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).format(formatter);
            }
            if (value instanceof String) {
                return formatStringDate((String) value, pattern);
            }
        } catch (Exception ignored) {}
        return value.toString();
    }

    private static String formatStringDate(String value, String pattern) {
        String[][] candidates = {
                {"yyyy-MM-dd HH:mm:ss", "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"},
                {"yyyy-MM-dd", "\\d{4}-\\d{2}-\\d{2}"},
                {"yyyy/MM/dd HH:mm:ss", "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"},
                {"yyyy/MM/dd", "\\d{4}/\\d{2}/\\d{2}"},
        };
        for (String[] candidate : candidates) {
            if (value.matches(candidate[1])) {
                try {
                    LocalDateTime parsed = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(candidate[0]));
                    return parsed.format(DateTimeFormatter.ofPattern(pattern));
                } catch (DateTimeParseException e) {
                    try {
                        java.time.LocalDate.parse(value, DateTimeFormatter.ofPattern(candidate[0]));
                        return java.time.LocalDate.parse(value, DateTimeFormatter.ofPattern(candidate[0]))
                                .format(DateTimeFormatter.ofPattern(pattern));
                    } catch (DateTimeParseException ignored) {}
                }
            }
        }
        return value;
    }
}
