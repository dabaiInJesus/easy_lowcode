package com.dabai.easy_lowcode.etl.engine.target;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * CSV 文件目标节点执行器
 */
@Slf4j
@Component
public class CsvTargetExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "csv"; }

    @Override
    public String getCategory() { return "TARGET"; }

    @Override
    public ItemWriter<Map<String, Object>> createWriter(Map<String, Object> config) {
        String filePath = (String) config.get("filePath");
        String delimiter = (String) config.getOrDefault("delimiter", ",");
        Boolean writeHeader = (Boolean) config.getOrDefault("writeHeader", true);

        log.info("CSV Target: file={}, delimiter={}", filePath, delimiter);

        return chunk -> {
            List<Map<String, Object>> items = new ArrayList<>(chunk.getItems());
            if (items.isEmpty()) return;

            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(new FileOutputStream(filePath, true), StandardCharsets.UTF_8))) {

                boolean isFirstBatch = new File(filePath).length() == 0;

                for (Map<String, Object> row : items) {
                    if (isFirstBatch && writeHeader) {
                        writer.write(String.join(delimiter, row.keySet()));
                        writer.newLine();
                        isFirstBatch = false;
                    }
                    List<String> values = new ArrayList<>();
                    for (Object v : row.values()) {
                        values.add(v != null ? v.toString().replace("\"", "\"\"") : "");
                    }
                    writer.write(String.join(delimiter, values));
                    writer.newLine();
                }
                log.info("CSV Target 写入 {} 条记录到 {}", items.size(), filePath);
            }
        };
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "filePath": { "type": "string", "label": "输出文件路径" },
                  "delimiter": { "type": "string", "label": "分隔符", "default": "," },
                  "writeHeader": { "type": "boolean", "label": "写入表头", "default": true }
                }""";
    }
}
