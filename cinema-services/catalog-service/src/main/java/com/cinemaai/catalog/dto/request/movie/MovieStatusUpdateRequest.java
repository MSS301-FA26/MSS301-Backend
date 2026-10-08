package com.cinemaai.catalog.dto.request.movie;

import com.cinemaai.catalog.enums.MovieStatus;
import jakarta.validation.constraints.NotNull;

public record MovieStatusUpdateRequest(
        @NotNull(message = "Status is required")
        MovieStatus status
) {
}
