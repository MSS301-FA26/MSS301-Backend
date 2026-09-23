package com.cinemaai.booking.dto.response;

import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.SeatType;
import com.cinemaai.booking.enums.TicketType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BookingSeatResponse(
        Long id,
        Long seatId,
        Long showtimeId,
        String rowLabel,
        int seatNumber,
        String seatLabel,
        SeatType seatType,
        BigDecimal unitPrice,
        BookingSeatStatus status,
        String ticketCode,
        String qrCode,
        TicketType ticketType,
        LocalDateTime checkedInAt
) {}
