package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.ShowtimeStatus;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeRequest(
        @NotNull(message = "Movie id is required")
        Long movieId,

        @NotNull(message = "Room id is required")
        Long roomId,

        @NotNull(message = "Start time is required")
        @Future(message = "Start time must be in the future")
        LocalDateTime startTime,

        @NotNull(message = "Base price is required")
        @DecimalMin(value = "10000", message = "Base price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Base price must be at most 1000000")
        BigDecimal basePrice,

        @DecimalMin(value = "10000", message = "VIP price must be at least 10000")
        @DecimalMax(value = "1000000", message = "VIP price must be at most 1000000")
        BigDecimal vipPrice,

        @DecimalMin(value = "10000", message = "Couple price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Couple price must be at most 1000000")
        BigDecimal couplePrice,

        @DecimalMin(value = "10000", message = "Adult standard price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Adult standard price must be at most 1000000")
        BigDecimal adultStandardPrice,

        @DecimalMin(value = "10000", message = "Child standard price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Child standard price must be at most 1000000")
        BigDecimal childStandardPrice,

        @DecimalMin(value = "10000", message = "Student standard price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Student standard price must be at most 1000000")
        BigDecimal studentStandardPrice,

        @DecimalMin(value = "10000", message = "Adult VIP price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Adult VIP price must be at most 1000000")
        BigDecimal adultVipPrice,

        @DecimalMin(value = "10000", message = "Child VIP price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Child VIP price must be at most 1000000")
        BigDecimal childVipPrice,

        @DecimalMin(value = "10000", message = "Student VIP price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Student VIP price must be at most 1000000")
        BigDecimal studentVipPrice,

        @DecimalMin(value = "10000", message = "Adult couple price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Adult couple price must be at most 1000000")
        BigDecimal adultCouplePrice,

        @DecimalMin(value = "10000", message = "Child couple price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Child couple price must be at most 1000000")
        BigDecimal childCouplePrice,

        @DecimalMin(value = "10000", message = "Student couple price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Student couple price must be at most 1000000")
        BigDecimal studentCouplePrice,

        Boolean weekendSurcharge,

        Boolean holidaySurcharge,

        @DecimalMin(value = "10000", message = "Late night surcharge amount must be at least 10000")
        @DecimalMax(value = "100000", message = "Late night surcharge amount must be at most 100000")
        BigDecimal lateNightSurchargeAmount,

        ShowtimeStatus status
) {
    public ShowtimeRequest(
            Long movieId,
            Long roomId,
            LocalDateTime startTime,
            BigDecimal basePrice,
            BigDecimal vipPrice,
            BigDecimal couplePrice,
            ShowtimeStatus status
    ) {
        this(
                movieId,
                roomId,
                startTime,
                basePrice,
                vipPrice,
                couplePrice,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                status
        );
    }
}
