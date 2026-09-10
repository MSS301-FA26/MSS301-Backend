package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.loyalty.LoyaltyAddRequest;
import com.sba301.cinemaai.dto.request.loyalty.LoyaltyConfigurationRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyConfigurationResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyReportResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyResponse;
import com.sba301.cinemaai.dto.response.loyalty.LoyaltyTransactionResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.FoodOrder;
import com.sba301.cinemaai.entity.User;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface LoyaltyPointService {

        LoyaltyResponse getMyPoints(String email);

        LoyaltyResponse addPoints(LoyaltyAddRequest request);

        LoyaltyResponse redeemPoints(Long userId, int points);

        /** Redeem points by authenticated customer (1000 points = 1000 VND discount). */
        LoyaltyResponse redeemMyPoints(String email, int points);

        void addPointsFromBooking(User user, Booking booking);

        void addPointsFromFoodOrder(User user, FoodOrder foodOrder);

        int redeemPointsForBooking(User user, Booking booking, int points);

        int getMaxRedeemablePointsForAmount(BigDecimal amount);

        void restoreRedeemedPointsFromBooking(User user, Booking booking);

        /** Revoke points that were previously earned from a booking (e.g. when showtime is cancelled). */
        void revokePointsFromBooking(User user, Booking booking);

        LoyaltyConfigurationResponse getConfiguration();

        LoyaltyConfigurationResponse updateConfiguration(LoyaltyConfigurationRequest request);

        PageResponse<LoyaltyTransactionResponse> searchTransactions(String keyword, LocalDateTime from, LocalDateTime to, int page, int size);

        LoyaltyReportResponse getReport(LocalDateTime from, LocalDateTime to);

        int expireAllActivePoints(String resetSource);
}
