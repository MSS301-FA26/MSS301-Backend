package com.cinemaai.catalog.dto.request.cinema;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record RoomPricingRequest(
        @NotNull(message = "Standard price is required")
        @DecimalMin(value = "0.0", message = "Standard price must be non-negative")
        BigDecimal standardPrice,

        @NotNull(message = "VIP price is required")
        @DecimalMin(value = "0.0", message = "VIP price must be non-negative")
        BigDecimal vipPrice,

        @NotNull(message = "Couple price is required")
        @DecimalMin(value = "0.0", message = "Couple price must be non-negative")
        BigDecimal couplePrice
) {
}
