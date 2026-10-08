package com.cinemaai.booking.dto.response;

import java.time.LocalDateTime;

public record OccupiedSeatDto(
        Long seatId,
        String runtimeStatus,
        LocalDateTime holdExpiresAt
) {}
