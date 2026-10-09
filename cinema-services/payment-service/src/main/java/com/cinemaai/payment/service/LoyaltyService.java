package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.request.AwardBookingPointsRequest;
import com.cinemaai.payment.dto.request.LoyaltyAddRequest;
import com.cinemaai.payment.dto.request.LoyaltyConfigurationRequest;
import com.cinemaai.payment.dto.request.RefundBookingPointsRequest;
import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyReportResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;
import com.cinemaai.payment.dto.response.LoyaltyTransactionResponse;
import com.cinemaai.payment.dto.response.PageResponse;
import com.cinemaai.payment.security.AuthenticatedUser;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LoyaltyService {
    LoyaltyResponse getMyPoints(Long userId, String email);
    LoyaltyConfigurationResponse getConfiguration();
    LoyaltyConfigurationResponse getConfiguration(Long cinemaId);
    LoyaltyResponse redeemMyPoints(Long userId, String email, int points);

    LoyaltyConfigurationResponse updateConfiguration(LoyaltyConfigurationRequest request);
    LoyaltyConfigurationResponse updateConfiguration(LoyaltyConfigurationRequest request, AuthenticatedUser user);
    PageResponse<LoyaltyTransactionResponse> searchTransactions(
            String keyword, LocalDateTime from, LocalDateTime to, int page, int size);
    LoyaltyReportResponse getReport(LocalDateTime from, LocalDateTime to);
    int expireAllActivePoints(String source);
    LoyaltyResponse addPoints(LoyaltyAddRequest request);
    LoyaltyResponse redeemPoints(Long userId, int points);

    LoyaltyResponse awardPointsForBooking(AwardBookingPointsRequest request);
    LoyaltyResponse refundPointsForBooking(RefundBookingPointsRequest request);
    LoyaltyResponse awardPointsForFoodOrder(Long userId, Long foodOrderId, String orderCode, BigDecimal amount);
}
