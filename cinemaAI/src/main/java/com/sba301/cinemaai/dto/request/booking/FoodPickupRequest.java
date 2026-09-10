package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.constraints.NotBlank;

public record FoodPickupRequest(
        @NotBlank(message = "Food order code or QR code is required") String code
) {
}
