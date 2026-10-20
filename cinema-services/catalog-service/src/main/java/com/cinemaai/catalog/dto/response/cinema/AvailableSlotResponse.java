package com.cinemaai.catalog.dto.response.cinema;

import java.time.LocalDateTime;

public record AvailableSlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
