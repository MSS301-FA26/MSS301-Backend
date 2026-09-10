package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.SeatType;
import jakarta.validation.constraints.NotNull;

public record SeatUpdateRequest(
        @NotNull(message = "Seat type is required")
        SeatType seatType,

        @NotNull(message = "Seat status is required")
        SeatStatus status
) {
}
