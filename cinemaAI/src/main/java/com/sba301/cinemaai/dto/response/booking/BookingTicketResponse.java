package com.sba301.cinemaai.dto.response.booking;

import com.sba301.cinemaai.enums.TicketType;
import java.math.BigDecimal;

public record BookingTicketResponse(
        TicketType ticketType,
        int viewerAge,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
}
