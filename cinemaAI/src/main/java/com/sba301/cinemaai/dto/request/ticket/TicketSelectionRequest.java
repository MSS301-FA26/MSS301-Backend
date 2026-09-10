package com.sba301.cinemaai.dto.request.ticket;

import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TicketSelectionRequest(
        Long seatId,

        @NotNull(message = "Ticket type is required")
        TicketType ticketType,

        SeatType seatType,

        @Min(value = 0, message = "Viewer age must be zero or positive")
        int viewerAge,

        @Min(value = 1, message = "Quantity must be at least 1")
        int quantity
) {
    public TicketSelectionRequest(TicketType ticketType, int viewerAge, int quantity) {
        this(null, ticketType, SeatType.STANDARD, viewerAge, quantity);
    }

    public TicketSelectionRequest {
        if (seatType == null) {
            seatType = SeatType.STANDARD;
        }
    }
}
