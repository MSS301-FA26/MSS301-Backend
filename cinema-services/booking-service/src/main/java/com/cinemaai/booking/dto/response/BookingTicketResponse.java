package com.cinemaai.booking.dto.response;

import com.cinemaai.booking.enums.TicketType;
import java.math.BigDecimal;

public record BookingTicketResponse(
        Long id,
        Long seatId,
        TicketType ticketType,
        Integer viewerAge,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {}
