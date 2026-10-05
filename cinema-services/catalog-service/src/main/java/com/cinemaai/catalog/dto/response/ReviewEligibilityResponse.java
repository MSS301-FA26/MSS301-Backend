package com.cinemaai.catalog.dto.response;

import java.time.LocalDateTime;

public record ReviewEligibilityResponse(
        boolean eligible,
        boolean canEdit,
        String reason,
        String messageVi,
        Long bookingId,
        String bookingCode,
        LocalDateTime showtimeStart,
        String cinemaName,
        ReviewResponse existingReview
) {}
