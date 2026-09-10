package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.ShowtimeStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Bulk showtime creation request.
 *
 * <p>The {@code movieId} and pricing fields are shared across all slots.
 * Each slot only needs its own {@code roomId} and {@code startTime}.
 * Individual slots may override {@code status}; if omitted the top-level
 * {@code defaultStatus} is used (defaults to SCHEDULED).
 *
 * <p>Example body:
 * <pre>{@code
 * {
 *   "movieId": 5,
 *   "basePrice": 90000,
 *   "vipPrice": 130000,
 *   "couplePrice": 160000,
 *   "defaultStatus": "OPEN",
 *   "slots": [
 *     { "roomId": 1, "startTime": "2026-06-10T09:00:00" },
 *     { "roomId": 1, "startTime": "2026-06-10T14:30:00" },
 *     { "roomId": 2, "startTime": "2026-06-10T19:00:00", "status": "SCHEDULED" }
 *   ]
 * }
 * }</pre>
 */
public record BulkShowtimeRequest(

        @NotNull(message = "Movie id is required")
        Long movieId,

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

        /** Status applied to every slot that does not override it. Defaults to SCHEDULED. */
        ShowtimeStatus defaultStatus,

        @NotEmpty(message = "At least one showtime slot is required")
        @Valid
        List<Slot> slots
) {

    public record Slot(
            @NotNull(message = "Room id is required")
            Long roomId,

            @NotNull(message = "Start time is required")
            @Future(message = "Start time must be in the future")
            LocalDateTime startTime,

            /** Optional per-slot status override. Falls back to {@code defaultStatus}. */
            ShowtimeStatus status
    ) {}
}
