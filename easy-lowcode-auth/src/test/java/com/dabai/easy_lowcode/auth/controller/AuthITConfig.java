package com.dabai.easy_lowcode.auth.controller;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * auth 集成测试完整上下文配置
 * <p>
 * @SpringBootTest 通过 classes 显式指定，不走向上发现；
 * 与真实启动类保持一致（宽包扫描 + MapperScan）。
 */
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration
@ComponentScan("com.dabai.easy_lowcode")
@MapperScan("com.dabai.easy_lowcode.**.mapper")
public class AuthITConfig {
}
