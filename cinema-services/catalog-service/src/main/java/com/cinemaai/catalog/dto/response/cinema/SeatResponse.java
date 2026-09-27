package com.cinemaai.catalog.dto.response.cinema;

import com.cinemaai.catalog.enums.SeatStatus;
import com.cinemaai.catalog.enums.SeatType;

public record SeatResponse(
        Long id,
        Long roomId,
        Long seatRowId,
        String rowLabel,
        int displayOrder,
        int seatNumber,
        int displayColumn,
        int startColumn,
        SeatType seatType,
        SeatStatus status
) {
}
