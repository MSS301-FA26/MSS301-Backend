package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.SeatType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.Valid;
import java.util.List;

public record SeatLayoutRequest(
        @NotNull(message = "Default seat type is required")
        SeatType defaultSeatType,

        List<@Valid SeatRowGenerationRequest> rows
) {
    public SeatLayoutRequest(SeatType defaultSeatType) {
        this(defaultSeatType, null);
    }
}
