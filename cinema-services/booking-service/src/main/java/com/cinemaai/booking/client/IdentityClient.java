package com.cinemaai.booking.client;

import com.cinemaai.booking.client.dto.UserAccessScopeDto;
import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.exception.UnauthorizedException;
import java.math.BigDecimal;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class IdentityClient {

    private final RestClient restClient;
    private final String internalSecret;

    public IdentityClient(
            @Value("${identity.service.url}") String identityServiceUrl,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.internalSecret = internalSecret;
        this.restClient = RestClient.builder().baseUrl(identityServiceUrl).build();
    }

    public UserAccessScopeDto getUserAccessScope(Long userId) {
        try {
            ApiResponse<UserAccessScopeDto> response = restClient.get()
                    .uri("/internal/v1/users/{userId}/scope", userId)
                    .header("X-Internal-Service-Secret", internalSecret)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<UserAccessScopeDto>>() {});

            if (response == null || response.data() == null) {
                throw new UnauthorizedException("Không tìm thấy thông tin tài khoản người dùng");
            }

            UserAccessScopeDto scope = response.data();
            if (!"ACTIVE".equalsIgnoreCase(scope.status())) {
                throw new ForbiddenException("Tài khoản đã bị khóa hoặc chưa kích hoạt");
            }

            return scope;
        } catch (ForbiddenException | UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to fetch access scope from Identity Service for userId {}: {}", userId, ex.getMessage());
            throw new UnauthorizedException("Không thể xác thực quyền hạn từ Identity Service: " + ex.getMessage());
        }
    }

    public void sendWalletRefundNotice(String to, String bookingCode, BigDecimal amount, BigDecimal newBalance, String reason) {
        if (to == null || to.isBlank()) {
            log.warn("Cannot send refund notice email: recipient email is blank for booking {}", bookingCode);
            return;
        }
        try {
            Map<String, Object> body = Map.of(
                    "to", to,
                    "bookingCode", bookingCode != null ? bookingCode : "",
                    "amount", amount != null ? amount : BigDecimal.ZERO,
                    "newBalance", newBalance != null ? newBalance : BigDecimal.ZERO,
                    "reason", reason != null ? reason : "Hoan tien ve"
            );

            ApiResponse<Void> response = restClient.post()
                    .uri("/internal/v1/mail/refund-notice")
                    .header("X-Internal-Service-Secret", internalSecret)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<ApiResponse<Void>>() {});

            if (response != null && response.success()) {
                log.info("Wallet refund notice email successfully dispatched to {} for booking {}", to, bookingCode);
            } else {
                log.warn("Wallet refund notice email response not successful for booking {}: {}",
                        bookingCode, response != null ? response.message() : "null response");
            }
        } catch (Exception ex) {
            log.error("Failed to send wallet refund email notice to {} for booking {}: {}", to, bookingCode, ex.getMessage());
        }
    }
}
