package com.cinemaai.catalog.dto.response.movie;

import com.cinemaai.catalog.entity.Director;
import java.time.LocalDateTime;

public record DirectorResponse(
        Long id,
        String name,
        String biography,
        String avatarUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static DirectorResponse fromEntity(Director director) {
        if (director == null) return null;
        return new DirectorResponse(
                director.getId(),
                director.getName(),
                director.getBiography(),
                director.getAvatarUrl(),
                director.getCreatedAt(),
                director.getUpdatedAt()
        );
    }
}
