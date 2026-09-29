package com.cinemaai.catalog.dto.response.cinema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Price preview response for a single draft showtime slot.
 * Contains the full ticket price matrix (seat type × audience type).
 */
public record ShowtimePricePreviewResponse(

        /** Echoed back from the request (frontend temp ID). */
        String tempId,

        Long movieId,
        String movieTitle,

        Long roomId,
        String roomName,
        Long cinemaId,
        String cinemaName,

        LocalDateTime startTime,
        LocalDateTime endTime,

        /** Room's base prices (snapshot at preview time). */
        BigDecimal roomStandardPrice,
        BigDecimal roomVipPrice,
        BigDecimal roomCouplePrice,

        /** Audience surcharges configured for this cinema. */
        BigDecimal childAdditional,
        BigDecimal studentAdditional,
        BigDecimal adultAdditional,

        /** Computed final prices: standard seat */
        BigDecimal childStandardPrice,
        BigDecimal studentStandardPrice,
        BigDecimal adultStandardPrice,

        /** Computed final prices: VIP seat */
        BigDecimal childVipPrice,
        BigDecimal studentVipPrice,
        BigDecimal adultVipPrice,

        /**
         * Computed final prices: couple seat (price for a PAIR of seats).
         * Formula: roomCouplePrice + additional(guest1) + additional(guest2)
         */
        BigDecimal childChildCouplePrice,
        BigDecimal childStudentCouplePrice,
        BigDecimal childAdultCouplePrice,
        BigDecimal studentStudentCouplePrice,
        BigDecimal studentAdultCouplePrice,
        BigDecimal adultAdultCouplePrice,

        /** True if any required audience surcharge is missing for this cinema. */
        boolean audiencePriceMissing,

        /** List of validation warnings (does not block preview, but blocks save). */
        List<String> warnings
) {
}
