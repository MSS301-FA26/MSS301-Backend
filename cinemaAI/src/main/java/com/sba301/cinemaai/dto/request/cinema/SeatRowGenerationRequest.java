package com.sba301.cinemaai.dto.request.cinema;

import com.sba301.cinemaai.enums.SeatType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record SeatRowGenerationRequest(
        @NotBlank(message = "Row label is required")
        String rowLabel,

        @Min(value = 1, message = "Display order must be positive")
        int displayOrder,

        @Min(value = 1, message = "Start column must be positive")
        int startColumn,

        SeatType seatType,

        @NotEmpty(message = "Seat numbers are required")
        List<@Min(value = 1, message = "Seat number must be positive") Integer> seatNumbers
) {
}
