package com.sba301.cinemaai.dto.request.ticket;

import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketPricingRuleRequest(
        Long cinemaId,

        TicketType ticketType,

        RoomType roomType,

        @NotNull(message = "Seat type is required")
        SeatType seatType,

        boolean weekend,

        boolean holiday,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "0", message = "Price must be at least 0")
        @DecimalMax(value = "10000000", message = "Price must be at most 10000000")
        BigDecimal price,

        Boolean active,

        LocalDateTime effectiveFrom,

        LocalDateTime effectiveTo
) {
    public TicketPricingRuleRequest(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday,
            BigDecimal price,
            Boolean active
    ) {
        this(null, ticketType, roomType, seatType, weekend, holiday, price, active, null, null);
    }
}
