package com.cinemaai.catalog.dto.request.cinema;

import com.cinemaai.catalog.enums.SeatStatus;
import com.cinemaai.catalog.enums.SeatType;
import jakarta.validation.constraints.NotNull;

public record SeatUpdateRequest(
        @NotNull(message = "Seat type is required")
        SeatType seatType,

        @NotNull(message = "Seat status is required")
        SeatStatus status
) {
}
