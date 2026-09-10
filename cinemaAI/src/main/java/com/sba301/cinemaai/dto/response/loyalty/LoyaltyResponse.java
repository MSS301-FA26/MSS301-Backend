package com.sba301.cinemaai.dto.response.loyalty;

import com.sba301.cinemaai.entity.LoyaltyPoint;
import com.sba301.cinemaai.enums.LoyaltyStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoyaltyResponse {

    private Long userId;
    private String userEmail;
    private int points;
    private int totalPoints;
    private LoyaltyStatus status;

    public static LoyaltyResponse from(LoyaltyPoint lp) {
        return new LoyaltyResponse(
                lp.getUser().getId(),
                lp.getUser().getEmail(),
                lp.getPoints(),
                lp.getTotalPoints(),
                lp.getStatus()
        );
    }
}
