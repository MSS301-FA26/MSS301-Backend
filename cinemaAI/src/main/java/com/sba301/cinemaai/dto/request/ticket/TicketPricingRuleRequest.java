package com.sba301.cinemaai.dto.request.ticket;

import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
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
