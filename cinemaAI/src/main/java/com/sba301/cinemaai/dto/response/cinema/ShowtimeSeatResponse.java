package com.sba301.cinemaai.dto.response.cinema;

import com.sba301.cinemaai.enums.SeatStatus;
import com.sba301.cinemaai.enums.SeatType;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ShowtimeSeatResponse(
        Long seatId,
        Long seatRowId,
        String rowLabel,
        int displayOrder,
        int seatNumber,
        int displayColumn,
        int startColumn,
        SeatType seatType,
        SeatStatus seatStatus,
        String runtimeStatus,
        LocalDateTime holdExpiresAt,
        BigDecimal unitPrice
) {
}
