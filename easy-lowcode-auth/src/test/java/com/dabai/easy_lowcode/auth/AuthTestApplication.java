package com.dabai.easy_lowcode.auth;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * auth 模块测试专用启动配置
 * <p>
 * 供 @WebMvcTest 向上搜索 @SpringBootConfiguration 时使用
 * （真实启动类在 easy-lowcode-startup 模块，本模块测试无法发现）。
 * <p>
 * 关键: @ComponentScan 必须显式挂上 TypeExcludeFilter（@SpringBootApplication 的派生
 * 注解里自带这个接线，裸写的 @ComponentScan 没有），否则 @WebMvcTest 的切片过滤
 * （controllers 限定、Service/Config 排除）整体失效，整个包的 bean 都会涌入切片上下文。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "com.dabai.easy_lowcode.auth.controller",
                "com.dabai.easy_lowcode.common.exception"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class)
)
public class AuthTestApplication {
}
