package com.sba301.cinemaai.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI cineAiOpenApi() {
        return new OpenAPI()
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter your JWT token")))
                .info(new Info()
                        .title("CineAI API")
                        .version("v1")
                        .description("Cinema booking platform"));
    }

    /** Admin operations only — everything under /api/v1/admin/**. */
    @Bean
    public GroupedOpenApi adminApi() {
        return GroupedOpenApi.builder()
                .group("1-admin")
                .displayName("Admin APIs")
                .pathsToMatch("/api/v1/admin/**")
                .build();
    }

    /** Staff operations only — check-in and lookup under /api/v1/staff/**. */
    @Bean
    public GroupedOpenApi staffApi() {
        return GroupedOpenApi.builder()
                .group("2-staff")
                .displayName("Staff APIs")
                .pathsToMatch("/api/v1/staff/**")
                .build();
    }

    /** Customer + public business flows — everything except admin/staff. */
    @Bean
    public GroupedOpenApi customerPublicApi() {
        return GroupedOpenApi.builder()
                .group("3-customer-public")
                .displayName("Customer & Public APIs")
                .pathsToMatch("/api/v1/**")
                .pathsToExclude("/api/v1/admin/**", "/api/v1/staff/**")
                .build();
    }
}
