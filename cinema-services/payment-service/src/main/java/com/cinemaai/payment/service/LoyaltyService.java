package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;

public interface LoyaltyService {
    LoyaltyResponse getMyPoints(Long userId, String email);
    LoyaltyConfigurationResponse getConfiguration();
    LoyaltyResponse redeemMyPoints(Long userId, String email, int points);

    LoyaltyConfigurationResponse updateConfiguration(com.cinemaai.payment.dto.request.LoyaltyConfigurationRequest request);
    com.cinemaai.payment.dto.response.PageResponse<com.cinemaai.payment.dto.response.LoyaltyTransactionResponse> searchTransactions(
            String keyword, java.time.LocalDateTime from, java.time.LocalDateTime to, int page, int size);
    com.cinemaai.payment.dto.response.LoyaltyReportResponse getReport(java.time.LocalDateTime from, java.time.LocalDateTime to);
    int expireAllActivePoints(String source);
    LoyaltyResponse addPoints(com.cinemaai.payment.dto.request.LoyaltyAddRequest request);
    LoyaltyResponse redeemPoints(Long userId, int points);
}
