package com.cinemaai.payment.dto.response;

import com.cinemaai.payment.enums.LoyaltyStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyResponse {
    private Long userId;
    private String userEmail;
    private int points;
    private int totalPoints;
    private LoyaltyStatus status;
}
