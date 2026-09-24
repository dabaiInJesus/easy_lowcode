package com.dabai.easy_lowcode;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * .env 文件加载测试
 * <p>
 * 说明：.env 是本地私有配置（在 .gitignore 中），不同机器的值各不相同，
 * 因此这里只断言"文件存在且关键变量非空"，不做具体值的断言。
 */
class DotEnvTest {

    /**
     * 定位项目根目录的 .env（Maven surefire 的工作目录是各模块目录）
     */
    private Dotenv loadRootEnv() {
        String[] candidates = {"./", "../"};
        for (String dir : candidates) {
            if (Files.exists(Path.of(dir, ".env"))) {
                return Dotenv.configure()
                        .directory(dir)
                        .ignoreIfMissing()
                        .load();
            }
        }
        return null;
    }

    @Test
    void testLoadEnvFile() {
        Dotenv dotenv = loadRootEnv();
        Assumptions.assumeTrue(dotenv != null, "未找到 .env 文件（本地私有配置），跳过");

        String dbPassword = dotenv.get("POSTGRES_PASSWORD");
        assertNotNull(dbPassword, "POSTGRES_PASSWORD 不应为空");
        assertFalse(dbPassword.isBlank(), "POSTGRES_PASSWORD 不应为空字符串");

        String jwtSecret = dotenv.get("JWT_SECRET");
        if (jwtSecret != null) {
            assertFalse(jwtSecret.startsWith("your-"), "JWT_SECRET 仍是占位符，请填入真实密钥");
        }

        String aesKey = dotenv.get("ENCRYPT_AES_KEY");
        if (aesKey != null) {
            assertFalse(aesKey.startsWith("your-"), "ENCRYPT_AES_KEY 仍是占位符，请填入真实密钥（16 位）");
        }
    }

    @Test
    void testEnvVariablesToSystemProperties() {
        Dotenv dotenv = loadRootEnv();
        Assumptions.assumeTrue(dotenv != null, "未找到 .env 文件（本地私有配置），跳过");

        // 与启动类相同的逻辑：将 .env 变量设置到系统属性（已有属性不覆盖）
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        // 验证系统属性是否设置成功
        assertEquals(dotenv.get("POSTGRES_PASSWORD"),
                System.getProperty("POSTGRES_PASSWORD"),
                "系统属性 POSTGRES_PASSWORD 应该与 .env 中一致");
    }
}
