package com.dabai.easy_lowcode;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 低代码平台启动类
 */
@SpringBootApplication(scanBasePackages = {"com.dabai.easy_lowcode"})
@MapperScan("com.dabai.easy_lowcode.**.mapper")
public class EasyLowcodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EasyLowcodeApplication.class, args);
        System.out.println("========================================");
        System.out.println("   低代码平台启动成功！");
        System.out.println("========================================");
    }
}
