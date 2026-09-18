package com.sba301.cinemaai.dto.response.ticket;

import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TicketPricingRuleResponse(
        Long id,
        Long cinemaId,
        String cinemaName,
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
}
