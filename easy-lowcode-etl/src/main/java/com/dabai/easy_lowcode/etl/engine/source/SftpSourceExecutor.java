package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * SFTP 数据源节点执行器
 */
@Slf4j
@Component
public class SftpSourceExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "sftp"; }

    @Override
    public String getCategory() { return "SOURCE"; }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        String host = (String) config.get("host");
        int port = config.containsKey("port") ? ((Number) config.get("port")).intValue() : 22;
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        String filePath = (String) config.get("filePath");
        String fileType = (String) config.getOrDefault("fileType", "csv");
        String delimiter = (String) config.getOrDefault("delimiter", ",");

        log.info("SFTP Source: host={}, port={}, file={}", host, port, filePath);

        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            session.setPassword(password);
            session.setConfig("StrictHostKeyChecking", "no");
            session.connect(30000);

            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect();

            InputStream is = channel.get(filePath);
            if (is == null) {
                throw new RuntimeException("SFTP 文件不存在: " + filePath);
            }

            ItemReader<Map<String, Object>> reader;
            if ("csv".equalsIgnoreCase(fileType)) {
                reader = new CsvSourceExecutor.CsvReaderWrapper(is, delimiter);
            } else {
                byte[] bytes = is.readAllBytes();
                is.close();
                channel.disconnect();
                session.disconnect();
                String content = new String(bytes);
                List<Map<String, Object>> list = new ArrayList<>();
                if (content.trim().startsWith("[")) {
                    var arr = com.alibaba.fastjson.JSON.parseArray(content);
                    for (int i = 0; i < arr.size(); i++) list.add(arr.getJSONObject(i));
                }
                return new JsonSourceExecutor.IteratorReaderWrapper(list.iterator());
            }
            return reader;
        } catch (Exception e) {
            throw new RuntimeException("SFTP 读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "host": { "type": "string", "label": "SFTP地址" },
                  "port": { "type": "number", "label": "端口", "default": 22 },
                  "username": { "type": "string", "label": "用户名" },
                  "password": { "type": "string", "label": "密码", "inputType": "password" },
                  "filePath": { "type": "string", "label": "文件路径" },
                  "fileType": { "type": "select", "label": "文件类型", "options": ["csv", "json"], "default": "csv" },
                  "delimiter": { "type": "string", "label": "CSV分隔符", "default": "," }
                }""";
    }
}
