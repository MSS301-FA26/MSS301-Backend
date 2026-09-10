package com.sba301.cinemaai.dto.response.report;

import java.time.LocalDateTime;

public record ShowtimeFillResponse(
        Long showtimeId,
        String movieTitle,
        String roomName,
        LocalDateTime startTime,
        long capacity,
        long sold,
        long remaining
) {
}
