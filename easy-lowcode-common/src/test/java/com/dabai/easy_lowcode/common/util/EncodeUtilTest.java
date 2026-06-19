package com.dabai.easy_lowcode.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EncodeUtilTest {

    @Test
    void base64Encode_base64Decode_roundtrip() {
        String original = "hello world";
        String encoded = EncodeUtil.base64Encode(original);
        String decoded = EncodeUtil.base64Decode(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void base64UrlEncode_base64UrlDecode_roundtrip() {
        String original = "hello world";
        String encoded = EncodeUtil.base64UrlEncode(original);
        String decoded = EncodeUtil.base64UrlDecode(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void base64UrlEncode_producesUrlSafeOutput() {
        String input = "a]b?c/d e+f=g";
        String encoded = EncodeUtil.base64UrlEncode(input);
        assertThat(encoded).doesNotContain("+", "/");
    }

    @Test
    void md5_produces32CharHexString() {
        String result = EncodeUtil.md5("test");
        assertThat(result).hasSize(32);
        assertThat(result).matches("[0-9a-f]{32}");
    }

    @Test
    void md5_isConsistent() {
        String hash1 = EncodeUtil.md5("hello");
        String hash2 = EncodeUtil.md5("hello");
        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void md5_knownVector() {
        assertThat(EncodeUtil.md5("hello")).isEqualTo("5d41402abc4b2a76b9719d911017c592");
    }

    @Test
    void md5_16_produces16CharHexString() {
        String result = EncodeUtil.md5_16("test");
        assertThat(result).hasSize(16);
        assertThat(result).matches("[0-9a-f]{16}");
    }

    @Test
    void sha1_produces40CharHexString() {
        String result = EncodeUtil.sha1("test");
        assertThat(result).hasSize(40);
        assertThat(result).matches("[0-9a-f]{40}");
    }

    @Test
    void sha256_produces64CharHexString() {
        String result = EncodeUtil.sha256("test");
        assertThat(result).hasSize(64);
        assertThat(result).matches("[0-9a-f]{64}");
    }

    @Test
    void sha512_produces128CharHexString() {
        String result = EncodeUtil.sha512("test");
        assertThat(result).hasSize(128);
        assertThat(result).matches("[0-9a-f]{128}");
    }

    @Test
    void bytesToHex_producesCorrectHexString() {
        byte[] bytes = {(byte) 0xCA, (byte) 0xFE, 0x00, (byte) 0xFF};
        assertThat(EncodeUtil.bytesToHex(bytes)).isEqualTo("cafe00ff");
    }

    @Test
    void uuid_returns36CharStringWithHyphens() {
        String result = EncodeUtil.uuid();
        assertThat(result).hasSize(36);
        assertThat(result).contains("-");
        assertThat(result).matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void uuidSimple_returns32CharStringWithoutHyphens() {
        String result = EncodeUtil.uuidSimple();
        assertThat(result).hasSize(32);
        assertThat(result).doesNotContain("-");
        assertThat(result).matches("[0-9a-f]{32}");
    }

    @Test
    void gzipCompress_gzipUncompress_roundtrip_chineseText() {
        String original = "你好世界，这是一个测试";
        String compressed = EncodeUtil.gzipCompress(original);
        String decompressed = EncodeUtil.gzipUncompress(compressed);
        assertThat(decompressed).isEqualTo(original);
    }

    @Test
    void gzipCompress_gzipUncompress_emptyString() {
        assertThat(EncodeUtil.gzipCompress("")).isEmpty();
        assertThat(EncodeUtil.gzipUncompress("")).isEmpty();
    }

    @Test
    void deflateCompress_deflateUncompress_roundtrip() {
        String original = "deflate test data 1234567890";
        String compressed = EncodeUtil.deflateCompress(original);
        String decompressed = EncodeUtil.deflateUncompress(compressed);
        assertThat(decompressed).isEqualTo(original);
    }

    @Test
    void urlEncode_urlDecode_roundtrip_specialCharacters() {
        String original = "hello world&foo=bar?q=你好";
        String encoded = EncodeUtil.urlEncode(original);
        String decoded = EncodeUtil.urlDecode(encoded);
        assertThat(decoded).isEqualTo(original);
    }

    @Test
    void htmlEscape_escapesSpecialCharacters() {
        String input = "<div class=\"a\">&'test'</div>";
        String escaped = EncodeUtil.htmlEscape(input);
        assertThat(escaped).isEqualTo("&lt;div class=&quot;a&quot;&gt;&amp;&#39;test&#39;&lt;/div&gt;");
    }

    @Test
    void htmlUnescape_reversesHtmlEscape() {
        String escaped = "&lt;div class=&quot;a&quot;&gt;&amp;&#39;test&#39;&lt;/div&gt;";
        String unescaped = EncodeUtil.htmlUnescape(escaped);
        assertThat(unescaped).isEqualTo("<div class=\"a\">&'test'</div>");
    }

    @Test
    void htmlEscape_htmlUnescape_roundtrip() {
        String original = "<p>\"Hello\" & 'World' <b>bold</b></p>";
        assertThat(EncodeUtil.htmlUnescape(EncodeUtil.htmlEscape(original))).isEqualTo(original);
    }
}
