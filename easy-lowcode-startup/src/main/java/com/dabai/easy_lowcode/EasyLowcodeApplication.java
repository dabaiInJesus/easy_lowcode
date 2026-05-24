package com.dabai.easy_lowcode;

import io.github.cdimascio.dotenv.Dotenv;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashMap;
import java.util.Map;

/**
 * 低代码平台启动类
 */
@SpringBootApplication(scanBasePackages = {"com.dabai.easy_lowcode"})
@MapperScan("com.dabai.easy_lowcode.**.mapper")
public class EasyLowcodeApplication {

    public static void main(String[] args) {
        // 加载 .env 文件并获取配置
        Map<String, Object> envProperties = loadEnvFile();
        
        // 创建 SpringApplication 实例
        SpringApplication app = new SpringApplication(EasyLowcodeApplication.class);
        
        // 设置默认属性（优先级低于系统环境变量和命令行参数）
        if (!envProperties.isEmpty()) {
            app.setDefaultProperties(envProperties);
            System.out.println("✅ 已加载 " + envProperties.size() + " 个环境变量配置");
        }
        
        // 启动应用
        app.run(args);
        
        System.out.println("========================================");
        System.out.println("   低代码平台启动成功！");
        System.out.println("========================================");
    }

    /**
     * 加载 .env 文件到 Map
     * @return 环境变量键值对
     */
    private static Map<String, Object> loadEnvFile() {
        Map<String, Object> properties = new HashMap<>();
        
        try {
            // 尝试多个可能的位置
            String[] possiblePaths = {"./", "../", "../../"};
            Dotenv dotenv = null;
            
            for (String path : possiblePaths) {
                try {
                    dotenv = Dotenv.configure()
                            .directory(path)
                            .filename(".env")
                            .load();
                    System.out.println("✅ .env 文件加载成功 (位置: " + path + ")");
                    break;
                } catch (Exception e) {
                    // 继续尝试下一个路径
                }
            }
            
            if (dotenv == null) {
                // 如果都失败，使用 ignoreIfMissing
                dotenv = Dotenv.configure()
                        .directory("./")
                        .ignoreIfMissing()
                        .load();
                System.out.println("⚠️  未找到 .env 文件，将使用系统环境变量");
            }
            
            // 将 .env 中的变量放入 Map
            dotenv.entries().forEach(entry -> {
                properties.put(entry.getKey(), entry.getValue());
                // 同时设置到系统属性（供其他库使用）
                if (System.getProperty(entry.getKey()) == null) {
                    System.setProperty(entry.getKey(), entry.getValue());
                }
                // 特殊处理 ENCRYPT_AES_KEY，设置到 encrypt.aes.key 系统属性
                if ("ENCRYPT_AES_KEY".equals(entry.getKey())) {
                    System.setProperty("encrypt.aes.key", entry.getValue());
                }
            });
            
        } catch (Exception e) {
            System.out.println("⚠️  警告: 加载 .env 文件失败 - " + e.getMessage());
            System.out.println("   提示: 请确保项目根目录存在 .env 文件或配置了系统环境变量");
        }
        
        return properties;
    }
}
