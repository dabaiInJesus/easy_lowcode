package com.dabai.easy_lowcode.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EncryptUtilTest {

    @Test
    void bcrypt_producesNonNullNonEmptyHash() {
        String hash = EncryptUtil.bcrypt("password123");
        assertThat(hash).isNotNull().isNotBlank();
    }

    @Test
    void verifyPassword_returnsTrueForMatchingPassword() {
        String hash = EncryptUtil.bcrypt("password123");
        assertThat(EncryptUtil.verifyPassword("password123", hash)).isTrue();
    }

    @Test
    void verifyPassword_returnsFalseForWrongPassword() {
        String hash = EncryptUtil.bcrypt("password123");
        assertThat(EncryptUtil.verifyPassword("wrongpassword", hash)).isFalse();
    }

    @Test
    void bcrypt_producesDifferentHashesForSameInput() {
        String hash1 = EncryptUtil.bcrypt("password123");
        String hash2 = EncryptUtil.bcrypt("password123");
        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    void encrypt_returnsNonNullNonEmptyString() {
        String encrypted = EncryptUtil.encrypt("hello");
        assertThat(encrypted).isNotNull().isNotBlank();
    }

    @Test
    void encrypt_decrypt_roundtrip() {
        String original = "Hello, World!";
        String encrypted = EncryptUtil.encrypt(original);
        String decrypted = EncryptUtil.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void encrypt_decrypt_withEmptyString() {
        String encrypted = EncryptUtil.encrypt("");
        String decrypted = EncryptUtil.decrypt(encrypted);
        assertThat(decrypted).isEmpty();
    }

    @Test
    void encrypt_decrypt_withSpecialCharacters() {
        String original = "你好世界!@#$%^&*()_+{}|:\"<>?";
        String encrypted = EncryptUtil.encrypt(original);
        String decrypted = EncryptUtil.decrypt(encrypted);
        assertThat(decrypted).isEqualTo(original);
    }

    @Test
    void decrypt_throwsForInvalidBase64Input() {
        assertThatThrownBy(() -> EncryptUtil.decrypt("!!!not-valid-base64!!!"))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void md5_producesConsistentHash() {
        String hash1 = EncryptUtil.md5("test");
        String hash2 = EncryptUtil.md5("test");
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(32);
    }

    @Test
    void sha256_producesConsistentHash() {
        String hash1 = EncryptUtil.sha256("test");
        String hash2 = EncryptUtil.sha256("test");
        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64);
    }
}
