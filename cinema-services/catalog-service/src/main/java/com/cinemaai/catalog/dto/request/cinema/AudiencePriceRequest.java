package com.cinemaai.catalog.dto.request.cinema;

import com.cinemaai.catalog.enums.AudienceType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/**
 * Request to set/update the additional price for one audience type at a cinema.
 */
public record AudiencePriceRequest(

        @NotNull(message = "Audience type is required")
        AudienceType audienceType,

        @NotNull(message = "Additional price is required")
        @DecimalMin(value = "0", message = "Additional price must be >= 0")
        @DecimalMax(value = "500000", message = "Additional price must be <= 500000")
        BigDecimal additionalPrice
) {
}
