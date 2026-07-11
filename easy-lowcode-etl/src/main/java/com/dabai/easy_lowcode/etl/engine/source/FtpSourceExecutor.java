package com.dabai.easy_lowcode.etl.engine.source;

import com.dabai.easy_lowcode.etl.engine.NodeExecutor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.*;

/**
 * FTP 数据源节点执行器
 */
@Slf4j
@Component
public class FtpSourceExecutor implements NodeExecutor {

    @Override
    public String getNodeType() { return "ftp"; }

    @Override
    public String getCategory() { return "SOURCE"; }

    @Override
    public ItemReader<Map<String, Object>> createReader(Map<String, Object> config) {
        String host = (String) config.get("host");
        int port = config.containsKey("port") ? ((Number) config.get("port")).intValue() : 21;
        String username = (String) config.get("username");
        String password = (String) config.get("password");
        String filePath = (String) config.get("filePath");
        String fileType = (String) config.getOrDefault("fileType", "csv");
        String delimiter = (String) config.getOrDefault("delimiter", ",");

        log.info("FTP Source: host={}, port={}, file={}", host, port, filePath);

        try {
            FTPClient ftpClient = new FTPClient();
            ftpClient.connect(host, port);
            ftpClient.login(username, password);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);

            InputStream is = ftpClient.retrieveFileStream(filePath);
            if (is == null) {
                throw new RuntimeException("FTP 文件不存在: " + filePath);
            }

            // 根据文件类型选择读取方式
            if ("csv".equalsIgnoreCase(fileType)) {
                return new CsvSourceExecutor.CsvReaderWrapper(is, delimiter);
            } else if ("json".equalsIgnoreCase(fileType)) {
                // 读取全部内容后解析
                byte[] bytes = is.readAllBytes();
                is.close();
                ftpClient.logout();
                ftpClient.disconnect();
                String content = new String(bytes);
                List<Map<String, Object>> list = new ArrayList<>();
                if (content.trim().startsWith("[")) {
                    var arr = com.alibaba.fastjson.JSON.parseArray(content);
                    for (int i = 0; i < arr.size(); i++) {
                        list.add(arr.getJSONObject(i));
                    }
                }
                return new JsonSourceExecutor.IteratorReaderWrapper(list.iterator());
            }

            throw new RuntimeException("不支持的 FTP 文件类型: " + fileType);
        } catch (Exception e) {
            throw new RuntimeException("FTP 读取失败: " + e.getMessage(), e);
        }
    }

    @Override
    public String getConfigSchema() {
        return """
                {
                  "host": { "type": "string", "label": "FTP地址" },
                  "port": { "type": "number", "label": "端口", "default": 21 },
                  "username": { "type": "string", "label": "用户名" },
                  "password": { "type": "string", "label": "密码", "inputType": "password" },
                  "filePath": { "type": "string", "label": "文件路径" },
                  "fileType": { "type": "select", "label": "文件类型", "options": ["csv", "json"], "default": "csv" },
                  "delimiter": { "type": "string", "label": "CSV分隔符", "default": "," }
                }""";
    }
}
