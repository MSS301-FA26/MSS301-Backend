package com.sba301.cinemaai.dto.response.booking;

import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingSeatResponse(
        Long seatId,
        String rowLabel,
        int seatNumber,
        BigDecimal unitPrice,
        SeatRuntimeStatus status,
        String ticketCode,
        String qrCode,
        TicketType ticketType,
        LocalDateTime checkedInAt
) {
}
