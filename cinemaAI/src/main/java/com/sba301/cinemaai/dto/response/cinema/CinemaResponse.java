package com.sba301.cinemaai.dto.response.cinema;

import com.sba301.cinemaai.enums.CinemaStatus;
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
