package com.cinemaai.identity.client;

import com.cinemaai.identity.dto.response.ApiResponse;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.NotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class CatalogClient {

    private final RestClient restClient;
    private final String internalSecret;

    public record InternalCinemaDto(
            Long id,
            String name,
            String status,
            boolean active
    ) {}

    public CatalogClient(
            @Value("${catalog.service.url}") String catalogServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(catalogServiceUrl).build();
    }

    public void validateActiveCinema(Long cinemaId) {
        if (cinemaId == null) {
            throw new BadRequestException("Cần chỉ định cụm rạp (cinemaId)");
        }
        try {
            ApiResponse<InternalCinemaDto> response = restClient.get()
                    .uri("/internal/v1/cinemas/{cinemaId}", cinemaId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<InternalCinemaDto>>() {});

            if (response == null || response.data() == null) {
                throw new NotFoundException("Cụm rạp ID #" + cinemaId + " không tồn tại");
            }
            if (!response.data().active()) {
                throw new BadRequestException("Cụm rạp '" + response.data().name() + "' (ID: " + cinemaId + ") hiện không hoạt động (Trạng thái: " + response.data().status() + ")");
            }
        } catch (NotFoundException | BadRequestException ex) {
            throw ex;
        } catch (HttpClientErrorException.NotFound ex) {
            throw new NotFoundException("Cụm rạp ID #" + cinemaId + " không tồn tại");
        } catch (Exception ex) {
            log.error("Lỗi khi kết nối sang Catalog Service để xác thực rạp cinemaId={}: {}", cinemaId, ex.getMessage());
            throw new BadRequestException("Không thể xác thực cụm rạp từ Catalog Service: " + ex.getMessage());
        }
    }
}
