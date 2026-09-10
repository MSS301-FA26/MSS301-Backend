package com.sba301.cinemaai.dto.response.cinema;

import java.time.LocalDateTime;

public record AvailableSlotResponse(
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
