package com.dabai.easy_lowcode.resource.processor.result.builtin;

import com.dabai.easy_lowcode.resource.processor.ConfigurableProcessor;
import com.dabai.easy_lowcode.resource.processor.ProcessorContext;
import com.dabai.easy_lowcode.resource.processor.result.ResultProcessor;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public class DateFormatProcessor implements ResultProcessor, ConfigurableProcessor {

    private String defaultFormat;
    private Map<String, String> fieldFormats = new LinkedHashMap<>();

    @Override
    public String getType() { return "dateFormat"; }

    @Override
    public int getOrder() { return 50; }

    @Override
    @SuppressWarnings("unchecked")
    public void configure(Map<String, Object> config) {
        if (config != null) {
            if (config.get("defaultFormat") != null) this.defaultFormat = (String) config.get("defaultFormat");
            if (config.get("fieldFormats") instanceof Map) {
                this.fieldFormats = (Map<String, String>) config.get("fieldFormats");
            }
        }
    }

    @Override
    public List<Map<String, Object>> process(List<Map<String, Object>> input, ProcessorContext context) {
        for (Map<String, Object> row : input) {
            for (Map.Entry<String, String> entry : fieldFormats.entrySet()) {
                String field = entry.getKey();
                String format = entry.getValue();
                Object val = row.get(field);
                if (val != null) {
                    String formatted = tryFormat(val, format);
                    if (formatted != null) {
                        row.put(field, formatted);
                    }
                }
            }
        }
        return input;
    }

    private String tryFormat(Object value, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        try {
            if (value instanceof java.util.Date) {
                java.util.Date date = (java.util.Date) value;
                LocalDateTime ldt = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
                return ldt.format(formatter);
            }
            if (value instanceof java.sql.Timestamp) {
                java.sql.Timestamp ts = (java.sql.Timestamp) value;
                return ts.toLocalDateTime().format(formatter);
            }
            if (value instanceof java.sql.Date) {
                java.sql.Date d = (java.sql.Date) value;
                return d.toLocalDate().format(formatter);
            }
            if (value instanceof LocalDateTime) {
                return ((LocalDateTime) value).format(formatter);
            }
            if (value instanceof String) {
                return formatStringDate((String) value, pattern);
            }
        } catch (Exception e) {
            return value.toString();
        }
        return value.toString();
    }

    private String formatStringDate(String value, String pattern) {
        for (String[] candidate : new String[][]{
                {"yyyy-MM-dd HH:mm:ss", "\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"},
                {"yyyy-MM-dd", "\\d{4}-\\d{2}-\\d{2}"},
                {"yyyy/MM/dd HH:mm:ss", "\\d{4}/\\d{2}/\\d{2} \\d{2}:\\d{2}:\\d{2}"},
                {"yyyy/MM/dd", "\\d{4}/\\d{2}/\\d{2}"},
                {"yyyyMMddHHmmss", "\\d{14}"},
        }) {
            if (value.matches(candidate[1])) {
                try {
                    LocalDateTime parsed = LocalDateTime.parse(value, DateTimeFormatter.ofPattern(candidate[0]));
                    return parsed.format(DateTimeFormatter.ofPattern(pattern));
                } catch (DateTimeParseException e) {
                    try {
                        java.time.LocalDate parsed = java.time.LocalDate.parse(value, DateTimeFormatter.ofPattern(candidate[0]));
                        return parsed.format(DateTimeFormatter.ofPattern(pattern));
                    } catch (DateTimeParseException ignored) {}
                }
            }
        }
        return value;
    }
}
