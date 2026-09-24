package com.cinemaai.booking.config;

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

    @Value("${app.gateway.url}")
    private String gatewayUrl;

    @Bean
    public OpenAPI bookingOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CinemaAI - Booking Service API")
                        .description("API Quản lý đặt vé, giữ chỗ ghế tạm thời (seat hold), combo đồ ăn, soát vé (check-in) và snapshot đơn hàng.")
                        .version("1.0.0")
                        .contact(new Contact().name("CinemaAI Development Team").email("dev@cinemaai.internal")))
                .servers(List.of(
                        new Server().url(gatewayUrl).description("API Gateway (All requests must go through Gateway)")
                ));
    }
}
