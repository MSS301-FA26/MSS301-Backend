package com.sba301.cinemaai.dto.response.cinema;

import com.sba301.cinemaai.enums.ShowtimeStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ShowtimeResponse(
        Long id,
        Long movieId,
        String movieTitle,
        String movieAgeRating,
        List<String> movieGenreNames,
        Long cinemaId,
        String cinemaName,
        Long roomId,
        String roomName,
        LocalDateTime startTime,
        LocalDateTime endTime,
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
        boolean weekendSurcharge,
        boolean holidaySurcharge,
        BigDecimal lateNightSurchargeAmount,
        BigDecimal surchargeAmount,
        ShowtimeStatus status,
        String cancellationReason,
        LocalDateTime cancelledAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
