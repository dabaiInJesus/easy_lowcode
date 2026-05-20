package com.dabai.easy_lowcode.ai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI (Swagger UI) 配置
 *
 * 访问地址：
 * - Swagger UI: http://localhost:8081/swagger-ui.html
 * - OpenAPI 文档: http://localhost:8081/v3/api-docs
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Easy Lowcode AI API")
                        .description("AI 模块接口文档，提供统一的大模型调用和 Agent 管理能力")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("大白")
                                .email("dabai@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0")))
                .servers(List.of(
                        new Server().url("http://localhost:8081").description("本地开发环境")
                ));
    }

    @Bean
    public GroupedOpenApi chatApi() {
        return GroupedOpenApi.builder()
                .group("AI 对话")
                .pathsToMatch("/api/ai/chat/**")
                .build();
    }

    @Bean
    public GroupedOpenApi agentApi() {
        return GroupedOpenApi.builder()
                .group("AI Agent")
                .pathsToMatch("/api/ai/agent/**")
                .build();
    }

    @Bean
    public GroupedOpenApi configApi() {
        return GroupedOpenApi.builder()
                .group("AI 配置")
                .pathsToMatch("/api/ai/config/**")
                .build();
    }
}
