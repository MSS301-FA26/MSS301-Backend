package com.cinemaai.catalog.dto.response.ticket;

import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketPricingRuleResponse(
        Long id,
        Long cinemaId,
        TicketType ticketType,
        RoomType roomType,
        SeatType seatType,
        boolean weekend,
        boolean holiday,
        BigDecimal price,
        boolean active,
        LocalDateTime effectiveFrom,
        LocalDateTime effectiveTo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public TicketPricingRuleResponse(
            Long id,
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday,
            BigDecimal price,
            boolean active,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this(id, null, ticketType, roomType, seatType, weekend, holiday, price, active, null, null, createdAt, updatedAt);
    }
}
