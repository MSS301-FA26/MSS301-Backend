package com.cinemaai.catalog.dto.response.movie;

import java.time.LocalDateTime;

public record GenreResponse(
        Long id,
        String name,
        String description,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
