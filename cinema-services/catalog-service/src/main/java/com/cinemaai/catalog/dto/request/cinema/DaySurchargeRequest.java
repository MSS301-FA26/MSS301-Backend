package com.cinemaai.catalog.dto.request.cinema;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Request to set/update weekend, holiday, and late night price surcharges for a cinema.
 */
public record DaySurchargeRequest(

        @NotNull(message = "Weekend surcharge is required")
        @DecimalMin(value = "0", message = "Weekend surcharge must be >= 0")
        @DecimalMax(value = "10000000", message = "Weekend surcharge must be <= 1000000")
        BigDecimal weekendSurcharge,

        @NotNull(message = "Holiday surcharge is required")
        @DecimalMin(value = "0", message = "Holiday surcharge must be >= 0")
        @DecimalMax(value = "10000000", message = "Holiday surcharge must be <= 1000000")
        BigDecimal holidaySurcharge,

        @DecimalMin(value = "0", message = "Night surcharge must be >= 0")
        @DecimalMax(value = "10000000", message = "Night surcharge must be <= 1000000")
        BigDecimal nightSurcharge
) {
    public DaySurchargeRequest(BigDecimal weekendSurcharge, BigDecimal holidaySurcharge) {
        this(weekendSurcharge, holidaySurcharge, BigDecimal.ZERO);
    }
}
