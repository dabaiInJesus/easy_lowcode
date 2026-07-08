package com.dabai.easy_lowcode.collector.service.storage;

import java.io.InputStream;

public interface StorageService {

    String upload(String fileName, InputStream content, long contentLength, String contentType);

    InputStream download(String path);

    void delete(String path);

    String getFileUrl(String path);

    String getStorageType();
}
