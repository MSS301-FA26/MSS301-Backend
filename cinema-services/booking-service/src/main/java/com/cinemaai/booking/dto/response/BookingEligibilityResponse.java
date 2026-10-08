package com.cinemaai.booking.dto.response;

import java.time.LocalDateTime;

public record BookingEligibilityResponse(
        boolean hasWatched,
        Long bookingId,
        String bookingCode,
        LocalDateTime showtimeStart,
        String cinemaName,
        String status
) {}
