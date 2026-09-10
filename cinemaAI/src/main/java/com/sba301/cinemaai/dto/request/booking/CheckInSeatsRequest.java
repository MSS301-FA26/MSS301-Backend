package com.sba301.cinemaai.dto.request.booking;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CheckInSeatsRequest(
        @NotBlank(message = "Booking code is required")
        String bookingCode,

        @NotEmpty(message = "At least one ticket code is required")
        List<String> ticketCodes
) {
}
