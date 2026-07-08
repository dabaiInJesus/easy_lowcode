package com.dabai.easy_lowcode.collector.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.xml.sax.ContentHandler;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@Service
public class TikaContentExtractor {

    public TikaResult extract(InputStream inputStream, String fileName) {
        try {
            Parser parser = new AutoDetectParser();
            ContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            metadata.set(Metadata.RESOURCE_NAME_KEY, fileName);
            ParseContext context = new ParseContext();

            parser.parse(inputStream, handler, metadata, context);

            String content = handler.toString();
            Map<String, String> meta = new LinkedHashMap<>();
            for (String name : metadata.names()) {
                meta.put(name, metadata.get(name));
            }

            String detectedType = metadata.get(Metadata.CONTENT_TYPE);

            return new TikaResult(content, meta, detectedType != null ? detectedType : "application/octet-stream");
        } catch (TikaException e) {
            log.warn("Tika解析失败(可能是不支持的文件格式): {} - {}", fileName, e.getMessage());
            return new TikaResult("", Map.of("error", e.getMessage()), "application/octet-stream");
        } catch (Exception e) {
            log.error("Tika解析异常: {}", fileName, e);
            return new TikaResult("", Map.of("error", e.getMessage()), "application/octet-stream");
        }
    }

    @Data
    public static class TikaResult {
        private final String contentText;
        private final Map<String, String> metadata;
        private final String detectedType;
    }
}
