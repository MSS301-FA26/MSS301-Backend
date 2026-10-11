package com.cinemaai.catalog.dto.response.cinema;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Price preview response for a single draft showtime slot.
 * Contains the full ticket price matrix (seat type x audience type) and day surcharges (weekend/holiday).
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
         * Computed final prices: couple seat.
         * Formula: roomCouplePrice + additional(audience) + daySurcharge (Ghế đôi KHÔNG nhân 2)
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
        List<String> warnings,

        boolean isWeekend,
        boolean isHoliday,
        BigDecimal daySurchargeAmount,
        String dayTypeLabel
) {
    public ShowtimePricePreviewResponse(
            String tempId,
            Long movieId,
            String movieTitle,
            Long roomId,
            String roomName,
            Long cinemaId,
            String cinemaName,
            LocalDateTime startTime,
            LocalDateTime endTime,
            BigDecimal roomStandardPrice,
            BigDecimal roomVipPrice,
            BigDecimal roomCouplePrice,
            BigDecimal childAdditional,
            BigDecimal studentAdditional,
            BigDecimal adultAdditional,
            BigDecimal childStandardPrice,
            BigDecimal studentStandardPrice,
            BigDecimal adultStandardPrice,
            BigDecimal childVipPrice,
            BigDecimal studentVipPrice,
            BigDecimal adultVipPrice,
            BigDecimal childChildCouplePrice,
            BigDecimal childStudentCouplePrice,
            BigDecimal childAdultCouplePrice,
            BigDecimal studentStudentCouplePrice,
            BigDecimal studentAdultCouplePrice,
            BigDecimal adultAdultCouplePrice,
            boolean audiencePriceMissing,
            List<String> warnings
    ) {
        this(
                tempId,
                movieId,
                movieTitle,
                roomId,
                roomName,
                cinemaId,
                cinemaName,
                startTime,
                endTime,
                roomStandardPrice,
                roomVipPrice,
                roomCouplePrice,
                childAdditional,
                studentAdditional,
                adultAdditional,
                childStandardPrice,
                studentStandardPrice,
                adultStandardPrice,
                childVipPrice,
                studentVipPrice,
                adultVipPrice,
                childChildCouplePrice,
                childStudentCouplePrice,
                childAdultCouplePrice,
                studentStudentCouplePrice,
                studentAdultCouplePrice,
                adultAdultCouplePrice,
                audiencePriceMissing,
                warnings,
                false,
                false,
                BigDecimal.ZERO,
                "STANDARD"
        );
    }
}
