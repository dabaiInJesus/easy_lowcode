package com.dabai.easy_lowcode.common.util;

import com.dabai.easy_lowcode.common.annotation.Sensitive;
import com.dabai.easy_lowcode.common.annotation.SensitiveType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveUtilTest {

    private SensitiveUtil util;

    @BeforeEach
    void setUp() {
        util = new SensitiveUtil();
    }

    // --- maskPhone ---

    @Test
    void maskPhone_normalPhone() {
        assertThat(util.maskPhone("13812345678")).isEqualTo("138***5678");
    }

    @Test
    void maskPhone_shortPhone_returnsAsIs() {
        assertThat(util.maskPhone("12345")).isEqualTo("12345");
    }

    @Test
    void maskPhone_null_returnsNull() {
        assertThat(util.maskPhone(null)).isNull();
    }

    // --- maskEmail ---

    @Test
    void maskEmail_normalEmail() {
        assertThat(util.maskEmail("test@example.com")).isEqualTo("t***@example.com");
    }

    @Test
    void maskEmail_noAtSymbol_returnsAsIs() {
        assertThat(util.maskEmail("testexample.com")).isEqualTo("testexample.com");
    }

    @Test
    void maskEmail_null_returnsNull() {
        assertThat(util.maskEmail(null)).isNull();
    }

    // --- maskIdCard ---

    @Test
    void maskIdCard_normalId() {
        assertThat(util.maskIdCard("110101199001011234")).isEqualTo("110101**********1234");
    }

    @Test
    void maskIdCard_shortId_returnsAsIs() {
        assertThat(util.maskIdCard("123456789")).isEqualTo("123456789");
    }

    // --- maskName ---

    @Test
    void maskName_twoCharName() {
        assertThat(util.maskName("张三")).isEqualTo("张*");
    }

    @Test
    void maskName_threeCharName() {
        assertThat(util.maskName("张三丰")).isEqualTo("张**");
    }

    @Test
    void maskName_oneCharName_returnsAsIs() {
        assertThat(util.maskName("张")).isEqualTo("张");
    }

    // --- maskBankCard ---

    @Test
    void maskBankCard_normalCard() {
        assertThat(util.maskBankCard("6222021234567890123")).isEqualTo("622202****0123");
    }

    @Test
    void maskBankCard_shortCard_returnsAsIs() {
        assertThat(util.maskBankCard("123456789")).isEqualTo("123456789");
    }

    // --- maskAddress ---

    @Test
    void maskAddress_normalAddress() {
        assertThat(util.maskAddress("北京市朝阳区三里屯街道")).isEqualTo("北京市朝阳区***");
    }

    @Test
    void maskAddress_shortAddress_returnsAsIs() {
        assertThat(util.maskAddress("北京")).isEqualTo("北京");
    }

    @Test
    void maskAddress_exactlySixChars_returnsAsIs() {
        assertThat(util.maskAddress("123456")).isEqualTo("123456");
    }

    // --- maskDefault ---

    @Test
    void maskDefault_normalValue() {
        assertThat(util.maskDefault("abcdef", '*')).isEqualTo("a****f");
    }

    @Test
    void maskDefault_shortValue_returnsAsIs() {
        assertThat(util.maskDefault("ab", '*')).isEqualTo("ab");
    }

    @Test
    void maskDefault_singleChar_returnsAsIs() {
        assertThat(util.maskDefault("a", '*')).isEqualTo("a");
    }

    // --- maskCustom ---

    @Test
    void maskCustom_validRule() {
        assertThat(util.maskCustom("1234567890", "3:4", '*')).isEqualTo("123***7890");
    }

    @Test
    void maskCustom_invalidRule_fallsBackToDefault() {
        assertThat(util.maskCustom("1234567890", "invalid", '*')).isEqualTo("1********0");
    }

    @Test
    void maskCustom_prefixSuffixExceedLength_fallsBackToDefault() {
        assertThat(util.maskCustom("abc", "3:3", '*')).isEqualTo("a*c");
    }

    // --- maskMap ---

    @Test
    void maskMap_phoneKeyGetsMasked() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("phone", "13812345678");
        map.put("unknown", "hello");

        Map<String, Object> result = util.maskMap(map);
        assertThat(result.get("phone")).isEqualTo("138***5678");
        assertThat(result.get("unknown")).isEqualTo("hello");
    }

    @Test
    void maskMap_nestedMapGetsMasked() {
        Map<String, Object> inner = new LinkedHashMap<>();
        inner.put("email", "user@test.com");

        Map<String, Object> outer = new LinkedHashMap<>();
        outer.put("contact", inner);
        outer.put("age", 25);

        Map<String, Object> result = util.maskMap(outer);
        @SuppressWarnings("unchecked")
        Map<String, Object> nested = (Map<String, Object>) result.get("contact");
        assertThat(nested.get("email")).isEqualTo("u***@test.com");
        assertThat(result.get("age")).isEqualTo(25);
    }

    @Test
    void maskMap_passwordKeyGetsFullyMasked() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("password", "secret123");

        Map<String, Object> result = util.maskMap(map);
        assertThat(result.get("password")).isEqualTo("******");
    }

    // --- mask (null) ---

    @Test
    void mask_nullInput_returnsNull() {
        assertThat(util.mask((Object) null)).isNull();
    }

    // --- mask with annotation type ---

    @Test
    void mask_withPhoneType_masksCorrectly() {
        Sensitive annotation = new Sensitive() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Sensitive.class; }
            @Override public SensitiveType type() { return SensitiveType.PHONE; }
            @Override public String customRule() { return ""; }
            @Override public char maskChar() { return '*'; }
            @Override public int visibleLength() { return 4; }
        };
        assertThat(util.mask("13812345678", annotation)).isEqualTo("138***5678");
    }

    @Test
    void mask_withNoneType_returnsAsIs() {
        Sensitive annotation = new Sensitive() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Sensitive.class; }
            @Override public SensitiveType type() { return SensitiveType.NONE; }
            @Override public String customRule() { return ""; }
            @Override public char maskChar() { return '*'; }
            @Override public int visibleLength() { return 4; }
        };
        assertThat(util.mask("13812345678", annotation)).isEqualTo("13812345678");
    }

    @Test
    void mask_withPasswordType_returnsStars() {
        Sensitive annotation = new Sensitive() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Sensitive.class; }
            @Override public SensitiveType type() { return SensitiveType.PASSWORD; }
            @Override public String customRule() { return ""; }
            @Override public char maskChar() { return '*'; }
            @Override public int visibleLength() { return 4; }
        };
        assertThat(util.mask("secret", annotation)).isEqualTo("******");
    }

    @Test
    void mask_withEmptyValue_returnsEmpty() {
        Sensitive annotation = new Sensitive() {
            @Override public Class<? extends java.lang.annotation.Annotation> annotationType() { return Sensitive.class; }
            @Override public SensitiveType type() { return SensitiveType.PHONE; }
            @Override public String customRule() { return ""; }
            @Override public char maskChar() { return '*'; }
            @Override public int visibleLength() { return 4; }
        };
        assertThat(util.mask("", annotation)).isEmpty();
    }
}
