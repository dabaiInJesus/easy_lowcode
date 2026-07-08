package com.dabai.easy_lowcode.collector.service.storage;

import jakarta.annotation.PostConstruct;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.net.URI;

@Slf4j
@Service
@ConditionalOnProperty(name = "easy-lowcode.storage.type", havingValue = "s3")
public class S3StorageService implements StorageService {

    @Setter
    @Value("${easy-lowcode.storage.s3.endpoint:}")
    private String endpoint;

    @Setter
    @Value("${easy-lowcode.storage.s3.region:us-east-1}")
    private String region;

    @Setter
    @Value("${easy-lowcode.storage.s3.access-key:}")
    private String accessKey;

    @Setter
    @Value("${easy-lowcode.storage.s3.secret-key:}")
    private String secretKey;

    @Setter
    @Value("${easy-lowcode.storage.s3.bucket:easy-lowcode-files}")
    private String bucket;

    private S3Client s3Client;

    @PostConstruct
    public void init() {
        var builder = S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)));

        if (endpoint != null && !endpoint.isEmpty()) {
            builder.endpointOverride(URI.create(endpoint));
        }

        s3Client = builder.build();

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
        } catch (NoSuchBucketException e) {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("创建S3 Bucket: {}", bucket);
        } catch (Exception e) {
            log.warn("S3初始化失败(可稍后重试): {}", e.getMessage());
        }
    }

    @Override
    public String upload(String fileName, InputStream content, long contentLength, String contentType) {
        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket)
                        .key(fileName)
                        .contentType(contentType)
                        .build(),
                RequestBody.fromInputStream(content, contentLength));
        log.info("文件上传S3成功: {}", fileName);
        return fileName;
    }

    @Override
    public InputStream download(String path) {
        var response = s3Client.getObject(GetObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build());
        return response;
    }

    @Override
    public void delete(String path) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(path)
                .build());
    }

    @Override
    public String getFileUrl(String path) {
        return endpoint + "/" + bucket + "/" + path;
    }

    @Override
    public String getStorageType() {
        return "s3";
    }
}
