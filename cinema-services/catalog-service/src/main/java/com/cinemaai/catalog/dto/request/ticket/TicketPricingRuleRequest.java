package com.cinemaai.catalog.dto.request.ticket;

import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record TicketPricingRuleRequest(
        Long cinemaId,

        TicketType ticketType,

        RoomType roomType,

        SeatType seatType,

        boolean weekend,

        boolean holiday,

        @NotNull(message = "Price is required")
        @DecimalMin(value = "10000", message = "Price must be at least 10000")
        @DecimalMax(value = "1000000", message = "Price must be at most 1000000")
        BigDecimal price,

        Boolean active,

        java.time.LocalDateTime effectiveFrom,

        java.time.LocalDateTime effectiveTo
) {
    public TicketPricingRuleRequest {
        if (ticketType == null) ticketType = TicketType.ADULT;
        if (roomType == null) roomType = RoomType.STANDARD;
        if (seatType == null) seatType = SeatType.STANDARD;
    }

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

    public TicketPricingRuleRequest(
            Long cinemaId,
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday,
            BigDecimal price,
            Boolean active
    ) {
        this(cinemaId, ticketType, roomType, seatType, weekend, holiday, price, active, null, null);
    }
}
