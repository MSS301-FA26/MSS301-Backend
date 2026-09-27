package com.cinemaai.catalog.dto.response.cinema;

import com.cinemaai.catalog.enums.CinemaStatus;
import java.time.LocalDateTime;

public record CinemaResponse(
        Long id,
        String name,
        String address,
        String city,
        String phone,
        CinemaStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
