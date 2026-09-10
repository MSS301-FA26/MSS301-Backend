package com.sba301.cinemaai.dto.request.loyalty;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LoyaltyAddRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Points are required")
    @Min(value = 1, message = "Points must be at least 1")
    private Integer points;

    private String reason;
}
