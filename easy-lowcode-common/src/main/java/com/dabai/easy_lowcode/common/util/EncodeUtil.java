package com.dabai.easy_lowcode.common.util;

import cn.hutool.core.util.IdUtil;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.UUID;
import java.util.zip.*;

/**
 * 编码与加密工具类
 */
@Slf4j
public class EncodeUtil {

    private EncodeUtil() {}

    // ==================== Base64 ====================

    /**
     * Base64编码
     */
    public static String base64Encode(String str) {
        return Base64.getEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64解码
     */
    public static String base64Decode(String str) {
        return new String(Base64.getDecoder().decode(str), StandardCharsets.UTF_8);
    }

    /**
     * Base64编码（URL安全）
     */
    public static String base64UrlEncode(String str) {
        return Base64.getUrlEncoder().encodeToString(str.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Base64解码（URL安全）
     */
    public static String base64UrlDecode(String str) {
        return new String(Base64.getUrlDecoder().decode(str), StandardCharsets.UTF_8);
    }

    // ==================== MD5 ====================

    /**
     * MD5加密
     */
    public static String md5(String str) {
        return hash("MD5", str);
    }

    /**
     * MD5加密（16位）
     */
    public static String md5_16(String str) {
        return md5(str).substring(8, 24);
    }

    // ==================== SHA ====================

    /**
     * SHA-1加密
     */
    public static String sha1(String str) {
        return hash("SHA-1", str);
    }

    /**
     * SHA-256加密
     */
    public static String sha256(String str) {
        return hash("SHA-256", str);
    }

    /**
     * SHA-512加密
     */
    public static String sha512(String str) {
        return hash("SHA-512", str);
    }

    // ==================== 通用哈希 ====================

    /**
     * 哈希加密
     */
    public static String hash(String algorithm, String str) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(str.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(digest);
        } catch (NoSuchAlgorithmException e) {
            log.error("哈希加密失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * 字节数组转十六进制字符串
     */
    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(b & 0xff);
            if (hex.length() == 1) {
                sb.append('0');
            }
            sb.append(hex);
        }
        return sb.toString();
    }

    // ==================== UUID ====================

    /**
     * 生成UUID
     */
    public static String uuid() {
        return UUID.randomUUID().toString();
    }

    /**
     * 生成不含横线的UUID
     */
    public static String uuidSimple() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 生成雪花ID
     */
    public static long snowflakeId() {
        return IdUtil.getSnowflakeNextId();
    }

    /**
     * 生成字符串类型的雪花ID
     */
    public static String snowflakeIdStr() {
        return String.valueOf(IdUtil.getSnowflakeNextId());
    }

    // ==================== 压缩 ====================

    /**
     * GZIP压缩字符串
     */
    public static String gzipCompress(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             GZIPOutputStream gzip = new GZIPOutputStream(out)) {
            gzip.write(str.getBytes(StandardCharsets.UTF_8));
            gzip.finish();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.error("GZIP压缩失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * GZIP解压字符串
     */
    public static String gzipUncompress(String compressedStr) {
        if (compressedStr == null || compressedStr.isEmpty()) {
            return compressedStr;
        }
        try {

            byte[] compressed = Base64.getDecoder().decode(compressedStr);
            try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
                 GZIPInputStream gzip = new GZIPInputStream(in);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = gzip.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return out.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("GZIP解压失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * DEFLATE压缩字符串
     */
    public static String deflateCompress(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             DeflaterOutputStream deflater = new DeflaterOutputStream(out)) {
            deflater.write(str.getBytes(StandardCharsets.UTF_8));
            deflater.finish();
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (IOException e) {
            log.error("DEFLATE压缩失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    /**
     * DEFLATE解压字符串
     */
    public static String deflateUncompress(String compressedStr) {
        if (compressedStr == null || compressedStr.isEmpty()) {
            return compressedStr;
        }
        try {
            byte[] compressed = Base64.getDecoder().decode(compressedStr);
            try (ByteArrayInputStream in = new ByteArrayInputStream(compressed);
                 InflaterInputStream inflater = new InflaterInputStream(in);
                 ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = inflater.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
                return out.toString(StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.error("DEFLATE解压失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    // ==================== URL编码 ====================

    /**
     * URL编码
     */
    public static String urlEncode(String str) {
        try {
            return java.net.URLEncoder.encode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * URL解码
     */
    public static String urlDecode(String str) {
        try {
            return java.net.URLDecoder.decode(str, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== HTML转义 ====================

    /**
     * HTML特殊字符转义
     */
    public static String htmlEscape(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /**
     * HTML特殊字符反转义
     */
    public static String htmlUnescape(String str) {
        if (str == null) {
            return null;
        }
        return str.replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'");
    }
}
