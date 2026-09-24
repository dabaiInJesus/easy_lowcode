package com.dabai.easy_lowcode.collector;

import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.TypeExcludeFilter;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

/**
 * collector 模块测试专用启动配置（供 @WebMvcTest 向上搜索时使用）。
 * <p>
 * 关键: @ComponentScan 必须显式挂上 TypeExcludeFilter（@SpringBootApplication 派生注解
 * 自带该接线，裸写的 @ComponentScan 没有），否则 @WebMvcTest 切片过滤整体失效。
 */
@SpringBootConfiguration
@EnableAutoConfiguration
@ComponentScan(
        basePackages = {
                "com.dabai.easy_lowcode.collector.controller",
                "com.dabai.easy_lowcode.common.exception"
        },
        excludeFilters = @ComponentScan.Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class)
)
public class CollectorTestApplication {
}
