package com.dabai.easy_lowcode.collector.service.storage;

import io.minio.*;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@ConditionalOnProperty(name = "easy-lowcode.storage.type", havingValue = "minio", matchIfMissing = true)
public class MinioStorageService implements StorageService {

    @Setter
    @Value("${easy-lowcode.storage.minio.endpoint:http://localhost:9000}")
    private String endpoint;

    @Setter
    @Value("${easy-lowcode.storage.minio.access-key:minioadmin}")
    private String accessKey;

    @Setter
    @Value("${easy-lowcode.storage.minio.secret-key:minioadmin}")
    private String secretKey;

    @Setter
    @Value("${easy-lowcode.storage.minio.bucket:easy-lowcode-files}")
    private String bucket;

    private MinioClient minioClient;

    @PostConstruct
    public void init() {
        minioClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        try {
            boolean found = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
            if (!found) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("创建MinIO Bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.warn("MinIO初始化失败(可稍后重试): {}", e.getMessage());
        }
    }

    @Override
    public String upload(String fileName, InputStream content, long contentLength, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(fileName)
                    .stream(content, contentLength, -1)
                    .contentType(contentType)
                    .build());
            log.info("文件上传MinIO成功: {}", fileName);
            return fileName;
        } catch (Exception e) {
            log.error("MinIO上传失败: {}", fileName, e);
            throw new RuntimeException("文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public InputStream download(String path) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.error("MinIO下载失败: {}", path, e);
            throw new RuntimeException("文件下载失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            log.warn("MinIO删除失败: {}", path, e);
        }
    }

    @Override
    public String getFileUrl(String path) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(bucket)
                    .object(path)
                    .method(Method.GET)
                    .expiry(7, TimeUnit.DAYS)
                    .build());
        } catch (Exception e) {
            log.warn("获取MinIO文件URL失败: {}", path);
            return path;
        }
    }

    @Override
    public String getStorageType() {
        return "minio";
    }
}
