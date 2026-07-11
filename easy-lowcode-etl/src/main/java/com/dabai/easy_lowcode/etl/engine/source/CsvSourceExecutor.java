package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * CSV 文件源节点执行器
 */
@Slf4j
@Component
public class CsvSourceExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "csv"; }

    @Override
    public String getCategory() { return "SOURCE"; }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        String filePath = (String) config.get("filePath");
        String delimiter = (String) config.getOrDefault("delimiter", ",");
        String encoding = (String) config.getOrDefault("encoding", "UTF-8");
        Boolean hasHeader = (Boolean) config.getOrDefault("hasHeader", true);

        log.info("CSV Source: file={}, delimiter={}, encoding={}", filePath, delimiter, encoding);

        return new CsvReaderWrapper(filePath, delimiter, Charset.forName(encoding), hasHeader);
    }

    static class CsvReaderWrapper implements ItemReader<Map<String, Object>> {
        private final BufferedReader reader;
        private final String delimiter;
        private String[] headers;
        private String nextLine;
        private boolean headerRead = false;

        CsvReaderWrapper(String filePath, String delimiter, Charset charset, boolean hasHeader) {
            try {
                this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), charset));
                this.delimiter = delimiter;
                if (hasHeader) {
                    this.headers = parseLine(reader.readLine());
                    this.headerRead = true;
                }
                this.nextLine = reader.readLine();
            } catch (Exception e) {
                throw new RuntimeException("CSV 文件打开失败: " + e.getMessage(), e);
            }
        }

        CsvReaderWrapper(InputStream is, String delimiter) {
            this.reader = new BufferedReader(new InputStreamReader(is));
            this.delimiter = delimiter;
            try {
                this.headers = parseLine(reader.readLine());
                this.nextLine = reader.readLine();
            } catch (Exception e) {
                throw new RuntimeException("CSV 读取失败: " + e.getMessage(), e);
            }
        }

        @Override
        public Map<String, Object> read() {
            try {
                if (nextLine == null) return null;
                String[] values = parseLine(nextLine);
                nextLine = reader.readLine();

                Map<String, Object> row = new LinkedHashMap<>();
                if (headers != null) {
                    for (int i = 0; i < headers.length && i < values.length; i++) {
                        row.put(headers[i], values[i]);
                    }
                } else {
                    for (int i = 0; i < values.length; i++) {
                        row.put("col_" + (i + 1), values[i]);
                    }
                }
                return row;
            } catch (Exception e) {
                throw new RuntimeException("CSV 读取失败: " + e.getMessage(), e);
            }
        }

        private String[] parseLine(String line) {
            if (line == null) return new String[0];
            // 简单的 CSV 解析（不处理引号内逗号）
            return line.split(delimiter, -1);
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "filePath": { "type": "string", "label": "文件路径" },
                  "delimiter": { "type": "string", "label": "分隔符", "default": "," },
                  "encoding": { "type": "select", "label": "编码", "options": ["UTF-8", "GBK", "ISO-8859-1"], "default": "UTF-8" },
                  "hasHeader": { "type": "boolean", "label": "首行为表头", "default": true }
                }""";
    }
}
