package com.dabai.easy_lowcode.resource;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 资源查询模块启动类
 */
@SpringBootApplication
@MapperScan("com.dabai.easy_lowcode.resource.mapper")
public class ResourceApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ResourceApplication.class, args);
    }
}
