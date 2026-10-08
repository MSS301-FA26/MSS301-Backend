package com.cinemaai.catalog.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "Bearer Authentication";

    @Bean
    public OpenAPI catalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CinemaAI - Catalog Service API")
                        .description("API Quản lý danh mục phim, thể loại, diễn viên, rạp chiếu, phòng chiếu, ghế ngồi, suất chiếu, đồ ăn và giá vé.")
                        .version("1.0.0")
                        .contact(new Contact().name("CinemaAI Development Team").email("dev@cinemaai.internal")))
                .servers(List.of(
                        new Server().url("http://localhost:8080").description("API Gateway (Port 8080)")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Nhập JWT Access Token vào ô bên dưới (không cần gõ tiền tố 'Bearer ')")));
    }
}
