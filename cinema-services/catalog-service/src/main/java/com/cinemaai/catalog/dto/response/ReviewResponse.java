package com.cinemaai.catalog.dto.response;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Long userId,
        String userEmail,
        String userFullName,
        Long movieId,
        String movieTitle,
        Long bookingId,
        String bookingCode,
        Long showtimeId,
        LocalDateTime showtimeStart,
        String cinemaName,
        String roomName,
        int rating,
        String comment,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
