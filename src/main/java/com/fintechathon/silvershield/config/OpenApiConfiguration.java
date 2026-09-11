package com.fintechathon.silvershield.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    static final String BEARER_SCHEME = "bearerSession";

    @Bean
    OpenAPI silverShieldOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("银盾智行 Banking Sandbox API")
                        .description("安全可信个人银行智能体的比赛演示接口；所有数据均为可复现的虚构沙箱数据。")
                        .version("0.8.0")
                        .contact(new Contact().name("FinTechathon 2026 银盾智行团队"))
                        .license(new License().name("Competition demonstration only")))
                .schemaRequirement(BEARER_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("opaque-session-token")
                        .description("通过 /api/v1/auth/login 获取的短期不透明会话令牌"));
    }

    @Bean
    OpenApiCustomizer authenticatedOperations() {
        return openApi -> {
            if (openApi.getPaths() == null) return;
            openApi.getPaths().forEach((path, pathItem) -> {
                if (isPublic(path)) return;
                pathItem.readOperations().forEach(operation ->
                        operation.addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME)));
            });
        };
    }

    private static boolean isPublic(String path) {
        return path.equals("/api/v1/auth/login")
                || path.equals("/actuator/health")
                || path.equals("/actuator/info");
    }
}
