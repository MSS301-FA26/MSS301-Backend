package com.cinemaai.catalog.dto.request.ticket;

import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TicketPricingRuleRequest(
        @NotNull(message = "Ticket type is required")
        TicketType ticketType,

        @NotNull(message = "Room type is required")
        RoomType roomType,

        @NotNull(message = "Seat type is required")
        SeatType seatType,

        boolean weekend,

        boolean holiday,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "10000", message = "Price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Price must be at most 1000000")
        BigDecimal price,

        Boolean active
) {
}
