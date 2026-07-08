package com.dabai.easy_lowcode.collector.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@ConditionalOnProperty(name = "easy-lowcode.storage.type", havingValue = "local", matchIfMissing = false)
public class LocalStorageService implements StorageService {

    @Setter
    @Value("${easy-lowcode.storage.local.path:./data/files}")
    private String basePath;

    private Path storagePath;

    @PostConstruct
    public void init() {
        storagePath = Paths.get(basePath).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storagePath);
        } catch (IOException e) {
            log.warn("创建本地存储目录失败: {}", storagePath, e);
        }
    }

    @Override
    public String upload(String fileName, InputStream content, long contentLength, String contentType) {
        try {
            String dateDir = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));
            Path dirPath = storagePath.resolve(dateDir);
            Files.createDirectories(dirPath);

            String uniqueName = System.currentTimeMillis() + "_" + fileName.replaceAll("[^a-zA-Z0-9._-]", "_");
            Path filePath = dirPath.resolve(uniqueName);

            try (OutputStream os = new BufferedOutputStream(new FileOutputStream(filePath.toFile()))) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = content.read(buffer)) != -1) {
                    os.write(buffer, 0, bytesRead);
                }
            }

            String relativePath = dateDir + "/" + uniqueName;
            log.info("文件保存到本地: {}", filePath);
            return relativePath;
        } catch (IOException e) {
            throw new RuntimeException("文件存储失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return new BufferedInputStream(new FileInputStream(storagePath.resolve(path).toFile()));
        } catch (IOException e) {
            throw new RuntimeException("文件读取失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(storagePath.resolve(path));
        } catch (IOException e) {
            log.warn("删除文件失败: {}", path, e);
        }
    }

    @Override
    public String getFileUrl(String path) {
        return "/api/files/" + path;
    }

    @Override
    public String getStorageType() {
        return "local";
    }
}
