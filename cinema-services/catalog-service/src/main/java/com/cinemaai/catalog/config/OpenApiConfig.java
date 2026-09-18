package com.cinemaai.catalog.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${app.gateway.url:http://localhost:8080}")
    private String gatewayUrl;

    @Bean
    public OpenAPI catalogOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CinemaAI - Catalog Service API")
                        .description("API Quản lý danh mục phim, thể loại, diễn viên, rạp chiếu, phòng chiếu, ghế ngồi, suất chiếu, đồ ăn và giá vé.")
                        .version("1.0.0")
                        .contact(new Contact().name("CinemaAI Development Team").email("dev@cinemaai.internal")))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("API Gateway (All requests must go through Gateway)")
                ));
    }
}
