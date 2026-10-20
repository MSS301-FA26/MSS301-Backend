package com.sba301.cinemaai.dto.response.cinema;

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
        Long roomId
) {
}
