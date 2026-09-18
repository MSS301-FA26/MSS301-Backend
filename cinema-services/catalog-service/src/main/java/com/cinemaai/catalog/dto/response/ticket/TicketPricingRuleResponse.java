package com.cinemaai.catalog.dto.response.ticket;

import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketPricingRuleResponse(
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
}
