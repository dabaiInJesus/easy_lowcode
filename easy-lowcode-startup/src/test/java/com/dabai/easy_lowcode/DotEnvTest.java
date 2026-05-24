package com.dabai.easy_lowcode;

import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * .env 文件加载测试
 */
class DotEnvTest {

    @Test
    void testLoadEnvFile() {
        // 加载 .env 文件
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        // 验证是否能读取到配置
        assertNotNull(dotenv, "Dotenv 实例不应为空");
        
        // 测试读取数据库密码
        String dbPassword = dotenv.get("POSTGRES_PASSWORD");
        System.out.println("POSTGRES_PASSWORD: " + dbPassword);
        assertNotNull(dbPassword, "POSTGRES_PASSWORD 不应为空");
        assertEquals("thinker", dbPassword, "POSTGRES_PASSWORD 应该是 thinker");

        // 测试读取 AI API Key
        String minimaxKey = dotenv.get("MINIMAX_API_KEY");
        System.out.println("MINIMAX_API_KEY: " + (minimaxKey != null ? "已配置" : "未配置"));
        assertNotNull(minimaxKey, "MINIMAX_API_KEY 不应为空");

        // 测试读取 AI 提供商
        String provider = dotenv.get("AI_DEFAULT_PROVIDER");
        System.out.println("AI_DEFAULT_PROVIDER: " + provider);
        assertEquals("minimax", provider, "AI_DEFAULT_PROVIDER 应该是 minimax");
    }

    @Test
    void testEnvVariablesToSystemProperties() {
        // 模拟启动类中的逻辑
        Dotenv dotenv = Dotenv.configure()
                .directory("./")
                .ignoreIfMissing()
                .load();

        // 将 .env 变量设置到系统属性
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });

        // 验证系统属性是否设置成功
        assertEquals("thinker", System.getProperty("POSTGRES_PASSWORD"), 
                "系统属性 POSTGRES_PASSWORD 应该被设置");
        assertEquals("minimax", System.getProperty("AI_DEFAULT_PROVIDER"), 
                "系统属性 AI_DEFAULT_PROVIDER 应该被设置");
        
        System.out.println("✅ 所有环境变量已成功加载到系统属性");
    }
}
