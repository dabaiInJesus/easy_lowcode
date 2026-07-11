package com.dabai.easy_lowcode.etl.engine.source;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * JSON 文件源节点执行器
 */
@Slf4j
@Component
public class JsonSourceExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "json"; }

    @Override
    public String getCategory() { return "SOURCE"; }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        String filePath = (String) config.get("filePath");
        String rootPath = (String) config.getOrDefault("rootPath", "$");

        log.info("JSON Source: file={}, rootPath={}", filePath, rootPath);

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filePath), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String content = sb.toString();

            // 支持 JSON 数组或 JSON Lines
            JSONArray arr;
            if (content.trim().startsWith("[")) {
                arr = JSON.parseArray(content);
            } else {
                // JSON Lines：每行一个 JSON 对象
                arr = new JSONArray();
                for (String l : content.split("\n")) {
                    if (!l.isBlank()) {
                        arr.add(JSON.parseObject(l));
                    }
                }
            }

            List<Map<String, Object>> list = new ArrayList<>();
            for (int i = 0; i < arr.size(); i++) {
                list.add(arr.getJSONObject(i));
            }
            return new IteratorReaderWrapper(list.iterator());
        } catch (Exception e) {
            throw new RuntimeException("JSON 文件读取失败: " + e.getMessage(), e);
        }
    }

    static class IteratorReaderWrapper implements ItemReader<Map<String, Object>> {
        private final Iterator<Map<String, Object>> iterator;
        IteratorReaderWrapper(Iterator<Map<String, Object>> it) { this.iterator = it; }
        @Override public Map<String, Object> read() { return iterator.hasNext() ? iterator.next() : null; }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "filePath": { "type": "string", "label": "文件路径" },
                  "rootPath": { "type": "string", "label": "根路径", "default": "$" }
                }""";
    }
}
