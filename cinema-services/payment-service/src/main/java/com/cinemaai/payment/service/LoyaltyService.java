package com.cinemaai.payment.service;

import com.cinemaai.payment.dto.response.LoyaltyConfigurationResponse;
import com.cinemaai.payment.dto.response.LoyaltyResponse;

public interface LoyaltyService {
    LoyaltyResponse getMyPoints(Long userId, String email);
    LoyaltyConfigurationResponse getConfiguration();
    LoyaltyResponse redeemMyPoints(Long userId, String email, int points);
}
