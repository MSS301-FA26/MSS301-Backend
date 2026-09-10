package com.sba301.cinemaai.dto.request.ticket;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record TicketPriceValidationRequest(
        @NotNull(message = "Showtime id is required")
        Long showtimeId,

        Long comboId,

        boolean holiday,

        @NotEmpty(message = "Tickets are required")
        List<@Valid TicketSelectionRequest> tickets
) {
}
