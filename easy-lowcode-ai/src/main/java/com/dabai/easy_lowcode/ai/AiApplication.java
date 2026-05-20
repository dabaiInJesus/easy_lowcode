package com.dabai.easy_lowcode.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Easy Lowcode AI 模块启动类
 * 
 * 本模块可以独立运行，提供 AI 聊天和 Agent 功能
 */
@Slf4j
@SpringBootApplication
@EnableScheduling
public class AiApplication {
    
    public static void main(String[] args) {
        log.info("========================================");
        log.info("  Easy Lowcode AI 模块启动中...");
        log.info("========================================");
        
        SpringApplication.run(AiApplication.class, args);
        
        log.info("========================================");
        log.info("  Easy Lowcode AI 模块启动成功！");
        log.info("  API 文档: http://localhost:8081/api/ai");
        log.info("========================================");
    }
}
