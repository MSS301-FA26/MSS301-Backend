package com.cinemaai.catalog.dto.response.cinema;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record CustomerShowtimeSlotResponse(
        Long showtimeId,
        Long movieId,
        String movieTitle,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String format,
        BigDecimal startingPrice,
        int availableSeats,
        Long roomId,
        BigDecimal basePrice,
        BigDecimal vipPrice,
        BigDecimal couplePrice,
        BigDecimal adultStandardPrice,
        BigDecimal childStandardPrice,
        BigDecimal studentStandardPrice,
        BigDecimal adultVipPrice,
        BigDecimal childVipPrice,
        BigDecimal studentVipPrice,
        BigDecimal adultCouplePrice,
        BigDecimal childCouplePrice,
        BigDecimal studentCouplePrice,
        BigDecimal surchargeAmount
) {
    public CustomerShowtimeSlotResponse(
            Long showtimeId,
            Long movieId,
            String movieTitle,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String format,
            BigDecimal startingPrice,
            int availableSeats,
            Long roomId
    ) {
        this(
                showtimeId,
                movieId,
                movieTitle,
                startTime,
                endTime,
                format,
                startingPrice,
                availableSeats,
                roomId,
                startingPrice,
                null,
                null,
                startingPrice,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                BigDecimal.ZERO
        );
    }
}
